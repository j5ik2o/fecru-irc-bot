package com.github.j5ik2o.fecruircbot

import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import com.atlassian.event.api.EventListener
import com.atlassian.event.api.EventPublisher
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.crucible.event._
import parser._
import scala.collection.JavaConverters._
import com.atlassian.crucible.spi.PermId
import com.atlassian.crucible.spi.data._
import com.atlassian.sal.api.pluginsettings.{PluginSettings, PluginSettingsFactory}
import com.atlassian.crucible.spi.services.{NotFoundException, ReviewService, ProjectService}
import scala.util.control.Exception._
import scala.None

class CrucibleEventListener
(
  eventPublisher: EventPublisher,
  applicationProperties: ApplicationProperties,
  reviewService: ReviewService,
  projectService: ProjectService,
  protected val pluginSettingsFactory: PluginSettingsFactory
  )
  extends DisposableBean with InitializingBean with IrcConfigAccess {

  protected val name = "cru-irc-bot"

  protected def isIrcBotChannelEnable(settings: PluginSettings, key: String) = {
    val r = settings.get(classOf[IrcBotProjectChannelConfig].getName + "_" + key + ".enable")
    if (r != null)
      r.asInstanceOf[String].toBoolean
    else
      false
  }

  protected def getIrcBotChannelName(settings: PluginSettings, key: String) =
    settings.get(classOf[IrcBotProjectChannelConfig].getName + "_" + key + ".channelName").asInstanceOf[String]


  protected def isIrcBotChannelNotice(settings: PluginSettings, key: String) = {
    val r = settings.get(classOf[IrcBotProjectChannelConfig].getName + "_" + key + ".notice")
    if (r != null)
      r.asInstanceOf[String].toBoolean
    else
      false
  }

  private def getReviewUrl(reviewId: PermId[ReviewData]): String =
    applicationProperties.getBaseUrl + "/cru/" + reviewId.getId

  private def getProjectUrl(projectKey: String): String =
    applicationProperties.getBaseUrl + "/project/" + projectKey

  private def formattedUser(userData: UserData) =
    "%s(%s)".format(
      if (userData.getDisplayName != null) userData.getDisplayName else "",
      if (userData.getUserName != null) userData.getUserName else "")

  private def formattedComment(commentData: CommentData) =
    "%s: %s".format(
      formattedUser(commentData.getUser),
      commentData.getMessage
    )

  private def formattedProject(reviewData: ReviewData) =
    "[%s]".format(reviewData.getPermaId.getId)

  private def formattedCommentMessage
  (projectData: ProjectData,
   reviewData: DetailedReviewData,
   commentId: PermId[CommentData],
   title: String) = {
    val commentDataOption = catching(classOf[NotFoundException]) opt reviewService.getComment(commentId)
    commentDataOption.map {
      commentData =>
        Seq(
          "%s %s".format(formattedProject(reviewData), title),
          "CommentId: %s".format(commentId.getId),
          formattedComment(commentData),
          getReviewUrl(reviewData.getPermaId) + "#c" + commentId.getId.split(":")(1)
        )
    } getOrElse {
      Seq(
        "%s %s".format(formattedProject(reviewData), title),
        "CommentId: %s".format(commentId.getId),
        getReviewUrl(reviewData.getPermaId)
      )
    }
  }


  private def formattedReviewMessage
  (projectData: ProjectData,
   reviewData: DetailedReviewData,
   title: String) = {
    val r = List(
      Some("%s %s".format(formattedProject(reviewData), title)),
      if (reviewData.getPermaId != null)
        Some("Id: %s".format(reviewData.getPermaId.getId))
      else
        None,
      if (reviewData.getName != null && reviewData.getName.isEmpty == false)
        Some("Name: %s".format(reviewData.getName))
      else
        None,
      if (reviewData.getDescription != null && reviewData.getDescription.isEmpty == false)
        Some("Description: %s".format(reviewData.getDescription))
      else
        None,
      if (reviewData.getAuthor != null)
        Some("Author: %s".format(formattedUser(reviewData.getAuthor)))
      else
        None,
      if (reviewData.getModerator != null)
        Some("Moderator: %s".format(formattedUser(reviewData.getModerator)))
      else
        None).flatten ++
      reviewData.getReviewers.reviewer.asScala.map {
        e => Some("Reviewer: %s(%s)".format(e.getDisplayName, e.getUserName))
      }.flatten.toList ++
      List(
        if (reviewData.getDueDate != null)
          Some("Due Date: %s".format(dateFormat.format(reviewData.getDueDate)))
        else
          None,
        if (reviewData.getJiraIssueKey != null)
          Some("JIRA: %s".format(reviewData.getJiraIssueKey))
        else
          None,
        Some(getReviewUrl(reviewData.getPermaId))
      ).flatten
    r.toSeq
  }

  private def formattedReviewStateChangeMessage
  (projectData: ProjectData,
   reviewData: DetailedReviewData,
   reviewOldDataState: ReviewData.State,
   reviewNewDataState: ReviewData.State,
   title: String) =
    Seq(
      "%s %s".format(formattedProject(reviewData), title),
      "%s から %s に変更しました".format(
        reviewOldDataState.name(),
        reviewNewDataState.name()
      ),
      getReviewUrl(reviewData.getPermaId)
    )

  private def formattedReviewerCompletedMessage
  (projectData: ProjectData,
   reviewData: DetailedReviewData,
   reviewerData: ReviewerData,
   title: String) =
    Seq(
      "%s %s".format(formattedProject(reviewData), title),
      "%s(%s)はレビューを完了させました。".format(
        reviewerData.getDisplayName,
        reviewerData.getUserName
      ),
      getReviewUrl(reviewData.getPermaId)
    )

  private def formattedonReviewItemRevisionDataChangeMessage
  (projectData: ProjectData,
   reviewData: DetailedReviewData,
   addedRevisions: Iterable[ReviewItemRevisionData],
   removedRevisions: Iterable[ReviewItemRevisionData],
   title: String): Seq[String] = {

    def buildMessages(header: String, it: Iterable[ReviewItemRevisionData]) = {
      it.toList match {
        case Nil => None
        case s => Some(
          header :: s.map {
            e => "r%s, %s, %s".format(
              e.getRevision,
              dateFormat.format(e.getAddDate),
              e.getPath)
          }
        )
      }
    }
    val result = "%s %s".format(formattedProject(reviewData), title) ::
      (buildMessages("追加リビジョン:", addedRevisions).flatten.toList ++
        buildMessages("削除リビジョン:", removedRevisions).flatten.toList)
    (result.:+(getReviewUrl(reviewData.getPermaId))).toSeq
  }

  @EventListener
  def onReviewCommentCreate(event: CommentCreatedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    sendMessages(
      projectData.getKey,
      formattedCommentMessage(
        projectData,
        reviewData,
        event.getCommentId,
        "コメントが投稿されました"))
  }

  @EventListener
  def onReviewCommentDeleted(event: CommentDeletedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    sendMessages(
      projectData.getKey,
      formattedCommentMessage(
        projectData,
        reviewData,
        event.getCommentId,
        "コメントが削除されました"))
  }


  @EventListener
  def onReviewCommentUpdated(event: CommentUpdatedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    sendMessages(
      projectData.getKey,
      formattedCommentMessage(
        projectData,
        reviewData,
        event.getCommentId,
        "コメントが更新されました"))
  }

  @EventListener
  def onReviewStateChange(event: ReviewStateChangedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    sendMessages(
      projectData.getKey,
      formattedReviewStateChangeMessage(
        projectData, reviewData,
        event.getOldState,
        event.getNewState,
        "レビューの状態が変更されました"))
  }

  @EventListener
  def onReviewCreated(event: ReviewCreatedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    sendMessages(
      projectData.getKey,
      formattedReviewMessage(
        projectData,
        reviewData,
        "レビューが作成されました"))
  }

  @EventListener
  def onReviewUpdate(event: ReviewUpdatedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    sendMessages(
      projectData.getKey,
      formattedReviewMessage(
        projectData,
        reviewData,
        "レビューが更新されました"))
  }

  @EventListener
  def onReviewUpdate(event: ReviewDeletedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    sendMessages(
      projectData.getKey,
      formattedReviewMessage(
        projectData,
        reviewData,
        "レビューが削除されました"))
  }

  @EventListener
  def onReviewerCompleted(event: ReviewerCompletedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    sendMessages(
      projectData.getKey,
      formattedReviewerCompletedMessage(
        projectData,
        reviewData,
        event.getReviewer,
        "レビューアがレビューを完了にしました"))
  }

  @EventListener
  def onReviewerUncompleted(event: ReviewerUncompletedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    sendMessages(
      projectData.getKey,
      formattedReviewerCompletedMessage(
        projectData,
        reviewData,
        event.getReviewer,
        "レビューアがレビューを未完了にしました"))
  }

  @EventListener
  def onReviewItemRevisionDataChange(event: ReviewItemRevisionDataChangedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)

    import scala.collection.JavaConverters._

    val addedRevisionsAsJava = event.getAddedRevisions
    val removedRevisionsAsJava = event.getRemovedRevisions
    sendMessages(
      projectData.getKey,
      formattedonReviewItemRevisionDataChangeMessage(
        projectData,
        reviewData,
        if (addedRevisionsAsJava != null) addedRevisionsAsJava.asScala else Iterable.empty,
        if (removedRevisionsAsJava != null) removedRevisionsAsJava.asScala else Iterable.empty,
        "リビジョンが追加もしくは削除されました"))

  }

  @EventListener
  def onAllReviewersCompleted(event: AllReviewersCompletedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    sendMessages(
      projectData.getKey,
      formattedReviewMessage(
        projectData,
        reviewData,
        "すべてのレビューアがレビューを完了させました"))
  }

  @EventListener
  def onAllReviewersNoLongerCompleted(event: AllReviewersNoLongerCompletedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    sendMessages(
      projectData.getKey,
      formattedReviewMessage(
        projectData,
        reviewData,
        "すべてのレビューアがレビューを久しく完了していません"))
  }

  def listReview(asc:Boolean) = {
    val r = reviewService.getAllReviews(true)
    if (r != null) {
      r.asScala.filter {
        e =>
          e.getState == ReviewData.State.Review
      }.sortWith(
        (o1, o2) =>
          (o1.asInstanceOf[DetailedReviewData].getVersionedComments.comments.size() <
            o2.asInstanceOf[DetailedReviewData].getVersionedComments.comments.size() == asc)
      ).take(10).map {
        e =>
          "[%s] %s , a:%s, m:%s : %s".
            format(e.getPermaId.getId, e.getName, formattedUser(e.getAuthor), formattedUser(e.getModerator), getReviewUrl(e.getPermaId))
      }
    } else
      List.empty[String]
  }

  def onMessage(channel: String, sender: String, login: String, hostname: String, message: String) {
    LOGGER.info("c = %s, s = %s, l = %s, h = %s, m = %s".format(channel, sender, login, hostname, message))
    val settings = pluginSettingsFactory.createGlobalSettings
    if (isIrcBotEnable(settings)) {
      val b = new BotParsers
      val r = b.parse(message) match {
        case ListOpecode(ReviewOperand(CommentSortType(Asc))) => listReview(true)
        case ListOpecode(ReviewOperand(CommentSortType(Desc))) => listReview(true)
      }
      r.foreach {
        pircBot.sendMessage(channel, _)
      }
    }
  }

  def destroy {
    eventPublisher.unregister(this)
  }

  def afterPropertiesSet {
    eventPublisher.register(this)

  }

}
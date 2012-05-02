package com.github.j5ik2o.fecruircbot

import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import com.atlassian.event.api.EventListener
import com.atlassian.event.api.EventPublisher
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.crucible.event._
import scala.collection.JavaConverters._
import com.atlassian.crucible.spi.PermId
import java.text.SimpleDateFormat
import scala.Some
import com.atlassian.crucible.spi.data._
import com.atlassian.sal.api.pluginsettings.{PluginSettings, PluginSettingsFactory}
import scala.Predef._
import com.atlassian.crucible.spi.services.{NotFoundException, ReviewService, ProjectService}
import scala.util.control.Exception._

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

  eventPublisher.register(this)

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
   reviewData: ReviewData,
   commentId: PermId[CommentData],
   title: String) = {
    val commentDataOption = catching(classOf[NotFoundException]) opt reviewService.getComment(commentId)
    commentDataOption.map {
      commentData =>
        Seq(
          "%s %s".format(formattedProject(reviewData), title),
          "CommentId: %s".format(commentId.getId),
          "User: %s".format(formattedUser(commentData.getUser)),
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
   detailedReviewData: DetailedReviewData,
   title: String) = {
    val r = List(
      Some("%s %s".format(formattedProject(detailedReviewData), title)),
      if (detailedReviewData.getPermaId != null)
        Some("Id: %s".format(detailedReviewData.getPermaId.getId))
      else
        None,
      if (detailedReviewData.getName != null)
        Some("Name: %s".format(detailedReviewData.getName))
      else
        None,
      if (detailedReviewData.getDescription != null)
        Some("Description: %s".format(detailedReviewData.getDescription))
      else
        None,
      if (detailedReviewData.getAuthor != null)
        Some("Author: %s".format(formattedUser(detailedReviewData.getAuthor)))
      else
        None,
      if (detailedReviewData.getModerator != null)
        Some("Moderator: %s".format(formattedUser(detailedReviewData.getModerator)))
      else
        None).flatten ++
      detailedReviewData.getReviewers.reviewer.asScala.map {
        e => Some("Reviewer: %s(%s)".format(e.getDisplayName, e.getUserName))
      }.flatten.toList ++
      List(
        if (detailedReviewData.getDueDate != null)
          Some("DueDate: %s".format(dateFormat.format(detailedReviewData.getDueDate)))
        else
          None,
        if (detailedReviewData.getJiraIssueKey != null)
          Some("JIRA: %s".format(detailedReviewData.getJiraIssueKey))
        else
          None,
        Some(getReviewUrl(detailedReviewData.getPermaId))
      ).flatten
    r.toSeq
  }

  private def formattedReviewStateChangeMessage
  (projectData: ProjectData,
   reviewData: ReviewData,
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
   reviewData: ReviewData,
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
   reviewData: ReviewData,
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


  def onMessage(channel: String, sender: String, login: String, hostname: String, message: String) {
    LOGGER.info("c = %s, s = %s, l = %s, h = %s, m = %s".format(channel, sender, login, hostname, message))
  }

  def destroy {
    eventPublisher.unregister(this)
  }

  def afterPropertiesSet {
    eventPublisher.register(this)
  }


}
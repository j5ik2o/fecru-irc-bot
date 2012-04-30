package com.github.j5ik2o.fecruircbot

import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import com.atlassian.event.api.EventListener
import com.atlassian.event.api.EventPublisher
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.crucible.spi.services.{ReviewService, ProjectService}
import com.atlassian.crucible.event._
import scala.collection.JavaConverters._
import com.atlassian.crucible.spi.PermId
import java.text.SimpleDateFormat
import scala.Some
import com.atlassian.crucible.spi.data._

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

  private def formattedProject(projectData: ProjectData) =
    "[%s:%s]".format(projectData.getKey, projectData.getName)

  private def formattedCommentMessage
  (projectData: ProjectData,
   reviewData: ReviewData,
   commentData: CommentData, title: String) =
    Seq(
      "%s %s".format(formattedProject(projectData), title),
      formattedComment(commentData),
      getReviewUrl(reviewData.getPermaId)
    )


  val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

  private def formattedReviewMessage
  (projectData: ProjectData,
   detailedReviewData: DetailedReviewData,
   title: String) = {
    val r = List(
      Some("%s %s".format(formattedProject(projectData), title)),
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
      "%s %s".format(formattedProject(projectData), title),
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
      "%s %s".format(formattedProject(projectData), title),
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
    val result = "%s %s".format(formattedProject(projectData), title) ::
      (buildMessages("追加リビジョン:", addedRevisions).flatten.toList ++
        buildMessages("削除リビジョン:", removedRevisions).flatten.toList)
    (result.:+(getReviewUrl(reviewData.getPermaId))).toSeq
  }


  @EventListener
  def onReviewComment(event: CommentCreatedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    val commentData = reviewService.getComment(event.getCommentId)
    sendMessages(
      getKey(projectData),
      formattedCommentMessage(
        projectData,
        reviewData,
        commentData,
        "コメントが投稿されました"))
  }

  @EventListener
  def onReviewComment(event: CommentDeletedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    val commentData = reviewService.getComment(event.getCommentId)
    sendMessages(
      getKey(projectData),
      formattedCommentMessage(
        projectData,
        reviewData,
        commentData,
        "コメントが削除されました"))
  }

  private def getKey(projectData: ProjectData): String =
    "cru_%s".format(projectData.getKey)

  @EventListener
  def onReviewComment(event: CommentUpdatedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    val commentData = reviewService.getComment(event.getCommentId)
    sendMessages(
      getKey(projectData),
      formattedCommentMessage(
        projectData,
        reviewData,
        commentData,
        "コメントが更新されました"))
  }

  @EventListener
  def onReviewStateChange(event: ReviewStateChangedEvent) {
    val reviewData = reviewService.getReviewDetails(event.getReviewId)
    val projectData = projectService.getProject(reviewData.getProjectKey)
    sendMessages(
      getKey(projectData),
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
      getKey(projectData),
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
      getKey(projectData),
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
      getKey(projectData),
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
      getKey(projectData),
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
      getKey(projectData),
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
      getKey(projectData),
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
      getKey(projectData),
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
      getKey(projectData),
      formattedReviewMessage(
        projectData,
        reviewData,
        "すべてのレビューアがレビューを久しく完了していません"))
  }


  def destroy {
    eventPublisher.unregister(this)
  }

  def afterPropertiesSet {
    eventPublisher.register(this)
  }


}
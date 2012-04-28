package com.github.j5ik2o.fecruircbot

import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import com.atlassian.crucible.event.ReviewCommentEvent
import com.atlassian.crucible.event.ReviewCreatedEvent
import com.atlassian.crucible.event.ReviewStateChangedEvent
import com.atlassian.crucible.event.ReviewUpdatedEvent
import com.atlassian.crucible.spi.PermId
import com.atlassian.crucible.spi.data.CommentData
import com.atlassian.crucible.spi.data.DetailedReviewData
import com.atlassian.crucible.spi.data.ProjectData
import com.atlassian.crucible.spi.data.ReviewData
import com.atlassian.crucible.spi.data.UserData
import com.atlassian.crucible.spi.services.ProjectService
import com.atlassian.crucible.spi.services.ReviewService
import com.atlassian.event.api.EventListener
import com.atlassian.event.api.EventPublisher
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory

class CrucibleEventListener
(
  eventPublisher: EventPublisher,
  applicationProperties: ApplicationProperties,
  reviewService: ReviewService,
  projectService: ProjectService,
  protected val pluginSettingsFactory: PluginSettingsFactory
  )
  extends DisposableBean with InitializingBean with IrcConfigAccess {

  protected val name = "cru-irc-oot"

  eventPublisher.register(this)

  private def getReviewUrl(reviewId: PermId[ReviewData]): String =
    applicationProperties.getBaseUrl + "/cru/" + reviewId.getId

  private def getProjectUrl(projectKey: String): String =
    applicationProperties.getBaseUrl + "/project/" + projectKey


  private def getProjectDataByReviewId(reviewId: PermId[ReviewData]): ProjectData = {
    val review: DetailedReviewData = reviewService.getReviewDetails(reviewId)
    projectService.getProject(review.getProjectKey)
  }


  @EventListener
  def onReviewCreate(event: ReviewCreatedEvent) {
    val reviewId = event.getReviewId
    val review = this.reviewService.getReviewDetails(reviewId)
    val project = this.projectService.getProject(review.getProjectKey)
    sendMessage(project.getKey, "created")
  }

  @EventListener
  def onReviewUpdate(event: ReviewUpdatedEvent) {
    val reviewId = event.getReviewId
    sendMessage(getProjectDataByReviewId(reviewId).getKey, "updated")
  }

  private lazy val formattedUser = {
    userData: UserData =>
      "%s".format(userData.getDisplayName)
  }

  private lazy val formattedCommentText = {
    commentData: CommentData =>
      "%s".format(commentData.getMessage)
  }

  @EventListener
  def onReviewComment(event: ReviewCommentEvent) {
    val reviewId = event.getReviewId
    val commentId = event.getCommentId
    val comment = this.reviewService.getComment(commentId)
    sendMessage(getProjectDataByReviewId(reviewId).getKey, "コメントが投稿されました")
  }

  @EventListener
  def onReviewStateChange(event: ReviewStateChangedEvent) {
    val reviewId = event.getReviewId
    sendMessage(getProjectDataByReviewId(reviewId).getKey,
      "status moved from " + event.getOldState.name + " to " + event.getNewState.name)
  }

  def destroy {
    LOGGER.debug("Unregister review event listener")
    eventPublisher.unregister(this)
  }

  def afterPropertiesSet {
    LOGGER.debug("Register review event listener")
    eventPublisher.register(this)
  }


}
package com.github.j5ik2o.fecruircbot

import java.io.IOException
import org.jibble.pircbot.IrcException
import org.jibble.pircbot.NickAlreadyInUseException
import org.jibble.pircbot.PircBot
import org.slf4j.LoggerFactory
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
import com.atlassian.sal.api.pluginsettings.PluginSettings
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory

class CrucibleEventListener
(
  eventPublisher: EventPublisher,
  applicationProperties: ApplicationProperties,
  reviewService: ReviewService,
  projectService: ProjectService,
  pluginSettingsFactory: PluginSettingsFactory
  )
  extends DisposableBean with InitializingBean {

  eventPublisher.register(this)

  private val LOGGER = LoggerFactory.getLogger("atlassian.plugin")

  private def getReviewUrl(reviewId: PermId[ReviewData]): String =
    applicationProperties.getBaseUrl + "/cru/" + reviewId.getId

  private def getProjectUrl(projectKey: String): String =
    applicationProperties.getBaseUrl + "/project/" + projectKey

  private def getChannelName(settings: PluginSettings, projectId: String): String = {
    settings.get(classOf[IrcBotChannelConfig].getName + "_" + projectId + ".channelName").asInstanceOf[String]
  }

  private def isIrcBotChannelEnable(settings: PluginSettings, projectId: String): Boolean = {
    settings.get(classOf[IrcBotChannelConfig].getName + "_" + projectId + ".enable").asInstanceOf[String].toBoolean
  }

  private def isIrcBotChannelNotice(settings: PluginSettings, projectId: String): Boolean = {
    settings.get(classOf[IrcBotChannelConfig].getName + "_" + projectId + ".notice").asInstanceOf[String].toBoolean
  }

  private def isIrcBotEnable(settings: PluginSettings): Boolean = {
    val enable: Boolean = settings.get(classOf[IrcBotGlobalConfig].getName + ".enable").asInstanceOf[String].toBoolean
    enable
  }

  private def getIrcServerName(settings: PluginSettings): String = {
    settings.get(classOf[IrcBotGlobalConfig].getName + ".ircServerName").asInstanceOf[String]
  }

  private def getIrcServerPort(settings: PluginSettings): Option[Int] = {
    val ircServerPort = settings.get(classOf[IrcBotGlobalConfig].getName + ".ircServerPort").asInstanceOf[String]
    if (ircServerPort.isEmpty == false) {
      Some(ircServerPort.toInt)
    }else{
      None
    }
  }

  private def getProjectDataByReviewId(reviewId: PermId[ReviewData]): ProjectData = {
    val review: DetailedReviewData = reviewService.getReviewDetails(reviewId)
    projectService.getProject(review.getProjectKey)
  }


  private def autoConnect(settings: PluginSettings) {
    if (pircBot.isConnected) {
      return
    }
    val ircServerName: String = getIrcServerName(settings)
    LOGGER.debug("irc server name = " + ircServerName)
    val ircServerPort = getIrcServerPort(settings)
    LOGGER.debug("irc server port = " + ircServerPort)
    if (ircServerPort != null && ircServerPort.getOrElse(0) != 0) {
      pircBot.connect(ircServerName, ircServerPort.get)
    } else {
      pircBot.connect(ircServerName)
    }
  }

  private def sendMessages(projectKey: String, title: String, messages: List[String]) {
    sendMessage(projectKey, title)
    messages.foreach { message =>
      sendMessage(projectKey, message)
    }
  }

  private def sendMessage(projectKey: String, message: String) {
    val settings: PluginSettings = pluginSettingsFactory.createGlobalSettings
    if (isIrcBotEnable(settings) == false || isIrcBotChannelEnable(settings, projectKey) == false) {
      return
    }
    try {
      autoConnect(settings)
      val channelName: String = getChannelName(settings, projectKey)
      pircBot.joinChannel(channelName)
      pircBot.sendMessage(channelName, message)
    } catch {
      case e: NickAlreadyInUseException => {
        LOGGER.error("catch Exception", e)
      }
      case e: IOException => {
        LOGGER.error("catch Exception", e)
      }
      case e: IrcException => {
        LOGGER.error("catch Exception", e)
      }
    }
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

  private def getFormattedUser(userData: UserData): String = {
    "%s".format(userData.getDisplayName)
  }

  private def getFormattedCommentText(commentData: CommentData): String = {
    "%s".format(commentData.getMessage)
  }

  @EventListener
  def onReviewComment(event: ReviewCommentEvent) {
    val reviewId = event.getReviewId
    val commentId = event.getCommentId
    val comment = this.reviewService.getComment(commentId)
    sendMessage(getProjectDataByReviewId(reviewId).getKey, "コメントが投稿されました")
  }

  @EventListener def onReviewStateChange(event: ReviewStateChangedEvent) {
    val reviewId = event.getReviewId
    sendMessage(getProjectDataByReviewId(reviewId).getKey, "status moved from " + event.getOldState.name + " to " + event.getNewState.name)
  }

  def destroy {
    LOGGER.debug("Unregister review event listener")
    eventPublisher.unregister(this)
  }

  def afterPropertiesSet {
    LOGGER.debug("Register review event listener")
    eventPublisher.register(this)
  }

  private final val pircBot: PircBot = new PircBot {
  }
}
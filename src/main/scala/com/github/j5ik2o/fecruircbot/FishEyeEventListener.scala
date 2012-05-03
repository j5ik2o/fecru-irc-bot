package com.github.j5ik2o.fecruircbot

import com.atlassian.event.api.EventListener
import com.atlassian.event.api.EventPublisher
import com.atlassian.fisheye.event.CommitEvent
import com.atlassian.fisheye.spi.services.RevisionDataService
import com.atlassian.sal.api.ApplicationProperties
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory

class FishEyeEventListener
(
  eventPublisher: EventPublisher,
  applicationProperties: ApplicationProperties,
  revisionDataService: RevisionDataService,
  protected val pluginSettingsFactory: PluginSettingsFactory,
  protected val ircBotGlobalConfigRepository: IrcBotGlobalConfigRepository,
  ircBotRepositoryChannelConfigRepository: IrcBotRepositoryChannelConfigRepository
  ) extends DisposableBean with InitializingBean with IrcConfigAccess {

  protected val name = "fe-irc-bot"

  eventPublisher.register(this)

  protected def isIrcBotChannelEnable(key: String) = {
    ircBotRepositoryChannelConfigRepository.resolve(key) match{
      case Some(IrcBotRepositoryChannelConfig(true, _, _)) => true
      case _ => false
    }
  }

  protected def getIrcBotChannelName(key: String) =
    ircBotRepositoryChannelConfigRepository.resolve(key).get.getChannelName()

  protected def isIrcBotChannelNotice(key: String) =
    ircBotRepositoryChannelConfigRepository.resolve(key).get.getNotice()

  @EventListener
  def onCommit(event: CommitEvent) {
    val repoName = event.getRepositoryName
    val changeSet = revisionDataService.getChangeset(repoName, event.getChangeSetId)
    import scala.collection.JavaConverters._
    sendMessages(
      event.getRepositoryName,
      (List(
        "[%s] リポジトリに変更がコミットされました".format(repoName),
        "Comment: %s".format(changeSet.getComment),
        "Author: %s".format(changeSet.getAuthor),
        "Branch: %s".format(changeSet.getBranch),
        "Date: %s".format(dateFormat.format(changeSet.getDate))) ++
        changeSet.getFileRevisions.asScala.map {
          e => "r%s: %s".format(e.getRev, e.getPath)
        }.toList ++
        List(
          "Changeset: %s/changelog/%s/cs=%s".format(applicationProperties.getBaseUrl, repoName, event.getChangeSetId)
        )).toSeq
    )
  }

  def destroy {
    eventPublisher.unregister(this)
  }

  def afterPropertiesSet {
    eventPublisher.register(this)
  }

  def onMessage(channel: String, sender: String, login: String, hostname: String, message: String) {
    LOGGER.info("c = %s, s = %s, l = %s, h = %s, m = %s".format(channel, sender, login, hostname, message))
  }

}
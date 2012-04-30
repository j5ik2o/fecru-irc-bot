package com.github.j5ik2o.fecruircbot

import com.atlassian.event.api.EventListener
import com.atlassian.event.api.EventPublisher
import com.atlassian.fisheye.event.CommitEvent
import com.atlassian.fisheye.spi.services.RevisionDataService
import com.atlassian.sal.api.ApplicationProperties
import org.jibble.pircbot.PircBot
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import java.util.StringTokenizer

class FishEyeEventListener
(
  eventPublisher: EventPublisher,
  applicationProperties: ApplicationProperties,
  revisionDataService: RevisionDataService ,
  protected val pluginSettingsFactory: PluginSettingsFactory
  ) extends DisposableBean with InitializingBean with IrcConfigAccess {

  protected val name = "fe-irc-bot"

  eventPublisher.register(this)

  def getKey(repositoryName:String):String =
    "fe_%s".format(repositoryName)

  @EventListener
  def onCommit(event: CommitEvent) {
    val repoName = event.getRepositoryName()

    val cs = revisionDataService.getChangeset(repoName, event.getChangeSetId())

    sendMessages(
      getKey(event.getRepositoryName),
      List(
        "%s リポジトリにコミットされました".format(repoName),
        "Comment: %s".format(cs.getComment),
        "Author: %s".format(cs.getAuthor),
        "Branche: %s".format(cs.getBranch),
        "Changeset: %s/changelog/%s/cs=%s".format(applicationProperties.getBaseUrl, repoName, event.getChangeSetId)
      ).toSeq
    )
  }

  def destroy {
    eventPublisher.unregister(this)
  }

  def afterPropertiesSet {
    eventPublisher.register(this)
  }

}
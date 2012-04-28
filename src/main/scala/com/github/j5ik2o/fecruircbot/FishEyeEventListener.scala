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

class FishEyeEventListener
(
  eventPublisher: EventPublisher,
  applicationProperties: ApplicationProperties,
  revisionDataService: RevisionDataService ,
  protected val pluginSettingsFactory: PluginSettingsFactory
  ) extends DisposableBean with InitializingBean with IrcConfigAccess {

  protected val name = "fe-irc-bot"

  eventPublisher.register(this)

  @EventListener
  def onCommit(event: CommitEvent) {
    LOGGER.info("event = " + event.toString)
  }

  def destroy {
    LOGGER.debug("Unregister commit event listener")
    eventPublisher.unregister(this)
  }

  def afterPropertiesSet {
    LOGGER.debug("Register commit event listener")
    eventPublisher.register(this)
  }

  private final val pircBot: PircBot = new PircBot {
  }
}
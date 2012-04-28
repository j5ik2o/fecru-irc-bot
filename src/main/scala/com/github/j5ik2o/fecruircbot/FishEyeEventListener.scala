package com.github.j5ik2o.fecruircbot

import com.atlassian.event.api.EventListener
import com.atlassian.event.api.EventPublisher
import com.atlassian.fisheye.event.CommitEvent
import com.atlassian.fisheye.spi.services.RevisionDataService
import com.atlassian.sal.api.ApplicationProperties
import org.jibble.pircbot.PircBot
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean

class FishEyeEventListener
(
  eventPublisher: EventPublisher, applicationProperties: ApplicationProperties, revisionDataService: RevisionDataService
  ) extends DisposableBean with InitializingBean {

  eventPublisher.register(this)

  private val LOGGER = LoggerFactory.getLogger("atlassian.plugin")

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
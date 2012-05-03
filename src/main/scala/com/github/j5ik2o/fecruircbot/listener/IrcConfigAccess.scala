package com.github.j5ik2o.fecruircbot.listener

import java.io.IOException
import org.jibble.pircbot.{PircBot, IrcException, NickAlreadyInUseException}
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import scala.Predef._
import com.github.j5ik2o.fecruircbot.domain.{IrcBotGlobalConfig, IrcBotGlobalConfigRepository}

trait IrcConfigAccess {

  self =>

  protected val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

  protected val LOGGER = LoggerFactory.getLogger("atlassian.plugin")
  protected val ircBotGlobalConfigRepository: IrcBotGlobalConfigRepository
  protected val name: String


  protected def autoConnect: Boolean = {
    if (pircBot.isConnected == false) {
      ircBotGlobalConfigRepository.resolve match {
        case Some(IrcBotGlobalConfig(true, host, port)) =>
          if (port != 0) {
            pircBot.connect(host, port);
            true
          } else {
            pircBot.connect(host);
            true
          }
        case _ => false
      }
    } else true
  }

  protected def sendMessages(key: String, messages: Seq[String]) {
    messages.foreach {
      message =>
        sendMessage(key, message)
    }
  }

  protected def isIrcBotChannelEnable(key: String): Boolean

  protected def isIrcBotChannelNotice(key: String): Boolean

  protected def getIrcBotChannelName(key: String): String

  protected def isIrcBotEnable: Boolean = {
    ircBotGlobalConfigRepository.resolve match {
      case Some(IrcBotGlobalConfig(true, _, _)) => true
      case Some(IrcBotGlobalConfig(false, _, _)) => false
      case _ => false
    }
  }

  protected def isEnableChannel(key: String) = {
    ircBotGlobalConfigRepository.resolve match {
      case Some(IrcBotGlobalConfig(true, _, _)) => isIrcBotChannelEnable(key)
      case Some(IrcBotGlobalConfig(false, _, _)) => false
      case _ => false
    }
  }


  protected def sendMessage(key: String, message: String) {
    if (autoConnect == false) {
      LOGGER.info("autoConnect = false")
      return
    }
    if (isEnableChannel(key) == false) {
      LOGGER.info("isEnableChannel = false")
      return
    }
    try {
      val channelName = getIrcBotChannelName(key)
      pircBot.joinChannel(channelName)
      if (isIrcBotChannelNotice(key)) {
        pircBot.sendNotice(channelName, message)
        LOGGER.info("sendNotice(%s,%s)".format(channelName, message))
      } else {
        pircBot.sendMessage(channelName, message)
        LOGGER.info("sendMessage(%s,%s)".format(channelName, message))
      }
    } catch {
      case e: NickAlreadyInUseException =>
        LOGGER.error("catch Exception", e)
      case e: IOException =>
        LOGGER.error("catch Exception", e)
      case e: IrcException =>
        LOGGER.error("catch Exception", e)
    }
  }

  def onMessage(channel: String, sender: String, login: String, hostname: String, message: String)

  protected lazy val pircBot = new PircBot {
    setName(name)

    override def onMessage(channel: String, sender: String, login: String, hostname: String, message: String) {
      self.onMessage(channel, sender, login, hostname, message)
    }
  }

}

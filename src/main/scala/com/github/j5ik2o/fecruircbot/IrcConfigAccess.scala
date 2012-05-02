package com.github.j5ik2o.fecruircbot

import java.io.IOException
import org.jibble.pircbot.{PircBot, IrcException, NickAlreadyInUseException}
import org.slf4j.LoggerFactory
import com.atlassian.sal.api.pluginsettings.{PluginSettingsFactory, PluginSettings}
import java.text.SimpleDateFormat

trait IrcConfigAccess {

  self =>

  protected val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

  protected val LOGGER = LoggerFactory.getLogger("atlassian.plugin")
  protected val pluginSettingsFactory: PluginSettingsFactory
  protected val name: String

  protected def isIrcBotEnable(settings: PluginSettings) = {
    val r = settings.get(classOf[IrcBotGlobalConfig].getName + ".enable")
    if (r != null)
      r.asInstanceOf[String].toBoolean
    else
      false
  }

  protected def getIrcServerName(settings: PluginSettings): Option[String] = {
    val r = settings.get(classOf[IrcBotGlobalConfig].getName + ".ircServerName")
    if (r != null) {
      val ircServerName = r.asInstanceOf[String]
      if (ircServerName.isEmpty == false) {
        Some(ircServerName)
      } else {
        None
      }
    } else
      None
  }

  protected def getIrcServerPort(settings: PluginSettings): Option[Int] = {
    val r = settings.get(classOf[IrcBotGlobalConfig].getName + ".ircServerPort")
    if (r != null) {
      val ircServerPort = r.asInstanceOf[String]
      if (ircServerPort.isEmpty == false) {
        Some(ircServerPort.toInt)
      } else {
        None
      }
    } else
      None
  }

  protected def autoConnect(settings: PluginSettings) {
    if (pircBot.isConnected) {
      return
    }
    val ircServerName = getIrcServerName(settings).get
    LOGGER.debug("irc server name = " + ircServerName)
    val ircServerPort = getIrcServerPort(settings)
    LOGGER.debug("irc server port = " + ircServerPort)
    if (ircServerPort != null && ircServerPort.getOrElse(0) != 0) {
      pircBot.connect(ircServerName, ircServerPort.get)
    } else {
      pircBot.connect(ircServerName)
    }
  }

  protected def sendMessages(key: String, messages: Seq[String]) {
    messages.foreach {
      message =>
        sendMessage(key, message)
    }
  }

  protected def isIrcBotChannelEnable(settings: PluginSettings, key: String):Boolean
  protected def isIrcBotChannelNotice(settings: PluginSettings, key: String):Boolean
  protected def getIrcBotChannelName(settings: PluginSettings, key: String):String


  protected def sendMessage(key: String, message: String) {
    val settings = pluginSettingsFactory.createGlobalSettings
    if (isIrcBotEnable(settings) == false ||
      isIrcBotChannelEnable(settings, key) == false) {
      LOGGER.info("有効になっていません (%s)".format(key))
      return
    }
    try {
      autoConnect(settings)
      val channelName = getIrcBotChannelName(settings, key)
      pircBot.joinChannel(channelName)
      if (isIrcBotChannelNotice(settings, key)) {
        pircBot.sendNotice(channelName, message)
      } else {
        pircBot.sendMessage(channelName, message)
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

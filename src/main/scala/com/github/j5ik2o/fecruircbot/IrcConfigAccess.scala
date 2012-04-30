package com.github.j5ik2o.fecruircbot

import java.io.IOException
import org.jibble.pircbot.{PircBot, IrcException, NickAlreadyInUseException}
import org.slf4j.LoggerFactory
import com.atlassian.sal.api.pluginsettings.{PluginSettingsFactory, PluginSettings}

trait IrcConfigAccess {

  protected val LOGGER = LoggerFactory.getLogger("atlassian.plugin")
  protected val pluginSettingsFactory: PluginSettingsFactory
  protected val name: String

  protected def getChannelName(settings: PluginSettings, projectId: String) =
    settings.get(classOf[IrcBotProjectChannelConfig].getName + "_" + projectId + ".channelName").asInstanceOf[String]

  protected def isIrcBotChannelEnable(settings: PluginSettings, projectId: String) =
    settings.get(classOf[IrcBotProjectChannelConfig].getName + "_" + projectId + ".enable").asInstanceOf[String].toBoolean

  protected def isIrcBotChannelNotice(settings: PluginSettings, projectId: String) =
    settings.get(classOf[IrcBotProjectChannelConfig].getName + "_" + projectId + ".notice").asInstanceOf[String].toBoolean

  protected def isIrcBotEnable(settings: PluginSettings) =
    settings.get(classOf[IrcBotGlobalConfig].getName + ".enable").asInstanceOf[String].toBoolean

  protected def getIrcServerName(settings: PluginSettings): Option[String] = {
    val ircServerName = settings.get(classOf[IrcBotGlobalConfig].getName + ".ircServerName").asInstanceOf[String]
    if (ircServerName.isEmpty == false) {
      Some(ircServerName)
    } else {
      None
    }
  }

  protected def getIrcServerPort(settings: PluginSettings): Option[Int] = {
    val ircServerPort = settings.get(classOf[IrcBotGlobalConfig].getName + ".ircServerPort").asInstanceOf[String]
    if (ircServerPort.isEmpty == false) {
      Some(ircServerPort.toInt)
    } else {
      None
    }
  }

  protected def autoConnect(settings: PluginSettings) {
    if (pircBot.isConnected) {
      return
    }
    val ircServerName = getIrcServerName(settings).getOrElse("")
    LOGGER.debug("irc server name = " + ircServerName)
    val ircServerPort = getIrcServerPort(settings)
    LOGGER.debug("irc server port = " + ircServerPort)
    if (ircServerPort != null && ircServerPort.getOrElse(0) != 0) {
      pircBot.connect(ircServerName, ircServerPort.get)
    } else {
      pircBot.connect(ircServerName)
    }
  }

  protected def sendMessages(projectKey: String, messages: Seq[String]) {
    messages.foreach {
      message =>
        sendMessage(projectKey, message)
    }
  }

  protected def sendMessage(key: String, message: String) {
    val settings = pluginSettingsFactory.createGlobalSettings
    if (isIrcBotEnable(settings) == false ||
      isIrcBotChannelEnable(settings, key) == false) {
      return
    }
    try {
      autoConnect(settings)
      val channelName = getChannelName(settings, key)
      pircBot.joinChannel(channelName)
      pircBot.sendMessage(channelName, message)
    } catch {
      case e: NickAlreadyInUseException =>
        LOGGER.error("catch Exception", e)
      case e: IOException =>
        LOGGER.error("catch Exception", e)
      case e: IrcException =>
        LOGGER.error("catch Exception", e)
    }
  }

  private lazy val pircBot = new PircBot {
    setName(name)
  }

}

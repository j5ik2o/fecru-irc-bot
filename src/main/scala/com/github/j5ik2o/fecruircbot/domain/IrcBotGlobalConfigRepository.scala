package com.github.j5ik2o.fecruircbot.domain

import com.atlassian.sal.api.pluginsettings.{PluginSettingsFactory, PluginSettings}
import com.atlassian.sal.api.transaction.{TransactionCallback, TransactionTemplate}
import org.slf4j.LoggerFactory

class IrcBotGlobalConfigRepository
(
  pluginSettingsFactory: PluginSettingsFactory,
  transactionTemplate: TransactionTemplate
  ) {

  private val LOGGER = LoggerFactory.getLogger("atlassian.plugin")

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

  def save(ircBotGlobalConfig: IrcBotGlobalConfig) {
    LOGGER.info("ircBotGlobalConfig = %s".format(ircBotGlobalConfig))
    transactionTemplate.execute(new TransactionCallback[Unit] {
      def doInTransaction = {
        LOGGER.info("transaction start")
        val pluginSettings = pluginSettingsFactory.createGlobalSettings
        pluginSettings.put(classOf[IrcBotGlobalConfig].getName + ".enable", ircBotGlobalConfig.getEnable.toString)
        pluginSettings.put(classOf[IrcBotGlobalConfig].getName + ".ircServerName", ircBotGlobalConfig.getIrcServerName)
        pluginSettings.put(classOf[IrcBotGlobalConfig].getName + ".ircServerPort", ircBotGlobalConfig.getIrcServerPort.toString)
        LOGGER.info("transaction end")
      }
    })
  }

  def resolve: Option[IrcBotGlobalConfig] = {
    val pluginSettings = pluginSettingsFactory.createGlobalSettings
    transactionTemplate.execute(new TransactionCallback[Option[IrcBotGlobalConfig]] {
      def doInTransaction = {
        val enable = isIrcBotEnable(pluginSettings)
        val ircServerName = getIrcServerName(pluginSettings)
        if (enable &&
          getIrcServerName(pluginSettings) != None) {
          Some(
            new IrcBotGlobalConfig(
              enable,
              ircServerName.get,
              getIrcServerPort(pluginSettings).getOrElse(0)
            )
          )
        } else None
      }
    })

  }
}
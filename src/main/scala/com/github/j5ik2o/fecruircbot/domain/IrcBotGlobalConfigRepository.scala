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
    transactionTemplate.execute(new TransactionCallback[Option[IrcBotGlobalConfig]] {
      def doInTransaction = {
        val pluginSettings = pluginSettingsFactory.createGlobalSettings
        val enable = pluginSettings.get(classOf[IrcBotGlobalConfig].getName + ".enable")
        val ircServerName = pluginSettings.get(classOf[IrcBotGlobalConfig].getName + ".ircServerName")
        val ircServerPort = pluginSettings.get(classOf[IrcBotGlobalConfig].getName + ".ircServerPort")
        if (enable == null || ircServerName == null || ircServerPort == null) {
          None
        } else {
          Some(
            new IrcBotGlobalConfig(
              enable.toString.toBoolean,
              ircServerName.toString,
              ircServerPort.toString.toInt
            )
          )
        }
      }
    })

  }
}

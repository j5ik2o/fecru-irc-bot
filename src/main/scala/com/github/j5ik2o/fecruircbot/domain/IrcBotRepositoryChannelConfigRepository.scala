package com.github.j5ik2o.fecruircbot.domain

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.transaction.{TransactionCallback, TransactionTemplate}

class IrcBotRepositoryChannelConfigRepository
(
  pluginSettingsFactory: PluginSettingsFactory,
  transactionTemplate: TransactionTemplate
  ) {

  def save(key: String, ircBotRepositoryChannelConfig: IrcBotRepositoryChannelConfig) {
    transactionTemplate.execute(new TransactionCallback[Unit] {
      def doInTransaction = {
        val pluginSettings = pluginSettingsFactory.createGlobalSettings
        pluginSettings.put(classOf[IrcBotRepositoryChannelConfig].getName + "_" + key + ".enable", ircBotRepositoryChannelConfig.enable.toString)
        pluginSettings.put(classOf[IrcBotRepositoryChannelConfig].getName + "_" + key + ".notice", ircBotRepositoryChannelConfig.notice.toString)
        pluginSettings.put(classOf[IrcBotRepositoryChannelConfig].getName + "_" + key + ".channelName", ircBotRepositoryChannelConfig.getChannelName)
      }
    })
  }

  def resolve(key: String): Option[IrcBotRepositoryChannelConfig] = {
    transactionTemplate.execute(new TransactionCallback[Option[IrcBotRepositoryChannelConfig]] {
      def doInTransaction = {
        val settings = pluginSettingsFactory.createGlobalSettings
        val enable = settings.get(classOf[IrcBotRepositoryChannelConfig].getName + "_" + key + ".enable")
        val notice = settings.get(classOf[IrcBotRepositoryChannelConfig].getName + "_" + key + ".notice")
        val channelName = settings.get(classOf[IrcBotRepositoryChannelConfig].getName + "_" + key + ".channelName")
        if (enable == null || notice == null || channelName == null) {
          None
        } else {
          Some(
            new IrcBotRepositoryChannelConfig(
              enable.toString.toBoolean,
              notice.toString.toBoolean,
              channelName.toString
            )
          )
        }
      }
    })
  }

}

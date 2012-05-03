package com.github.j5ik2o.fecruircbot

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.transaction.{TransactionCallback, TransactionTemplate}



class IrcBotProjectChannelConfigRepository
(
  pluginSettingsFactory: PluginSettingsFactory,
  transactionTemplate: TransactionTemplate
  ) {

  def save(key:String, ircBotProjectChannelConfig: IrcBotProjectChannelConfig) {
    transactionTemplate.execute(new TransactionCallback[Unit] {
      def doInTransaction = {
        val pluginSettings = pluginSettingsFactory.createGlobalSettings
        pluginSettings.put(classOf[IrcBotProjectChannelConfig].getName + "_" + key + ".enable", ircBotProjectChannelConfig.enable.toString)
        pluginSettings.put(classOf[IrcBotProjectChannelConfig].getName + "_" + key + ".notice", ircBotProjectChannelConfig.notice.toString)
        pluginSettings.put(classOf[IrcBotProjectChannelConfig].getName + "_" + key + ".channelName", ircBotProjectChannelConfig.getChannelName)
      }
    })
  }

  def resolve(key: String): Option[IrcBotProjectChannelConfig] = {
    transactionTemplate.execute(new TransactionCallback[Option[IrcBotProjectChannelConfig]] {
      def doInTransaction = {
        val settings = pluginSettingsFactory.createGlobalSettings
        val enable = settings.get(classOf[IrcBotProjectChannelConfig].getName + "_" + key + ".enable")
        val notice = settings.get(classOf[IrcBotProjectChannelConfig].getName + "_" + key + ".notice")
        val channelName = settings.get(classOf[IrcBotProjectChannelConfig].getName + "_" + key + ".channelName")
        if (enable == null || notice == null || channelName == null) {
          None
        } else {
          Some(
            new IrcBotProjectChannelConfig(
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

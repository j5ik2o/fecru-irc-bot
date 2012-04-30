package com.github.j5ik2o.fecruircbot

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.transaction.TransactionCallback
import com.atlassian.sal.api.transaction.TransactionTemplate
import com.atlassian.sal.api.user.UserManager
import javax.ws.rs._

@Path("/projectChannelConfig")
class IrcBotProjectChannelConfigResource
(
  userManager: UserManager,
  pluginSettingsFactory: PluginSettingsFactory,
  transactionTemplate: TransactionTemplate
  ) {

  private val LOGGER = LoggerFactory.getLogger(classOf[ProjectServlet])

  @GET
  @Path("{key}")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def get(@PathParam("key") key: String,
          @Context request: HttpServletRequest): Response = {
    LOGGER.debug(String.format("get : start(%s,%s)", key, request))
    val response = Response.ok(transactionTemplate.execute(new TransactionCallback[IrcBotProjectChannelConfig] {
      def doInTransaction = {
        val settings = pluginSettingsFactory.createGlobalSettings
        val config = new IrcBotProjectChannelConfig
        val enable = settings.get(classOf[IrcBotProjectChannelConfig].getName + "_" + key + ".enable")
        config.enable = if (enable != null) enable.asInstanceOf[String].toBoolean else false
        val notice = settings.get(classOf[IrcBotProjectChannelConfig].getName + "_" + key + ".notice")
        config.notice = if (notice != null) notice.asInstanceOf[String].toBoolean else false
        val channelName = settings.get(classOf[IrcBotProjectChannelConfig].getName + "_" + key + ".channelName")
        config.channelName = if (channelName != null) channelName.asInstanceOf[String] else ""
        config
      }
    })).build
    LOGGER.debug(String.format("get : finished(%s)", response))
    response
  }

  @PUT
  @Path("{key}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def put(@PathParam("key") projectKey: String,
          config: IrcBotProjectChannelConfig,
          @Context request: HttpServletRequest): Response = {
    transactionTemplate.execute(new TransactionCallback[Unit] {
      def doInTransaction = {
        val pluginSettings = pluginSettingsFactory.createGlobalSettings
        pluginSettings.put(classOf[IrcBotProjectChannelConfig].getName + "_" + projectKey + ".enable", config.enable.toString)
        pluginSettings.put(classOf[IrcBotProjectChannelConfig].getName + "_" + projectKey + ".notice", config.notice.toString)
        pluginSettings.put(classOf[IrcBotProjectChannelConfig].getName + "_" + projectKey + ".channelName", config.getChannelName)
      }
    })
    Response.noContent.build
  }

}
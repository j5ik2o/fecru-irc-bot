package com.github.j5ik2o.fecruircbot

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.atlassian.sal.api.pluginsettings.PluginSettings
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.transaction.TransactionCallback
import com.atlassian.sal.api.transaction.TransactionTemplate
import com.atlassian.sal.api.user.UserManager

@Path("/channelConfig")
class IrcBotChannelConfigResource
(
  userManager: UserManager,
  pluginSettingsFactory: PluginSettingsFactory,
  transactionTemplate: TransactionTemplate
  ) {

  private val LOGGER = LoggerFactory.getLogger(classOf[ProjectServlet])

  @GET
  @Path("{projectKey}")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def get(@PathParam("projectKey") projectKey: String,
          @Context request: HttpServletRequest): Response = {
    LOGGER.debug(String.format("get : start(%s,%s)", projectKey, request))
    val response = Response.ok(transactionTemplate.execute(new TransactionCallback[IrcBotChannelConfig] {
      def doInTransaction = {
        val settings = pluginSettingsFactory.createGlobalSettings
        val config = new IrcBotChannelConfig
        config.enable = settings.get(classOf[IrcBotChannelConfig].getName + "_" + projectKey + ".enable").asInstanceOf[String].toBoolean
        config.notice = settings.get(classOf[IrcBotChannelConfig].getName + "_" + projectKey + ".notice").asInstanceOf[String].toBoolean
        val channelName = settings.get(classOf[IrcBotChannelConfig].getName + "_" + projectKey + ".channelName").asInstanceOf[String]
        if (channelName != null) {
          config.channelName = channelName
        }
        config
      }
    })).build
    LOGGER.debug(String.format("get : finished(%s)", response))
    response
  }

  @PUT
  @Path("{projectKey}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def put(@PathParam("projectKey") projectKey: String,
          config: IrcBotChannelConfig,
          @Context request: HttpServletRequest): Response = {
    transactionTemplate.execute(new TransactionCallback[Unit] {
      def doInTransaction = {
        val pluginSettings: PluginSettings = pluginSettingsFactory.createGlobalSettings
        pluginSettings.put(classOf[IrcBotChannelConfig].getName + "_" + projectKey + ".enable", config.enable.toString)
        pluginSettings.put(classOf[IrcBotChannelConfig].getName + "_" + projectKey + ".notice", config.notice.toString)
        pluginSettings.put(classOf[IrcBotChannelConfig].getName + "_" + projectKey + ".channelName", config.getChannelName)
      }
    })
    Response.noContent.build
  }

}
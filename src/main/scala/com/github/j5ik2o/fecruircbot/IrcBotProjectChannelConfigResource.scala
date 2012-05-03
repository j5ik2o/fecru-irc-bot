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
  ircBotProjectChannelConfigRepository: IrcBotProjectChannelConfigRepository
  ) {

  private val LOGGER = LoggerFactory.getLogger(classOf[ProjectServlet])

  @GET
  @Path("{key}")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def get(@PathParam("key") key: String,
          @Context request: HttpServletRequest): Response = {
    LOGGER.debug(String.format("get : start(%s,%s)", key, request))
    val response = Response.ok(
      ircBotProjectChannelConfigRepository.
        resolve(key).
        getOrElse(new IrcBotProjectChannelConfig())
    ).build
    LOGGER.debug(String.format("get : finished(%s)", response))
    response
  }

  @PUT
  @Path("{key}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def put(@PathParam("key") key: String,
          config: IrcBotProjectChannelConfig,
          @Context request: HttpServletRequest): Response = {
    ircBotProjectChannelConfigRepository.save(key, config)
    Response.noContent.build
  }

}
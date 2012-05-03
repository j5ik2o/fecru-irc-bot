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
import org.slf4j.LoggerFactory
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.transaction.TransactionCallback
import com.atlassian.sal.api.transaction.TransactionTemplate
import com.atlassian.sal.api.user.UserManager

@Path("/repositoryChannelConfig")
class IrcBotRepositoryChannelConfigResource
(
  userManager: UserManager,
  ircBotRepositoryChannelConfigRepository:IrcBotRepositoryChannelConfigRepository
  ) {

  private val LOGGER = LoggerFactory.getLogger(classOf[ProjectServlet])

  @GET
  @Path("{key}")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def get(@PathParam("key") key: String,
          @Context request: HttpServletRequest): Response = {
    LOGGER.debug(String.format("get : start(%s,%s)", key, request))
    val response = Response.ok(
      ircBotRepositoryChannelConfigRepository.
        resolve(key).
        getOrElse(new IrcBotRepositoryChannelConfig())
    ).build
    LOGGER.debug(String.format("get : finished(%s)", response))
    response
  }

  @PUT
  @Path("{key}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def put(@PathParam("key") key: String,
          config: IrcBotRepositoryChannelConfig,
          @Context request: HttpServletRequest): Response = {
    ircBotRepositoryChannelConfigRepository.save(key, config)
    Response.noContent.build
  }

}
package com.github.j5ik2o.fecruircbot

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status
import org.slf4j.LoggerFactory
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.transaction.TransactionCallback
import com.atlassian.sal.api.transaction.TransactionTemplate
import com.atlassian.sal.api.user.UserManager

@Path("/globalConfig")
class IrcBotGlobalConfigResource
(
  userManager: UserManager,
  ircBotGlobalConfigRepository: IrcBotGlobalConfigRepository
  ) {

  protected val LOGGER = LoggerFactory.getLogger("atlassian.plugin")

  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  def get(@Context request: HttpServletRequest): Response = {
    LOGGER.debug(String.format("get : start(%s)", request))
    val username = userManager.getRemoteUsername(request)
    if (username != null && !userManager.isSystemAdmin(username)) {
      LOGGER.debug(String.format("get : finished(%s)", request))
      return Response.status(Status.UNAUTHORIZED).build
    }
    val result = Response.ok(
      ircBotGlobalConfigRepository.
        resolve.
        getOrElse(new IrcBotGlobalConfig())
    ).build
    LOGGER.debug(String.format("get : finished(%s)", result))
    result
  }

  @PUT
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def put(config: IrcBotGlobalConfig,
          @Context request: HttpServletRequest): Response = {
    val username = userManager.getRemoteUsername(request)
    LOGGER.info("userName = %s".format(username))
    if (username != null && !userManager.isSystemAdmin(username)) {
      return Response.status(Status.UNAUTHORIZED).build
    }
    LOGGER.info("save start = %s".format(config))
    ircBotGlobalConfigRepository.save(config)
    LOGGER.info("save end = %s".format(config))

    Response.noContent.build
  }
}
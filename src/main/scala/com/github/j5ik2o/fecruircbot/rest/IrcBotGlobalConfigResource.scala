package com.github.j5ik2o.fecruircbot.rest

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
import com.atlassian.sal.api.user.UserManager
import com.github.j5ik2o.fecruircbot.domain.{IrcBotGlobalConfigRepository, IrcBotGlobalConfig}
import java.lang.String

/**
 * [[com.github.j5ik2o.fecruircbot.domain.IrcBotGlobalConfig]]のリソース。
 *
 * @param userManager [[com.atlassian.sal.api.user.UserManager]]
 * @param ircBotGlobalConfigRepository [[com.github.j5ik2o.fecruircbot.domain.IrcBotGlobalConfigRepository]]
 */
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
    if (username != null && userManager.isSystemAdmin(username) == false) {
      LOGGER.debug(String.format("get : finished(%s)", request))
      Response.status(Status.UNAUTHORIZED).build
    } else {
      val result = Response.ok(
        ircBotGlobalConfigRepository.
          resolve.
          getOrElse(new IrcBotGlobalConfig())
      ).build
      LOGGER.debug(String.format("get : finished(%s)", result))
      result
    }
  }

  @PUT
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def put(config: IrcBotGlobalConfig,
          @Context request: HttpServletRequest): Response = {
    LOGGER.debug("put : start(%s)".format(request))
    val username = userManager.getRemoteUsername(request)
    LOGGER.info("userName = %s".format(username))
    if (username != null && userManager.isSystemAdmin(username) == false) {
      val response = Response.status(Status.UNAUTHORIZED).build
      LOGGER.debug("put : finished(%s)".format(response))
      response
    } else {
      ircBotGlobalConfigRepository.save(config)
      val response = Response.noContent.build
      LOGGER.debug("put : finished(%s)".format(response))
      response
    }
  }
}
package com.github.j5ik2o.fecruircbot.rest

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import com.atlassian.sal.api.user.UserManager
import com.github.j5ik2o.fecruircbot.servlet.ProjectServlet
import com.github.j5ik2o.fecruircbot.domain.{IrcBotProjectChannelConfig, IrcBotProjectChannelConfigRepository}
import javax.ws.rs._
import core.Response.Status

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
    LOGGER.debug("get : start(%s,%s)".format(key, request))
    val username = userManager.getRemoteUsername(request)
    LOGGER.debug("userName = %s".format(username))
    if (username != null && userManager.isSystemAdmin(username) == false) {
      val response = Response.status(Status.UNAUTHORIZED).build
      LOGGER.debug("get : finished(%s)".format(response))
      response
    } else {
      val response = Response.ok(
        ircBotProjectChannelConfigRepository.
          resolve(key).
          getOrElse(IrcBotProjectChannelConfig())
      ).build
      LOGGER.debug("get : finished(%s)".format(response))
      response
    }
  }

  @PUT
  @Path("{key}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def put(@PathParam("key") key: String,
          config: IrcBotProjectChannelConfig,
          @Context request: HttpServletRequest): Response = {
    LOGGER.debug("put : start(%s,%s,%s)".format(key, config, request))
    val username = userManager.getRemoteUsername(request)
    if (username != null && userManager.isSystemAdmin(username) == false) {
      val response = Response.status(Status.UNAUTHORIZED).build
      LOGGER.debug("get : finished(%s)".format(response))
      response
    } else {
      ircBotProjectChannelConfigRepository.save(key, config)
      val response = Response.noContent.build
      LOGGER.debug("get : finished(%s)".format(response))
      response
    }
  }

}
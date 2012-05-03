package com.github.j5ik2o.fecruircbot.rest

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
import com.atlassian.sal.api.user.UserManager
import com.github.j5ik2o.fecruircbot.servlet.ProjectServlet
import com.github.j5ik2o.fecruircbot.domain.{IrcBotRepositoryChannelConfig, IrcBotRepositoryChannelConfigRepository}
import javax.ws.rs.core.Response.Status

@Path("/repositoryChannelConfig")
class IrcBotRepositoryChannelConfigResource
(
  userManager: UserManager,
  ircBotRepositoryChannelConfigRepository: IrcBotRepositoryChannelConfigRepository
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
        ircBotRepositoryChannelConfigRepository.
          resolve(key).
          getOrElse(new IrcBotRepositoryChannelConfig())
      ).build
      LOGGER.debug(String.format("get : finished(%s)", response))
      response
    }
  }

  @PUT
  @Path("{key}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def put(@PathParam("key") key: String,
          config: IrcBotRepositoryChannelConfig,
          @Context request: HttpServletRequest): Response = {
    LOGGER.debug("put : start(%s,%s,%s)".format(key, config, request))
    val username = userManager.getRemoteUsername(request)
    LOGGER.info("userName = %s".format(username))
    if (username != null && userManager.isSystemAdmin(username) == false) {
      val response = Response.status(Status.UNAUTHORIZED).build
      LOGGER.debug("put : finished(%s)".format(response))
      response
    } else {
      ircBotRepositoryChannelConfigRepository.save(key, config)
      val response = Response.noContent.build
      LOGGER.debug("put : finished(%s)".format(response))
      response
    }
  }

}
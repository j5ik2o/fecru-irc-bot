package com.github.j5ik2o.fecruircbot.servlet

import org.slf4j.LoggerFactory
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.net.URI
import com.atlassian.sal.api.auth.LoginUriProvider

trait ServletSupport {

  protected val LOGGER = LoggerFactory.getLogger("atlassian.plugin")

  protected[this] val loginUriProvider: LoginUriProvider

  protected def redirectToLogin(request: HttpServletRequest, response: HttpServletResponse) {
    LOGGER.debug("redirectToLogin : start(%s, %s)".format(request, response))
    response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString)
    LOGGER.debug("redirectToLogin : finished")
  }

  protected def getUri(request: HttpServletRequest): URI = {
    LOGGER.debug("getUri : start(%s)".format(request))
    val builder = request.getRequestURL
    if (request.getQueryString != null) {
      builder.append("?")
      builder.append(request.getQueryString)
    }
    val result: URI = URI.create(builder.toString)
    LOGGER.debug("getUri : finished(%s)".format(result))
    result
  }
}

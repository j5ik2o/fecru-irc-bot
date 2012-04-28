package com.github.j5ik2o.fecruircbot

import com.atlassian.sal.api.user.UserManager
import com.atlassian.sal.api.auth.LoginUriProvider
import com.atlassian.templaterenderer.TemplateRenderer
import com.atlassian.sal.api.ApplicationProperties
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import java.net.URI
import org.slf4j.LoggerFactory

class AdminServlet
(
  userManager: UserManager,
  loginUriProvider: LoginUriProvider,
  templateRenderer: TemplateRenderer,
  applicationProperties: ApplicationProperties
  ) extends HttpServlet {

  private val LOGGER = LoggerFactory.getLogger("atlassian.plugin")
  private val isDebug = true

  override def doGet(request: HttpServletRequest, response: HttpServletResponse) {
    LOGGER.debug(String.format("doGet : start(%s, %s)", request, response))
    val username: String = userManager.getRemoteUsername(request)
    if (username != null && !userManager.isSystemAdmin(username)) {
      redirectToLogin(request, response)
      LOGGER.debug("doGet : finished")
      return
    }
    import scala.collection.JavaConverters._

    var context = Map.empty[String, AnyRef]

    val baseUriKey: String = "base-url"
    if (isDebug == false) {
      context += (baseUriKey -> applicationProperties.getBaseUrl)
    }
    else {
      context += (baseUriKey -> "http://localhost:3990/fecru")
    }
    response.setContentType("text/html;charset=utf-8")
    templateRenderer.render("admin.vm", context.asJava, response.getWriter)
    LOGGER.debug("doGet : finished")
  }

  private def redirectToLogin(request: HttpServletRequest, response: HttpServletResponse) {
    LOGGER.debug(String.format("redirectToLogin : start(%s, %s)", request, response))
    response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString)
    LOGGER.debug("redirectToLogin : finished")
  }

  private def getUri(request: HttpServletRequest): URI = {
    LOGGER.debug(String.format("getUri : start(%s)", request))
    val builder: StringBuffer = request.getRequestURL
    if (request.getQueryString != null) {
      builder.append("?")
      builder.append(request.getQueryString)
    }
    val result: URI = URI.create(builder.toString)
    LOGGER.debug(String.format("getUri : finished(%s)", result))
    return result
  }
}

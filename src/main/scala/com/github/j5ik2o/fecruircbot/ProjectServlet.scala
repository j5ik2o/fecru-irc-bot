package com.github.j5ik2o.fecruircbot

import java.net.URI
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.auth.LoginUriProvider
import com.atlassian.sal.api.user.UserManager
import com.atlassian.templaterenderer.TemplateRenderer

@SuppressWarnings(Array("serial"))
class ProjectServlet
(
  userManager: UserManager,
  loginUriProvider: LoginUriProvider,
  renderer: TemplateRenderer,
  applicationProperties: ApplicationProperties
  )
  extends HttpServlet {

  protected val LOGGER = LoggerFactory.getLogger("atlassian.plugin")

  override def doGet(request: HttpServletRequest, response: HttpServletResponse) {
    LOGGER.debug(String.format("doGet : start(%s, %s)", request, response))
    val username = userManager.getRemoteUsername(request)
    if (username != null && !userManager.isSystemAdmin(username)) {
      redirectToLogin(request, response)
      LOGGER.debug("doGet : finished")
      return
    }
    import scala.collection.JavaConverters._
    val key = request.getParameter("key")

    var context = Map.empty[String, AnyRef]
    response.setContentType("text/html;charset=utf-8")
    context += ("key" -> key)
    val baseUriKey = "base-url"
    if (isDebug == false) {
      context += (baseUriKey -> applicationProperties.getBaseUrl)
    }
    else {
      context += (baseUriKey -> "http://localhost:3990/fecru")
    }
    renderer.render("project.vm", context.asJava, response.getWriter)
    LOGGER.debug("doGet : finshed")
  }

  private def redirectToLogin(request: HttpServletRequest, response: HttpServletResponse) {
    LOGGER.debug(String.format("redirectToLogin : start(%s, %s)", request, response))
    response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString)
    LOGGER.debug("redirectToLogin : finshed")
  }

  private def getUri(request: HttpServletRequest): URI = {
    LOGGER.debug("getUri : start")
    val builder: StringBuffer = request.getRequestURL
    if (request.getQueryString != null) {
      builder.append("?")
      builder.append(request.getQueryString)
    }
    LOGGER.debug("getUri : finshed")
    URI.create(builder.toString)
  }

  private var isDebug: Boolean = true
}
package com.github.j5ik2o.fecruircbot.servlet

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.auth.LoginUriProvider
import com.atlassian.sal.api.user.UserManager
import com.atlassian.templaterenderer.TemplateRenderer

@SuppressWarnings(Array("serial"))
class ProjectServlet
(
  userManager: UserManager,
  protected[this] val loginUriProvider: LoginUriProvider,
  renderer: TemplateRenderer,
  applicationProperties: ApplicationProperties
  ) extends HttpServlet with ServletSupport {

  private val isDebug = true

  override def doGet(request: HttpServletRequest, response: HttpServletResponse) {
    LOGGER.debug("doGet : start(%s, %s)".format(request, response))
    val username = userManager.getRemoteUsername(request)
    if (username != null && userManager.isSystemAdmin(username) == false) {
      redirectToLogin(request, response)
      LOGGER.debug("doGet : finished")
    } else {
      import scala.collection.JavaConverters._
      val key = request.getParameter("key")
      var context = Map.empty[String, AnyRef]
      response.setContentType("text/html;charset=utf-8")
      context += ("key" -> key)
      val baseUriKey = "base-url"
      if (isDebug == false) {
        context += (baseUriKey -> applicationProperties.getBaseUrl)
      } else {
        context += (baseUriKey -> "http://localhost:3990/fecru")
      }
      renderer.render("project.vm", context.asJava, response.getWriter)
      LOGGER.debug("doGet : finshed")
    }
  }

}
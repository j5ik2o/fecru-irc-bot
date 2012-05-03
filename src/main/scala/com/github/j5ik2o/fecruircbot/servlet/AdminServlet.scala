package com.github.j5ik2o.fecruircbot.servlet

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
  protected[this] val loginUriProvider: LoginUriProvider,
  templateRenderer: TemplateRenderer,
  applicationProperties: ApplicationProperties
  ) extends HttpServlet with ServletSupport {

  private val isDebug = true

  override def doGet(request: HttpServletRequest, response: HttpServletResponse) {
    LOGGER.debug("doGet : start(%s, %s)".format(request, response))
    val username: String = userManager.getRemoteUsername(request)
    if (username != null && userManager.isSystemAdmin(username) == false) {
      redirectToLogin(request, response)
      LOGGER.debug("doGet : finished")
      return
    }
    import scala.collection.JavaConverters._

    var context = Map.empty[String, AnyRef]

    val baseUriKey = "base-url"
    if (isDebug == false) {
      context += (baseUriKey -> applicationProperties.getBaseUrl)
    } else {
      context += (baseUriKey -> "http://localhost:3990/fecru")
    }

    response.setContentType("text/html;charset=utf-8")
    templateRenderer.render("admin.vm", context.asJava, response.getWriter)
    LOGGER.debug("doGet : finished")
  }


}

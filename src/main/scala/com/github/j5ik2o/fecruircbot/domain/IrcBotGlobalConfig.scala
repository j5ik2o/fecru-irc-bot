package com.github.j5ik2o.fecruircbot.domain

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import reflect.BeanProperty

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
final class IrcBotGlobalConfig
(
  @BeanProperty @XmlElement var enable: Boolean,
  @BeanProperty @XmlElement var ircServerName: String,
  @BeanProperty @XmlElement var ircServerPort: Int
  )

object IrcBotGlobalConfig {

  def apply() = new IrcBotGlobalConfig(false, "", 0)

  def apply(enable: Boolean, ircServerName: String, ircServerPort: Int) =
    new IrcBotGlobalConfig(enable, ircServerName, ircServerPort)

  def unapply(ircBotGlobalConfig: IrcBotGlobalConfig): Option[(Boolean, String, Int)] =
    Some(ircBotGlobalConfig.enable,
      ircBotGlobalConfig.ircServerName,
      ircBotGlobalConfig.ircServerPort)

}

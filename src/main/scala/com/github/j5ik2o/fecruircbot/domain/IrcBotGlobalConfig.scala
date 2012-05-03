package com.github.j5ik2o.fecruircbot.domain

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import reflect.BeanProperty

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
final case class IrcBotGlobalConfig
(
  @BeanProperty @XmlElement var enable: Boolean,
  @BeanProperty @XmlElement var ircServerName: String,
  @BeanProperty @XmlElement var ircServerPort: Int
  ) {

  def this() = this(false, "", 0)

}


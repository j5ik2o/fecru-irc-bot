package com.github.j5ik2o.fecruircbot.domain

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import reflect.BeanProperty

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
final case class IrcBotRepositoryChannelConfig
(
  @BeanProperty @XmlElement var enable: Boolean,
  @BeanProperty @XmlElement var notice: Boolean,
  @BeanProperty @XmlElement var channelName: String
  ) {

  def this() = this(false, false, "")

}

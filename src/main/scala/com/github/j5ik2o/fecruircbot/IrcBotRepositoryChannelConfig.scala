package com.github.j5ik2o.fecruircbot

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import reflect.BeanProperty

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
final class IrcBotRepositoryChannelConfig
(
  @BeanProperty @XmlElement var enable: Boolean,
  @BeanProperty @XmlElement var notice: Boolean,
  @BeanProperty @XmlElement var channelName: String
  ) {

  def this() = this(false, false, "")

}

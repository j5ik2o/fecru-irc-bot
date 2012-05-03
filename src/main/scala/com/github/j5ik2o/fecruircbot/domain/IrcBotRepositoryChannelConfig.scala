package com.github.j5ik2o.fecruircbot.domain

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
  )

object IrcBotRepositoryChannelConfig {

  def apply() = new IrcBotRepositoryChannelConfig(false, false, "")

  def apply(enable: Boolean, notice: Boolean, channelName: String) =
    new IrcBotRepositoryChannelConfig(enable, notice, channelName)

  def unapply(ircBotRepositoryChannelConfig: IrcBotRepositoryChannelConfig): Option[(Boolean, Boolean, String)] =
    Some(ircBotRepositoryChannelConfig.enable,
      ircBotRepositoryChannelConfig.notice,
      ircBotRepositoryChannelConfig.channelName)
}

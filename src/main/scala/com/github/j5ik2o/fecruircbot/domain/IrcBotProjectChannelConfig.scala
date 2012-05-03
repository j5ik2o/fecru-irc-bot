package com.github.j5ik2o.fecruircbot.domain

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import reflect.BeanProperty

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
final class IrcBotProjectChannelConfig
(
  @BeanProperty @XmlElement var enable: Boolean,
  @BeanProperty @XmlElement var notice: Boolean,
  @BeanProperty @XmlElement var channelName: String
  )

object IrcBotProjectChannelConfig {

  def apply() = new IrcBotProjectChannelConfig(false, false, "")

  def apply(enable: Boolean, notice: Boolean, channelName: String) =
    new IrcBotProjectChannelConfig(enable, notice, channelName)

  def unapply(ircBotProjectChannelConfig:IrcBotProjectChannelConfig):Option[(Boolean,Boolean,String)] =
    Some(ircBotProjectChannelConfig.enable,
      ircBotProjectChannelConfig.notice,
      ircBotProjectChannelConfig.channelName)
}
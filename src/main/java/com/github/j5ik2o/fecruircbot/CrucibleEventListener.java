package com.github.j5ik2o.fecruircbot;

import java.io.IOException;
import java.util.List;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.crucible.event.ReviewCommentEvent;
import com.atlassian.crucible.event.ReviewCreatedEvent;
import com.atlassian.crucible.event.ReviewStateChangedEvent;
import com.atlassian.crucible.event.ReviewUpdatedEvent;
import com.atlassian.crucible.spi.PermId;
import com.atlassian.crucible.spi.data.DetailedReviewData;
import com.atlassian.crucible.spi.data.ProjectData;
import com.atlassian.crucible.spi.data.ReviewData;
import com.atlassian.crucible.spi.services.ProjectService;
import com.atlassian.crucible.spi.services.ReviewService;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class CrucibleEventListener implements DisposableBean,
    InitializingBean {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(CrucibleEventListener.class);
  private static final Logger log = LoggerFactory
      .getLogger(CrucibleEventListener.class);
  private final EventPublisher eventPublisher;
  private final ApplicationProperties applicationProperties;
  private final ReviewService reviewService;
  private final ProjectService projectService;

  private final PircBot pircBot = new PircBot() {
    {
      setName("cru-irc-bot");
    }
  };
  private PluginSettingsFactory pluginSettingsFactory;

  public CrucibleEventListener(EventPublisher eventPublisher,
      ApplicationProperties applicationProperties,
      ReviewService reviewService,
      ProjectService projectService,
      PluginSettingsFactory pluginSettingsFactory) {
    this.eventPublisher = eventPublisher;
    this.applicationProperties = applicationProperties;
    this.reviewService = reviewService;
    this.projectService = projectService;
    this.pluginSettingsFactory = pluginSettingsFactory;
    eventPublisher.register(this);
  }

  private String getChannelName(PluginSettings settings,
      String projectId) {
    return (String) settings.get(IrcBotChannelConfig.class
        .getName() + "_" + projectId + ".channelName");
  }

  private boolean isIrcBotChannelEnable(
      PluginSettings settings, String projectId) {
    return Boolean.parseBoolean((String) settings
        .get(IrcBotChannelConfig.class.getName() + "_"
            + projectId + ".enable"));
  }

  private boolean isIrcBotChannelNotice(
      PluginSettings settings, String projectId) {
    return Boolean.parseBoolean((String) settings
        .get(IrcBotChannelConfig.class.getName() + "_"
            + projectId + ".notice"));
  }

  private boolean isIrcBotEnable(PluginSettings settings) {
    boolean enable =
        Boolean.parseBoolean((String) settings
            .get(IrcBotGlobalConfig.class.getName()
                + ".enable"));
    return enable;
  }

  private String getIrcServerName(PluginSettings settings) {
    return (String) settings.get(IrcBotGlobalConfig.class
        .getName() + ".ircServerName");
  }

  private Integer getIrcServerPort(PluginSettings settings) {
    String ircServerPort =
        (String) settings.get(IrcBotGlobalConfig.class
            .getName() + ".ircServerPort");
    if (ircServerPort != null) {
      return Integer.parseInt(ircServerPort);
    }
    return null;
  }

  @EventListener
  public void onReviewCreate(ReviewCreatedEvent event) {
    PermId<ReviewData> reviewId = event.getReviewId();
    sendNotification(reviewId, "created");
  }

  private void autoConnect(PluginSettings settings)
      throws NickAlreadyInUseException, IOException,
      IrcException {
    if (pircBot.isConnected()) {
      return;
    }

    String ircServerName = getIrcServerName(settings);
    LOGGER.debug("irc server name = " + ircServerName);
    Integer ircServerPort = getIrcServerPort(settings);
    LOGGER.debug("irc server port = " + ircServerPort);
    if (ircServerPort != null
        && ircServerPort.intValue() != 0) {
      pircBot
          .connect(ircServerName, ircServerPort.intValue());
    } else {
      pircBot.connect(ircServerName);
    }

    // List<Project> projects = projectManager.getProjectObjects();
    // for (Project project : projects) {
    // String projectId = project.getId().toString();
    // String projectName = project.getName();
    // LOGGER.debug(String.format("projectName = %s, projectId = %s",
    // projectName, projectId));
    // if (isIrcBotEnable(settings)
    // && isIrcBotChannelEnable(settings, projectId)) {
    // String channelName = getChannelName(settings, projectId);
    // LOGGER.debug(String.format("channelName = %s", channelName));
    // pircBot.joinChannel(channelName);
    // }
    // }
  }

  private void sendNotification(PermId<ReviewData> reviewId,
      String message) {

    PluginSettings settings =
        pluginSettingsFactory.createGlobalSettings();

    DetailedReviewData review =
        this.reviewService.getReviewDetails(reviewId);
    ProjectData project =
        this.projectService
            .getProject(review.getProjectKey());

    String url =
        applicationProperties.getBaseUrl() + "/cru/"
            + reviewId.getId();
    String projectUrl =
        applicationProperties.getBaseUrl() + "/project/"
            + project.getKey();

    if (isIrcBotEnable(settings) == false
        || isIrcBotChannelEnable(settings, project.getKey()) == false) {
      return;
    }

    try {
      autoConnect(settings);
      String channelName =
          getChannelName(settings, project.getKey());
      pircBot.joinChannel(channelName);

      String msg =
          "Crucible review <a href=\"" + url + "\"><b>"
              + reviewId.getId() + "</b></a> " + message
              + " in Project <a href=\"" + projectUrl + "\">"
              + project.getName() + "</a>";

      pircBot.sendMessage(channelName, message);
    } catch (NickAlreadyInUseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IrcException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @EventListener
  public void onReviewUpdate(ReviewUpdatedEvent event) {
    PermId<ReviewData> reviewId = event.getReviewId();
    sendNotification(reviewId, "updated");
  }

  @EventListener
  public void onReviewComment(ReviewCommentEvent event) {
    PermId<ReviewData> reviewId = event.getReviewId();
    sendNotification(reviewId, "commeted");
  }

  @EventListener
  public void onReviewStateChange(
      ReviewStateChangedEvent event) {
    PermId<ReviewData> reviewId = event.getReviewId();

    sendNotification(reviewId, "status moved from "
        + event.getOldState().name() + " to "
        + event.getNewState().name());
  }

  @Override
  public void destroy() throws Exception {
    log.debug("Unregister review event listener");
    eventPublisher.unregister(this);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    log.debug("Register review event listener");
    eventPublisher.register(this);
  }

}

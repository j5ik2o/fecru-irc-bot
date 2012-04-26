package com.github.j5ik2o.fecruircbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.crucible.event.ReviewEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.fisheye.event.CommitEvent;
import com.atlassian.fisheye.spi.services.RevisionDataService;
import com.atlassian.sal.api.ApplicationProperties;

public class FishEyeEventListener implements DisposableBean, InitializingBean {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(FishEyeEventListener.class);

	private static final Logger log = LoggerFactory
			.getLogger(FishEyeEventListener.class);
	private final EventPublisher eventPublisher;
	private final ApplicationProperties applicationProperties;
	private final RevisionDataService revisionDataService;

	public FishEyeEventListener(EventPublisher eventPublisher,
			ApplicationProperties applicationProperties,
			RevisionDataService revisionDataService) {
		this.eventPublisher = eventPublisher;
		this.applicationProperties = applicationProperties;
		this.revisionDataService = revisionDataService;
	}

	@EventListener
	public void onCommit(CommitEvent event) {
		LOGGER.info("event = " + event.toString());
	}

	@Override
	public void destroy() throws Exception {
		log.debug("Unregister commit event listener");
		eventPublisher.unregister(this);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		log.debug("Register commit event listener");
		eventPublisher.register(this);
	}
}

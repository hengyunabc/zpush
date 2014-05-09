package com.zpush;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultRejectedListener implements RejectedListener{
	static final Logger logger = LoggerFactory.getLogger(DefaultRejectedListener.class);
	@Override
	public void handle(ErrorResponse response, Notification notification) {
		logger.warn("send notification error! response:" + response + ", notification:" + notification);
	}
}

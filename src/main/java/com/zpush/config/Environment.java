package com.zpush.config;

import com.alibaba.fastjson.JSON;

public class Environment {
	String pushHost;
	int pushPort;
	String feedbackHost;
	int feedbackPort;

	public Environment(String pushHost, int pushPort, String feedbackHost, int feedbackPort) {
		this.pushHost = pushHost;
		this.pushPort = pushPort;
		this.feedbackHost = feedbackHost;
		this.feedbackPort = feedbackPort;
	}

	public static final Environment Product = new Environment("gateway.push.apple.com", 2195,
			"feedback.push.apple.com", 2196);

	public static final Environment Development = new Environment("gateway.sandbox.push.apple.com", 2195,
			"feedback.sandbox.push.apple.com", 2196);

	public static final Environment LocalTest = new Environment("localhost", 2195,
			"localhost", 2196);
	
	public String getPushHost() {
		return pushHost;
	}

	public void setPushHost(String pushHost) {
		this.pushHost = pushHost;
	}

	public int getPushPort() {
		return pushPort;
	}

	public void setPushPort(int pushPort) {
		this.pushPort = pushPort;
	}

	public String getFeedbackHost() {
		return feedbackHost;
	}

	public void setFeedbackHost(String feedbackHost) {
		this.feedbackHost = feedbackHost;
	}

	public int getFeedbackPort() {
		return feedbackPort;
	}

	public void setFeedbackPort(int feedbackPort) {
		this.feedbackPort = feedbackPort;
	}

	public String toString() {
		return JSON.toJSONString(this);
	}
}

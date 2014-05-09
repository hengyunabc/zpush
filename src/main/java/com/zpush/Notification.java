package com.zpush;

import java.util.Date;

import com.alibaba.fastjson.JSON;

public class Notification {
	byte[] token; // 据官方的文档，当command是2时，这个必须是32 bytes的。是二进制格式的。
	byte[] payload;
	// TODO 用calendar? 这里一定是要UTC时间的
	Date expirationDate;

//	The notification’s priority. Provide one of the following values:
//		10 The push message is sent immediately.
//		The push notification must trigger an alert, sound, or badge on the device. It is an error to use this priority for a push that contains only the content-available key.
//		5 The push message is sent at a time that conserves power on the device receiving it.
	static final int defaultPriority = 10;
	
	int priority = defaultPriority;
	
	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public byte[] getToken() {
		return token;
	}

	public void setToken(byte[] token) {
		this.token = token;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public String toString() {
		return JSON.toJSONString(this);
	}
}

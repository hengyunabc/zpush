package com.zpush;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;

public class ErrorResponse {
	public static final int No_errors_encountered = 0;
	public static final int Processing_error = 1;
	public static final int Missing_device_token = 2;
	public static final int Missing_topic = 3;
	public static final int Missing_payload = 4;
	public static final int Invalid_token_size = 5;
	public static final int Invalid_topic_size = 6;
	public static final int Invalid_payload_size = 7;
	public static final int Invalid_token = 8;
	public static final int Shutdown = 10;
	public static final int None_unknown = 255;
	
	static final Map<Integer, String> errorMsgMap = new HashMap<Integer, String>();
	static {
		errorMsgMap.put(No_errors_encountered, "No errors encountered");
		errorMsgMap.put(Processing_error, "Processing error");
		errorMsgMap.put(Missing_device_token, "Missing device token");
		errorMsgMap.put(Missing_topic, "Missing topic");
		errorMsgMap.put(Missing_payload, "Missing payload");
		errorMsgMap.put(Invalid_token_size, "Invalid token size");
		errorMsgMap.put(Invalid_topic_size, "Invalid topic size");
		errorMsgMap.put(Invalid_payload_size, "Invalid payload size");
		errorMsgMap.put(Invalid_token, "Invalid token");
		errorMsgMap.put(Shutdown, "Shutdown");
		errorMsgMap.put(None_unknown, "None (unknown)");
	}
	
	// 据文档，目前是8，实际这个是版本号
	int commond;
	int status;
	int identifier;
	String errorMsg;

	public static String getErrorMsgByStatus(int status) {
		return errorMsgMap.get(status);
	}
	
	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public int getCommond() {
		return commond;
	}

	public void setCommond(int commond) {
		this.commond = commond;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getIdentifier() {
		return identifier;
	}

	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}

	public String toString() {
		return JSON.toJSONString(this);
	}
}

package com.zpush;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.alibaba.fastjson.JSONObject;

/**
 * 辅助生成Notification的工具类。
 * <pre>		
Notification notification = new NotificationBuilder()
		.setToken(token)
		.setBadge(1)
		.setAlert("just test").build();
 * </pre>
 *
 */
public class NotificationBuilder {
	byte[] token = null;
	int priority = Notification.defaultPriority;
	//new Date(3000, 1, 1).getTime() == 92464560000000L
	private Date expirationDate = new Date(92464560000000L);

	JSONObject payload = new JSONObject();
	JSONObject aps = new JSONObject();
	String alert = null;
	JSONObject alertObject = new JSONObject();
	
	final HashMap<String, Object> customProperties = new HashMap<String, Object>();
	static final Charset utf8 = Charset.forName("utf-8");

	static final int MAX_PAYLOAD_SIZE = 256;

	public NotificationBuilder() {
	}

	public Notification build() {
		Notification notification = new Notification();
		if (token != null) {
			notification.setToken(token);
		} else {
			throw new IllegalArgumentException("token is null!");
		}
		notification.setPriority(priority);
		//TODO 这里是否把没有设置过期时间的通知都设置成无限的？
		notification.setExpirationDate(expirationDate);
		
		/**
		 * <pre>
		 * 因为这里有两种格式，一种是：
		 *     "aps" : {
		 *         "alert" : "You got your emails.",
		 *         "badge" : 9,
		 *         "sound" : "bingbong.aiff"
		 *     },
		 * 
		 * 另一种是：
		 * "aps" : {
		 *    "alert" : {
		 *      "body" : "Bob wants to play poker",
		 *      "action-loc-key" : "PLAY"
		 *    },
		 *    "badge" : 5,
		 *  },
		 * </pre>
		 */
		if (alert != null) {
			aps.put("alert", alert);
		}else {
			aps.put("alert", alertObject);
		}
		
		payload.put("aps", aps);
		
		byte[] bytes = payload.toString().getBytes(utf8 );
		if (bytes.length > MAX_PAYLOAD_SIZE) {
			throw new IllegalArgumentException("payload.length >" + MAX_PAYLOAD_SIZE);
		}
		notification.setPayload(bytes);
		return notification;
	}

	public NotificationBuilder setToken(byte[] token) {
		if (token.length != 32) {
			throw new IllegalArgumentException("token.length != 32");
		}
		this.token = token;
		return this;
	}

	//TODO 这里应该可以自动去掉中间的空白字符
	public NotificationBuilder setToken(String token) {
		try {
			byte[] hex = Hex.decodeHex(token.toCharArray());
			setToken(hex);
		} catch (DecoderException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public NotificationBuilder setPriority(int priority) {
		this.priority = priority;
		return this;
	}

	public NotificationBuilder setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
		return this;
	}

	public NotificationBuilder setBadge(int badge) {
		aps.put("badge", badge);
		return this;
	}

	public NotificationBuilder setSound(String sound) {
		aps.put("sound", sound);
		return this;
	}

	public NotificationBuilder setcontentAvailable(boolean contentAvilable) {
		aps.put("content-available", 1);
		return this;
	}
	
	public NotificationBuilder setAlert(String alert) {
		this.alert = alert;
		return this;
	}

	public NotificationBuilder setAlertBody(String alertBody) {
		alertObject.put("body", alertBody);
		return this;
	}

	public NotificationBuilder setAlertActionLocKey(String alertActionLocKey) {
		alertObject.put("action-loc-key", alertActionLocKey);
		return this;
	}

	public NotificationBuilder setAlertLocKey(String alertLocKey) {
		alertObject.put("loc-key", alertLocKey);
		return this;
	}

	public NotificationBuilder setAlertLocArgs(String... alertLocArgs) {
		alertObject.put("loc-args", alertLocArgs);
		return this;
	}

	public NotificationBuilder setAlertLocArgs(List<String> alertLocArgs) {
		alertObject.put("loc-args", alertLocArgs);
		return this;
	}

	public NotificationBuilder setAlertLunchImage(String alertLunchImage) {
		alertObject.put("launch-image", alertLunchImage);
		return this;
	}

	public NotificationBuilder setUserProperty(String key, Object value) {
		payload.put(key, value);
		return this;
	}
}

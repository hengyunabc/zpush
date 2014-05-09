package com.zpush.feedback;

import java.util.Date;

import org.apache.commons.codec.binary.Hex;

public class Feedback {
	Date expiration;
	byte[] token;

	public Date getExpiration() {
		return expiration;
	}

	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}

	public byte[] getToken() {
		return token;
	}

	public void setToken(byte[] token) {
		this.token = token;
	}

	public Feedback(Date expiration, byte[] token) {
		super();
		this.expiration = expiration;
		this.token = token;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(512);
		return sb.append("{expiration:")
				.append(expiration.toLocaleString())
				.append(",token:")
				.append(Hex.encodeHexString(token)).toString();
		// return JSON.toJSONString(this);
	}

}

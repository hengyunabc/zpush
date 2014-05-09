package com.zpush;


public interface RejectedListener {

	/**
	 * 当发送的格式有问题时，会回调这个
	 * @param response
	 * @param byIdentifier  这个有可能是null，如果是null，则可以尝试加大PushClient的缓存大小
	 */
	public void handle(ErrorResponse response, Notification notification);
}

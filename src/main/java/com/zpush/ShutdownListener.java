package com.zpush;

import java.util.List;

/**
 * 当被调用PushClient被调用shutdown时，要回调的listener
 * 默认情况下应该把数据再放回到pushManager的queue中，然后再停止
 * 
 * 如果是pushManager的shutdownListener，则应该把数据保存到文件里？
 * 等下次启动再重新发送。
 * @author hengyunabc
 *
 */
public interface ShutdownListener {
	/**
	 * 
	 * @param notifications   没发送完的通知
	 */
	public void handle(List<Notification> notifications);
}

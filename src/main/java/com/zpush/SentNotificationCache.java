package com.zpush;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

/**
 * 简单的缓存已发送过的NOtification，每个PushClient都有一个自己的发送缓存。
 * 因为apple服务器只返回出错的消息的Identifier，所以要把已发送的缓存起来。 
 * TODO 是否要提高点查找效率
 * 
 * @author hengyunabc
 * 
 */
public class SentNotificationCache {
	static final int MAX_SIZE = 1024;
	Pair<Integer, Notification>[] pairArray;
	HashMap<Integer, Notification> notificationMap;
	
//	Notification[] array;

	int index = 0;
	
	public SentNotificationCache() {
		this(MAX_SIZE);
	}

	public SentNotificationCache(int cacheSize) {
//		array = new Notification[cacheSize];
		pairArray = new Pair[cacheSize];
		notificationMap = new HashMap<>(cacheSize);
	}

	public void add(Pair<Integer, Notification> pair) {
		//先把index位置下原来的从map里删除
		notificationMap.remove(index);
		
		pairArray[index] = pair;
		notificationMap.put(pair.getLeft(), pair.getRight());
		index++;
		if (index >= pairArray.length) {
			index = index % pairArray.length;
		}
	}
	/**
	 * 据identifier 从缓存里查找Notification
	 * @param identifier
	 * @return
	 */
	public Notification getByIdentifier(int identifier) {
		return notificationMap.get(identifier);
	}

	/**
	 * 这里只返回大于参数的Notification，对于等于identifier的Notification，程序自己要判断是出错了，
	 * 还是苹果的服务器shutdown了等原因
	 * 
	 * @param identifier
	 * @return
	 */
	public List<Notification> getGreaterNotifications(int identifier) {
		List<Notification> result = new ArrayList<>();
		for (int i = 0; i < pairArray.length; i++) {
			if (pairArray[i] != null && pairArray[i].getLeft() > identifier) {
				result.add(pairArray[i].getRight());
			}
		}
		return result;
	}

	public void clear() {
		pairArray = null;
		notificationMap.clear();
		notificationMap = null;
	}
}

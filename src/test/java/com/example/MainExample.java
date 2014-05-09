package com.example;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.zpush.Notification;
import com.zpush.NotificationBuilder;
import com.zpush.PushManager;
import com.zpush.PushManagerImpl;
import com.zpush.Statistic;
import com.zpush.config.Environment;

public class MainExample {
	public static void main(String[] args) throws InterruptedException {
		Environment environment = Environment.Product;
		String password = "123456";
		String keystore = "/home/hengyunabc/test/apptype/app_type_1/productAPNS.p12";
		PushManager pushManager = new PushManagerImpl(keystore, password, environment);
		
		//set a push queue
		BlockingQueue<Notification> queue = new LinkedBlockingQueue<Notification>(8192);
		pushManager.setQueue(queue );
		
		//waiting for SSL handshake success
		pushManager.start().sync();

		//build a notification
		String token = "5f6aa01d8e3358949b7c25d461bb78ad740f4707462c7eafbebcf74fa5ddb387";
		Notification notification = new NotificationBuilder()
				.setToken(token)
				.setBadge(1)
				.setPriority(5)
				.setAlertBody("xxxxx").build();

		//put notification into the queue
		queue.put(notification);
		
		TimeUnit.SECONDS.sleep(10);
		
		//get statistic info
		Statistic statistic = pushManager.getStatistic();
		System.out.println(statistic);
	}
}

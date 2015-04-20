package com.example;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.zpush.Notification;
import com.zpush.NotificationBuilder;
import com.zpush.PushManager;
import com.zpush.PushManagerImpl;
import com.zpush.ShutdownListener;
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
		pushManager.setShutdownListener(new ShutdownListener() {
			@Override
			public void handle(List<Notification> notifications) {
				System.out.println("PrintShutdownListener:");
				for(Notification notification : notifications) {
					System.out.println(notification);
				}
			}
		});
		
		//waiting for SSL handshake success
		pushManager.start().sync();

		//build a notification
		String token = "0dea779dd8850531e7120631efb27a269818a637b823087bf2b2c46347a8e518";
		Notification notification = new NotificationBuilder()
				.setToken(token)
				.setSound("default")
				.setBadge(1)
				.setUserProperty("hello", "world")
				.setAlert("test").build();

		System.err.println(notification.payloadJSONString());
		
		//put notification into the queue
		queue.put(notification);
		
		TimeUnit.SECONDS.sleep(10);
		
		//get statistic info
		Statistic statistic = pushManager.getStatistic();
		System.out.println(statistic);
		
		pushManager.shudownWithListener();
		System.out.println("pushManager.shudownWithListener()");
	}
}

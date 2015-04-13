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
		String keystore = "/tmp/productAPNS.p12";
		PushManager pushManager = new PushManagerImpl(keystore, password, environment);
		
		//set a push queue
		BlockingQueue<Notification> queue = new LinkedBlockingQueue<Notification>(8192);
		pushManager.setQueue(queue );
		
		//waiting for SSL handshake success
		pushManager.start().sync();

		//build a notification
		String token = "0dea779dd8850531e7120631efb27a269818a637b823087bf2b2c46347a8e518";
		Notification notification = new NotificationBuilder()
				.setToken(token)
				.setSound("default")
				.setBadge(1)
				.setAlert("test").build();

		//put notification into the queue
		queue.put(notification);
		
		TimeUnit.SECONDS.sleep(10);
		
		//get statistic info
		Statistic statistic = pushManager.getStatistic();
		System.out.println(statistic);
		pushManager.shutdownGracefully();
		System.out.println("pushManager.shutdownGracefully()");
	}
}

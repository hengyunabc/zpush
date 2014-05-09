package com.example;

import java.util.LinkedList;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.zpush.Notification;
import com.zpush.NotificationBuilder;
import com.zpush.PushManagerImpl;
import com.zpush.Statistic;

public class SpringExample {
	public static void main(String[] args) {
		ApplicationContext ctx = new FileSystemXmlApplicationContext("classpath:/spring-test.xml");

		PushManagerImpl pushManager = ctx.getBean(PushManagerImpl.class);
		String token = "5f6aa01d8e3358949b7c25d461bb78ad740f4707462c7eafbebcf74fa5ddb387";
		Notification notification = new NotificationBuilder()
				.setToken(token)
				.setBadge(1)
				.setPriority(5)
				.setAlertBody("xxxxx").build();

		pushManager.push(notification);
		
		List<Notification> list = new LinkedList<>();
		for(int i = 0; i < 100; ++i) {
			list.add(notification);
		}
		
		pushManager.push(list);
		
		Statistic statistic = pushManager.getStatistic();
		System.out.println(statistic);
	}
}

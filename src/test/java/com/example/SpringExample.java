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
		ApplicationContext ctx = new FileSystemXmlApplicationContext(
				"classpath:/spring-test.xml");

		PushManagerImpl pushManager = ctx.getBean(PushManagerImpl.class);
		String token = "0dea779dd8850531e7120631efb27a269818a637b823087bf2b2c46347a8e518";
		Notification notification = new NotificationBuilder()
				.setToken(token)
				.setSound("default")
				.setBadge(1)
				.setAlert("test").build();

		pushManager.push(notification);

		List<Notification> list = new LinkedList<>();
		for (int i = 0; i < 100; ++i) {
			list.add(notification);
		}

		pushManager.push(list);

		Statistic statistic = pushManager.getStatistic();
		System.out.println(statistic);
	}
}

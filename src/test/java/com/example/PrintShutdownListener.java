package com.example;

import java.util.List;

import com.zpush.Notification;
import com.zpush.ShutdownListener;

public class PrintShutdownListener implements ShutdownListener{
	@Override
	public void handle(List<Notification> notifications) {
		System.out.println("PrintShutdownListener:");
		for(Notification notification : notifications) {
			System.out.println(notification);
		}
	}
}

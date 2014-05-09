package com.example;

import com.zpush.ErrorResponse;
import com.zpush.Notification;
import com.zpush.RejectedListener;

public class PrintRejectListener implements RejectedListener{

	@Override
	public void handle(ErrorResponse response, Notification notification) {
		System.out.println("PrintRejectListener:");
		System.out.println("response:" + response + ", notification:" + notification);
	}

}

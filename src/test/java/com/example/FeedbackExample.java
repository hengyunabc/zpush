package com.example;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.zpush.config.Environment;
import com.zpush.feedback.Feedback;
import com.zpush.feedback.FeedbackClient;
import com.zpush.feedback.FeedbackListener;

public class FeedbackExample {
	public static void main(String[] args) throws InterruptedException {
		Environment environment = Environment.Product;
		String password = "123456";
		String keystore = "/home/hengyunabc/test/apptype/app_type_1/productAPNS.p12";
		FeedbackClient client= new FeedbackClient(keystore, password, environment);
		client.setFeedBackListener(new FeedbackListener() {
			@Override
			public void handle(List<Feedback> feedbackList) {
				for(Feedback feedback : feedbackList) {
					System.err.println("feedback:" + feedback);
				}
			}
		});
		client.start();
		
		TimeUnit.SECONDS.sleep(10);
		client.shutdownGracefully();
	}
}

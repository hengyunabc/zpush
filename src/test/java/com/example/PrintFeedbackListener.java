package com.example;

import java.util.List;

import com.zpush.feedback.Feedback;
import com.zpush.feedback.FeedbackListener;

public class PrintFeedbackListener implements FeedbackListener{

	@Override
	public void handle(List<Feedback> feedbackList) {
		System.out.println("PrintFeedbackListener:");
		for(Feedback feedback : feedbackList) {
			System.out.println(feedback);
		}
	}
}

package com.zpush.feedback;

import java.util.List;

public interface FeedbackListener {
	public void handle(List<Feedback> feedbackList);
}

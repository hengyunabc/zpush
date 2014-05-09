package com.zpush;

import io.netty.util.concurrent.Future;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import com.zpush.feedback.FeedbackListener;

public interface PushManager {
	public Future<?> start();
	
	/**
	 * 只有在设置了ShutdownListener的情况下才可以调用这个函数来停止，不然，有可能在队列里没有发送完的消息就会丢失了。
	 * 不应和shutdownGracefully重复调用
	 */
	public void shudownWithListener();
	
	/**
	 * 返回的Futrue里有未发送完的Notification，可以自行处理。
	 * 调用这个函数来停止，调不会ShutdownListener来处理没有发送完的Notification，而是在futrure里返回给用户自己处理。
	 * 不应和shudownWithListener重复调用
	 * @return
	 */
	public Future<List<Notification>> shutdownGracefully();
	public Future<List<Notification>> shutdownGracefully(final int timeout, final TimeUnit timeUnit);
	
	public void push(Notification notification);
	public void push(byte[] token, byte[] playload);
	public void push(List<Notification> notifications);
	
	public void setFeedBackListener(FeedbackListener feedbackListener);
	public FeedbackListener getFeedBackListener();
	
	public void setRejectedListener(RejectedListener listener);
	public RejectedListener getRejectedListener();
	
	public BlockingQueue<Notification> getQueue();
	public void setQueue(BlockingQueue<Notification> queue);
	
	public SSLContext getSSLContext();
	
	/**
	 * 得到统计信息
	 * @author hengyunabc
	 * @date 2014年4月4日
	 * @return
	 */
	public Statistic getStatistic();
}

package com.zpush;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.FailedFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.SucceededFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zpush.client.PushClient;
import com.zpush.config.Environment;
import com.zpush.feedback.FeedbackClient;
import com.zpush.feedback.FeedbackListener;
import com.zpush.util.SSLUtil;

public class PushManagerImpl implements PushManager {
	static final Logger logger = LoggerFactory.getLogger(PushManagerImpl.class);
	static Executor excutor = Executors.newCachedThreadPool();

	static final int defaultQueueSize = 1024;
	BlockingQueue<Notification> queue;

	/**
	 * 证书文件路径
	 */
	String keystore;
	String password;
	SSLContext sslContext;

	Environment environment = Environment.Product;

	// 设置多少个并发
	int pushClientCount = 1;

	PushClient[] pushClients;

	RejectedListener rejectedListener = new DefaultRejectedListener();

	FeedbackClient feedbackClient;
	// 如果feedbackListener是null，则不启动FeedbackClient
	FeedbackListener feedbackListener = null;

	ShutdownListener shutdownListener = null;

	public PushManagerImpl() {
	}

	public PushManagerImpl(String keystore, String password) {
		this(keystore, password, Environment.Product);
	}

	public PushManagerImpl(String keystore, String password, Environment environment) {
		this.keystore = keystore;
		this.password = password;
		this.environment = environment;
	}

	@Override
	public Future<?> start() {
		if (queue == null) {
			queue = new ArrayBlockingQueue<>(defaultQueueSize);
		}

		if (sslContext == null) {
			sslContext = SSLUtil.initSSLContext(keystore, password);
		}
		pushClients = new PushClient[pushClientCount];
		for (int i = 0; i < pushClients.length; ++i) {
			pushClients[i] = new PushClient(environment.getPushHost(), environment.getPushPort(), sslContext, queue);
			pushClients[i].setRejectedListener(rejectedListener);
		}

		final DefaultPromise<Object> promise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

		//client失败的计数
		final AtomicInteger failCount = new AtomicInteger(0);
		for (int i = 0; i < pushClients.length; ++i) {
			pushClients[i].start().addListener(new FutureListener<Object>() {
				@Override
				public void operationComplete(Future<Object> future) throws Exception {
					// 如果有一个pushclient成功了，则把promise设置为成功，promise只能设置一次，所以这里加锁了
					// 如果全部都失败了，则设置为promise为failure。
					synchronized (promise) {
						if (!promise.isDone()) {
							if (future.isSuccess()) {
								promise.setSuccess(new Object());
							}else {
								failCount.incrementAndGet();
								if (failCount.intValue() >= pushClients.length) {
									promise.setFailure(future.cause());
								}
							}
						}
					}
				}
			});
		}

		if (feedbackListener != null) {
			feedbackClient = new FeedbackClient(keystore, password, environment);
			feedbackClient.start();
		}

		return promise;
	}

	public Future<List<Notification>> shutdownGracefully(final int timeout, final TimeUnit timeUnit) {
		logger.info("try to shutdownGracefully");
		if (feedbackClient != null) {
			feedbackClient.shutdownGracefully(timeout, timeUnit);
		}

		final CountDownLatch latch = new CountDownLatch(pushClients.length);
		final List<Notification> result = new ArrayList<>();
		//尽量让所有的Client都shutdownGracefully
		for (int i = 0; i < pushClients.length; i++) {
			final PushClient client = pushClients[i];
			if (client == null) {
				latch.countDown();
			} else {
				excutor.execute(new Runnable() {
					@Override
					public void run() {
						Future<List<Notification>> future = client.shutdownGracefully(timeout, timeUnit);
						try {
							List<Notification> list = future.get(timeout, TimeUnit.SECONDS);
							result.addAll(list);
							latch.countDown();
						} catch (InterruptedException | ExecutionException | TimeoutException e) {
							logger.error("pushclient shutdown error!", e);
						}
					}
				});
			}
		}
		try {
			if(latch.await(timeout, TimeUnit.SECONDS)) {
				logger.info("PushManager shutDown success");
				return new SucceededFuture<>(GlobalEventExecutor.INSTANCE, result);
			}else {
				return new FailedFuture<>(GlobalEventExecutor.INSTANCE, new TimeoutException());
			}
		} catch (InterruptedException e) {
			logger.error("PushManager shutdonw error:" + e);
			return new FailedFuture<>(GlobalEventExecutor.INSTANCE, e);
		}
	}

	@Override
	public Future<List<Notification>> shutdownGracefully() {
		return shutdownGracefully(15, TimeUnit.SECONDS);
	}

	@Override
	public void push(byte[] token, byte[] playload) {
		Notification notification = new Notification();
		notification.setToken(token);
		notification.setPayload(playload);
		push(notification);
	}

	@Override
	public void push(List<Notification> notifications) {
		queue.addAll(notifications);
	}

	@Override
	public RejectedListener getRejectedListener() {
		return rejectedListener;
	}

	@Override
	public void setRejectedListener(RejectedListener listener) {
		this.rejectedListener = listener;
	}

	@Override
	public SSLContext getSSLContext() {
		return sslContext;
	}

	@Override
	public void push(Notification notification) {
		queue.add(notification);
	}

	@Override
	public BlockingQueue<Notification> getQueue() {
		return queue;
	}

	@Override
	public void setQueue(BlockingQueue<Notification> queue) {
		this.queue = queue;
	}

	@Override
	public void setFeedBackListener(FeedbackListener feedbackListener) {
		this.feedbackListener = feedbackListener;
	}

	@Override
	public FeedbackListener getFeedBackListener() {
		return feedbackListener;
	}

	@Override
	public void shudownWithListener() {
		if (shutdownListener != null) {
			Future<List<Notification>> future = this.shutdownGracefully();
			future.addListener(new FutureListener<List<Notification>>() {
				@Override
				public void operationComplete(Future<List<Notification>> future) throws Exception {
					if (future.isSuccess()) {
						shutdownListener.handle(future.get());
					}
				}
			});
		}
	}

	@Override
	public Statistic getStatistic() {
		Statistic statistic = new Statistic();
		if (pushClients != null) {
			//把PushClient里的统计信息加到总的统计里去。其中queueSize不会加上，因为所有的client共享一个queue。
			for(PushClient client : pushClients) {
				if (client != null) {
					Statistic clientStatistic = client.getStatistic();
					statistic.addAndGetWritedCount(clientStatistic.getWritedCount());
					statistic.addAndGetReSendCount(clientStatistic.getReSendCount());
					statistic.addAndGetRejectedCount(clientStatistic.getRejectedCount());
				}
			}
		}

		statistic.setQueueSize(queue.size());
		return statistic;
	}
	
	public String getKeystore() {
		return keystore;
	}

	public void setKeystore(String keystore) {
		this.keystore = keystore;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setShutdownListener(ShutdownListener shutdownListener) {
		this.shutdownListener = shutdownListener;
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
}

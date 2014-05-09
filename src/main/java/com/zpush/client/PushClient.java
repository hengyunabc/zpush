package com.zpush.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.SucceededFuture;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zpush.DefaultRejectedListener;
import com.zpush.ErrorResponse;
import com.zpush.Notification;
import com.zpush.RejectedListener;
import com.zpush.SentNotificationCache;
import com.zpush.Statistic;
import com.zpush.codec.ErrorResponseDecoder;
import com.zpush.codec.NotificationEncoder;
import com.zpush.util.DebugFlag;

/**
 * 
 * @author hengyunabc
 * 
 */
public class PushClient {
	static final Logger logger = LoggerFactory.getLogger(PushClient.class);

	// 断线重连的时间间隔，秒
	private static final long RECONNECT_DELAY = 5;
	// 如果1秒钟，没有write事件，则触发
	static int WRITE_IDLE = 5;

	volatile boolean bTryShutdown = false;
	String host;
	int port;

	// 如果ssl连接成功，则置这个key对应的Promise为成功
	private static final AttributeKey<DefaultChannelPromise> SSL_HANDSHAKE_PROMISE = AttributeKey.valueOf("sslHandshakePromise");

	BlockingQueue<Notification> notificationQueue;

	// 发送失败的消息会被放到这里，然后再次发送
	// TODO 也许这里应该用identifier进行排序，这样可以防止有些消息的接收顺序颠倒了，不过这种情况应该是比较少的。
	LinkedBlockingQueue<Notification> reSendNotifications = new LinkedBlockingQueue<Notification>();

	SentNotificationCache sentNotificationCache = new SentNotificationCache(1024);

	SSLContext sslContext;

	volatile Channel channel;
	EventLoopGroup eventLoopGroup;

	// TODO 处理溢出的问题
	int identifierSeq = 0;

	RejectedListener rejectedListener = new DefaultRejectedListener();
	
	Statistic statistic = new Statistic();

	public PushClient() {
	}

	public PushClient(String host, int port, SSLContext sslContext, BlockingQueue<Notification> queue) {
		this.host = host;
		this.port = port;
		this.sslContext = sslContext;
		this.notificationQueue = queue;
	}

	public Future<?> start() {
		logger.info("start PushClient");
		if (eventLoopGroup == null) {
			eventLoopGroup = new NioEventLoopGroup();
		}
		Bootstrap bootstrap = configureBootstrap(new Bootstrap());
		ChannelFuture future = bootstrap.connect();
		return addSslHandshakePromise(future);
	}
	
	Future<?> addSslHandshakePromise(ChannelFuture future) {
		// 增加一个Pomise，直到ssl握手结束后才算成功连接
		final DefaultChannelPromise sslHandShakePromise = new DefaultChannelPromise(future.channel());
		future.channel().attr(SSL_HANDSHAKE_PROMISE).set(sslHandShakePromise);
		future.addListener(new FutureListener<Object>() {
			@Override
			public void operationComplete(Future<Object> future) throws Exception {
				// 如果connect都不成功，那说明已经失败了，否则要等sslHandler的连接结果
				if (!future.isSuccess()) {
					sslHandShakePromise.setFailure(future.cause());
				}
			}
		});
		return sslHandShakePromise;
	}

	public Future<List<Notification>> shutdownGracefully() {
		return shutdownGracefully(15, TimeUnit.SECONDS);
	}

	public Future<List<Notification>> shutdownGracefully(int timeout, TimeUnit timeUnit) {
		bTryShutdown = true;
		
		final DefaultPromise<List<Notification>> promise = new DefaultPromise<List<Notification>>(GlobalEventExecutor.INSTANCE);

		final List<Notification> list = new ArrayList<>();
		if (eventLoopGroup != null) {

			Future<?> shutdownFutrue = eventLoopGroup.shutdownGracefully(1, timeout, timeUnit);
			shutdownFutrue.addListener(new FutureListener<Object>() {
				@Override
				public void operationComplete(Future<Object> future) throws Exception {
					if (future.isSuccess()) {
						logger.debug("pushclient shutdown success.");
					} else {
						logger.error("pushclient shutdown not success.", future.cause());
					}
					list.addAll(notificationQueue);
					list.addAll(reSendNotifications);
					promise.setSuccess(list);
				}
			});
			return promise;
		}
		return new SucceededFuture<List<Notification>>(GlobalEventExecutor.INSTANCE, Collections.<Notification> emptyList());
	}

	private Bootstrap configureBootstrap(Bootstrap b) {
		return configureBootstrap(b, eventLoopGroup);
	}

	Bootstrap configureBootstrap(Bootstrap b, EventLoopGroup g) {
		b.group(g)
				.channel(NioSocketChannel.class)
				.remoteAddress(host, port)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast(new IdleStateHandler(0, WRITE_IDLE, 0));
						SSLEngine sslEngine = sslContext.createSSLEngine();
						sslEngine.setUseClientMode(true);
						pipeline.addLast("ssl", new SslHandler(sslEngine));

						if (DebugFlag.debug) {
							pipeline.addLast("log", new LoggingHandler(LogLevel.DEBUG));
						}

						pipeline.addLast("notificationEncoder", new NotificationEncoder());
						pipeline.addLast("errorResponseDecoder", new ErrorResponseDecoder());
						pipeline.addLast(new PusherClientHandler());
					}
				});

		return b;
	}

	// TODO 这里的public去掉，只能在内部调用，而且只能同时一个线程调用
	public void sendNextNotification() {
		// 先尝试从重新发送的list里取notification，如果没有，则从正常的queue里取
		Notification notification = null;
		notification = reSendNotifications.poll();
		if (notification == null) {
			notification = notificationQueue.poll();
		}

		if (notification != null) {
			// 把已发送的放到cache里
			sentNotificationCache.add(Pair.of(identifierSeq, notification));
			identifierSeq++;
			channel.write(notification).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					// 只有上次成功了，才再次发送，如果上次Write失败了，则说明connection可能已经出错了。
					if (future.isSuccess()) {
						sendNextNotification();
					} else {
						// 是否认为这个是发送发败了？应该重发？
						// 或者是信任apple的服务器，如果出错了都是能有返回值的
					}
				}
			});
			//统计信息
			statistic.addAndGetWritedCount(1);
		} else {
			logger.debug("no notification need to send.");
		}
	}

	private void handleErrorResponse(ErrorResponse response) {
		// 把没有发送的通知重新发送，如果是苹果的服务器有问题，则停止发送，并把消息保存到一个地方去?
		logger.error("error Response:" + response);

		switch (response.getStatus()) {
		// 只有当Apple的服务器是停止的状态下，要把response里的identifier对应的通知重发
		// 其它情况，一律认为通知本身是有错的，抛弃掉
		case ErrorResponse.Shutdown:
			List<Notification> failNotifications = sentNotificationCache.getGreaterNotifications(response.getIdentifier() - 1);
			statistic.addAndGetReSendCount(failNotifications.size());  //统计信息
			reSendNotifications.addAll(failNotifications);
			break;
		default:
			statistic.addAndGetRejectedCount(1); //统计信息
			rejectedListener.handle(response, sentNotificationCache.getByIdentifier(response.getIdentifier()));
			break;
		}
	}

	class PusherClientHandler extends SimpleChannelInboundHandler<ErrorResponse> {
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			super.channelActive(ctx);
			channel = ctx.channel();
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, ErrorResponse msg) throws Exception {
			handleErrorResponse(msg);
		}

		@Override
		public void channelUnregistered(final ChannelHandlerContext ctx)
				throws Exception {
			logger.debug("channelUnregistered");
			// 断线重连
			if (bTryShutdown == false) {
				final EventLoop loop = ctx.channel().eventLoop();
				loop.schedule(new Runnable() {
					@Override
					public void run() {
						ChannelFuture future = configureBootstrap(new Bootstrap(), loop).connect();
						addSslHandshakePromise(future);
					}
				}, RECONNECT_DELAY, TimeUnit.SECONDS);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			logger.error("exceptionCaught:" + cause);
			if (bTryShutdown == false && cause instanceof ConnectException) {
				// 断线重连
				final EventLoop loop = ctx.channel().eventLoop();
				loop.schedule(new Runnable() {
					@Override
					public void run() {
						ChannelFuture future = configureBootstrap(new Bootstrap(), loop).connect();
						addSslHandshakePromise(future);
					}
				}, RECONNECT_DELAY, TimeUnit.SECONDS);
			}
			ctx.close();
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			logger.debug("userEventTriggered:" + evt);
			if (evt instanceof IdleStateEvent) {
				// 如果是长时间没有write事件，则尝试去从队列里拿出通知来发送
				IdleStateEvent e = (IdleStateEvent) evt;
				if (e.state() == IdleState.WRITER_IDLE) {
					if (bTryShutdown == false) {
						sendNextNotification();
					}
				}
			} else if (evt instanceof SslHandshakeCompletionEvent) {
				// 如果是SSL连接成功了，则先设置start()函数返回的future为success
				if(((SslHandshakeCompletionEvent) evt).isSuccess()) {
					DefaultChannelPromise sslHandshakePromise = ctx.channel().attr(SSL_HANDSHAKE_PROMISE).get();
					sslHandshakePromise.setSuccess();
					// 开始发送notification
					sendNextNotification();
				}else {
					DefaultChannelPromise sslHandshakePromise = ctx.channel().attr(SSL_HANDSHAKE_PROMISE).get();
					sslHandshakePromise.setFailure(((SslHandshakeCompletionEvent) evt).cause());
				}
			} else if (evt instanceof SSLHandshakeException) {
				// 如果是SSL连接不成功，则先设置start()函数返回的future为failure
				DefaultChannelPromise sslHandshakePromise = ctx.channel().attr(SSL_HANDSHAKE_PROMISE).get();
				sslHandshakePromise.setFailure(((SSLHandshakeException) evt).getCause());
			}
		}
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public EventLoopGroup getEventLoopGroup() {
		return eventLoopGroup;
	}

	public void setEventLoopGroup(EventLoopGroup eventLoopGroup) {
		this.eventLoopGroup = eventLoopGroup;
	}

	public RejectedListener getRejectedListener() {
		return rejectedListener;
	}

	public void setRejectedListener(RejectedListener rejectedListener) {
		this.rejectedListener = rejectedListener;
	}

	/**
	 * 获得统计信息
	 * @date 2014年4月4日
	 * @return
	 */
	public Statistic getStatistic() {
		statistic.setQueueSize(notificationQueue.size());
		return statistic;
	}
}

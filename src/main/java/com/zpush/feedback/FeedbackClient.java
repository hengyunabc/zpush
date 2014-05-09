package com.zpush.feedback;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zpush.codec.FeedBackDecoder;
import com.zpush.config.Environment;
import com.zpush.util.DebugFlag;
import com.zpush.util.SSLUtil;

/**
 * 据苹果的文档，feedback服务只有一个作用，就是得到失效的token，这样可以减少因为invalid token而push失败的情况，从而提高push效率。
 * 因为feedback服务并不是很重要，所以并不需要像PushClient那样处理很多关闭的情况。
 * @author hengyunabc
 *
 */
public class FeedbackClient {
	static final Logger logger = LoggerFactory.getLogger(FeedbackClient.class);

	// 断线重连的时间间隔，秒
	private static final long RECONNECT_DELAY = 5;

	/**
	 * 证书文件路径
	 */
	String keystore;
	String password;
	SSLContext sslContext;

	Environment environment = Environment.Product;

	EventLoopGroup eventLoopGroup;

	FeedbackListener feedbackListener;

	public FeedbackClient() {
	}

	public FeedbackClient(String keystore, String password) {
		this(keystore, password, Environment.Product);
	}

	public FeedbackClient(String keystore, String password, Environment environment) {
		this.keystore = keystore;
		this.password = password;
		this.environment = environment;
	}

	public Future<?>  start() {
		sslContext = SSLUtil.initSSLContext(keystore, password);
		if (eventLoopGroup == null) {
			eventLoopGroup = new NioEventLoopGroup();
		}
		return configureBootstrap(new Bootstrap()).connect();
	}

	/**
	 * shutdownGracefully(5, TimeUnit.SECONDS)
	 */
	public Future<?>  shutdownGracefully() {
		return shutdownGracefully(5, TimeUnit.SECONDS);
	}
	
	public Future<?> shutdownGracefully(final int timeout, final TimeUnit timeUnit){
		return eventLoopGroup.shutdownGracefully(1, timeout, timeUnit);
	}

	private Bootstrap configureBootstrap(Bootstrap b) {
		return configureBootstrap(b, eventLoopGroup);
	}

	Bootstrap configureBootstrap(Bootstrap b, EventLoopGroup g) {
		b.group(g)
				.channel(NioSocketChannel.class)
				.remoteAddress(environment.getFeedbackHost(), environment.getFeedbackPort())
				.option(ChannelOption.SO_KEEPALIVE, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						SSLEngine sslEngine = sslContext.createSSLEngine();
						sslEngine.setUseClientMode(true);
						pipeline.addLast("ssl", new SslHandler(sslEngine));
						if (DebugFlag.debug) {
							pipeline.addLast("log", new LoggingHandler(LogLevel.DEBUG));
						}
						pipeline.addLast("feedbackDecoder", new FeedBackDecoder());
						pipeline.addLast("handler", new FeedBackHandler());
					}
				});

		return b;
	}

	class FeedBackHandler extends SimpleChannelInboundHandler<List<Feedback>> {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, List<Feedback> feedbackList) throws Exception {
			feedbackListener.handle(feedbackList);
		}

		@Override
		public void channelUnregistered(final ChannelHandlerContext ctx)
				throws Exception {
			// 断线重连
			final EventLoop loop = ctx.channel().eventLoop();
			loop.schedule(new Runnable() {
				@Override
				public void run() {
					configureBootstrap(new Bootstrap(), loop).connect();
				}
			}, RECONNECT_DELAY, TimeUnit.SECONDS);
		}
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

	public SSLContext getSslContext() {
		return sslContext;
	}

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public FeedbackListener getFeedBackListener() {
		return feedbackListener;
	}

	public void setFeedBackListener(FeedbackListener feedbackListener) {
		this.feedbackListener = feedbackListener;
	}
}

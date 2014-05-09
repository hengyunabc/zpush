package com.zpush.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zpush.feedback.Feedback;
import com.zpush.util.DebugFlag;

/**
 * <pre>
 *  4 bytes        2 bytes       32 bytes
 * Timestamp + Token length + Device token
 * 
 * Timestamp: seconds since 12:00 midnight on January 1, 1970 UTC.
 * 
 * Device token其实就是32bytes，有个token length，应该是为了和以前的兼容
 * 目前的实现为了简单起见，直接读32bytes。
 * </pre>
 * @author hengyunabc
 *
 */
public class FeedBackDecoder extends ByteToMessageDecoder {
	static final Logger logger = LoggerFactory.getLogger(FeedBackDecoder.class);
	//一次性最多只解包出1024个FeedBack对象，防止过多的FeedBack导致内存上升
	//这个设置不一定起作用，不过也不会影响正常的逻辑
	static final int maxFeedBackListSize = 1024;
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (DebugFlag.debug) {
			logger.debug("readableBytes:" + in.readableBytes());
		}
		
		List<Feedback> feedbackList = new ArrayList<>(maxFeedBackListSize);
		while (in.readableBytes() >= 4 + 2 + 32) {
			long timestamp = (in.readInt() & 0xFFFFFFFFL) * 1000L;
			short len = in.readShort();
			assert(len == 2);
			byte[] token = new byte[32];
			in.readBytes(token);
			feedbackList.add(new Feedback(new Date(timestamp), token));
			if (feedbackList.size() >= maxFeedBackListSize) {
				break;
			}
		}
		if (feedbackList.size() > 0) {
			out.add(feedbackList);
		}
	}
}

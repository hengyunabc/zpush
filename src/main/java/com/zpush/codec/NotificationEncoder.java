package com.zpush.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.Charset;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zpush.Item;
import com.zpush.Notification;

/**
 * <pre>
 * 1 byte      4 byte 
 * command + frameLength + item + item + ... + item
 * 
 *         1 byte     2 byte 
 * item = itemId + itemLength + itemData
 * 
 * itemData据不同的类型有不同的长度：
 * 
 * </pre>
 * 
 * 采用组发送的方式，一次发送一系列的消息，然后每个消息的notificationIdentifier都是连续的，
 * 这样，如果apple的服务器返回错误，那么可以从错误里的Identifier知道是哪个出错了，然后重发下面的消息。
 * 
 * @author hengyunabc
 * 
 */
public class NotificationEncoder extends MessageToByteEncoder<Notification> {
	static final Logger logger = LoggerFactory.getLogger(NotificationEncoder.class);
	static final Charset utf8 = Charset.forName("UTF-8");

	// 据apple的文档，这个值就是2，据其它地方看到的资料，有0和1的版本。
	static final int COMMAND = 2;

	@Override
	protected void encode(ChannelHandlerContext ctx, Notification notification, ByteBuf out) throws Exception {
		logger.debug("NotificationEncoder:" + notification);

		out.writeByte(COMMAND);
		out.writeInt(0); // 预先写入frameLen
		
		int frameLenIndex = out.writerIndex();
		
		// 这里不检验数据格式的正确性，上层要检查

		// 开始写入items
		// 开始写入Device token
		out.writeByte(Item.DEVICE_TOKEN);
		out.writeShort(32);
		out.writeBytes(notification.getToken());

		// 写入Payload
		out.writeByte(Item.PAYLOAD);
		out.writeShort(notification.getPayload().length);
		out.writeBytes(notification.getPayload());
		
		// 写入Notification identifier
		out.writeByte(Item.NOTIFICATION_IDENTIFIER);
		out.writeShort(4);
		out.writeInt(notification.getIdentifier());

		// 写入Expiration date
		out.writeByte(Item.EXPIRATION_DATE);
		out.writeShort(4);
		Date expirationDate = notification.getExpirationDate();
		if (expirationDate == null) {
			out.writeInt(0);
		} else {
			out.writeInt((int) (expirationDate.getTime() / 1000));
		}

		// 写入Priority
		out.writeByte(Item.PRIORITY);
		out.writeShort(1);
		out.writeByte(notification.getPriority());

		// 回退到该写入frameLen的位置，写入frameLen，再重新设置write index
		int currentWriteIndex = out.writerIndex();
		out.writerIndex(1);  //因为command后面就是len，而command是1 byte。
		out.writeInt(currentWriteIndex - frameLenIndex);
		out.writerIndex(currentWriteIndex);
	}
}

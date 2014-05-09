package com.zpush.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.Charset;
import java.util.Date;

import org.apache.commons.codec.binary.Hex;
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

	// TODO 这个到底要不要做成static的，static的可能容易调试点，不然每个线程都有一个计数的话，可能会比较乱
	// TODO 处理超出int范围，再重新从0开始的逻辑
	int identifier = 0;

	static final Charset utf8 = Charset.forName("UTF-8");

	// 据apple的文档，这个值就是2，据其它地方看到的资料，有0和1的版本。
	static final int COMMAND = 2;

	static final int defaultExpirationDate = 0;

	@Override
	protected void encode(ChannelHandlerContext ctx, Notification notification, ByteBuf out) throws Exception {
		logger.debug("NotificationEncoder:" + notification);

//		 String data = "02000000770100205CA6A718250E1868A8EB7D8964D3E8A051B009268A3D710D0D8CC6591572890802003F7B22617073223A7B22736F756E64223A2264656661756C74222C22616C657274223A2252696E672072696E672C204E656F2E222C226261646765223A317D7D030004000000650400047FFFFFFF0500010A";
//		 out.writeBytes(Hex.decodeHex(data.toCharArray()));

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
		String string = "{\"aps\":{\"sound\":\"default\",\"alert\":\"Ring ring, Neo.\",\"badge\":1}}";
		byte[] bytes = string.getBytes();
		out.writeShort(notification.getPayload().length);
		out.writeBytes(notification.getPayload());
		
		// 写入Notification identifier
		out.writeByte(Item.NOTIFICATION_IDENTIFIER);
		out.writeShort(4);
		out.writeInt(identifier);

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

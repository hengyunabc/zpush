package com.zpush.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zpush.ErrorResponse;

public class ErrorResponseDecoder extends ByteToMessageDecoder{
	static final Logger logger = LoggerFactory.getLogger(ErrorResponseDecoder.class);
	
	//据文档： command + status + identifier = 1 + 1 + 4
	static final int responseSize = 6;
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		logger.debug("readableBytes:" + in.readableBytes());
		if (in.readableBytes() >= responseSize) {
			ErrorResponse errorResponse = new ErrorResponse();
			errorResponse.setCommond(in.readByte());
			errorResponse.setStatus(in.readByte());
			errorResponse.setIdentifier(in.readInt());
			errorResponse.setErrorMsg(ErrorResponse.getErrorMsgByStatus(errorResponse.getStatus()));
			out.add(errorResponse);
		}
		logger.debug("decoder:" + out);
	}
}

package com.zpush;

import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSON;

public class Statistic {
	// 写到netty缓冲区里的消息的次数，有可能会被总的消息数要大，因为有重发消息会多次计数
	AtomicLong writedCount = new AtomicLong(0);
	// 重发消息的计数
	AtomicLong reSendCount = new AtomicLong(0);
	// 被拒绝的消息的计数
	AtomicLong rejectedCount = new AtomicLong(0);

	// 队列里剩余的消息的数量，这个数据是调用时得到的Queue.size()
	long queueSize = 0;
	
	public void reset() {
		writedCount.set(0);
		reSendCount.set(0);
		rejectedCount.set(0);
		queueSize = 0;
	}

	public long addAndGetWritedCount(long delta) {
		return writedCount.addAndGet(delta);
	}

	public long addAndGetReSendCount(long delta) {
		return reSendCount.addAndGet(delta);
	}

	public long addAndGetRejectedCount(long delta) {
		return rejectedCount.addAndGet(delta);
	}

	public long getQueueSize() {
		return queueSize;
	}

	public void setQueueSize(long queueSize) {
		this.queueSize = queueSize;
	}

	public long getWritedCount() {
		return writedCount.get();
	}

	public long getReSendCount() {
		return reSendCount.get();
	}

	public long getRejectedCount() {
		return rejectedCount.get();
	}

	public String toString() {
		return JSON.toJSONString(this);
	}
}

package com.ngmj.caffeine;

import com.alibaba.dashscope.common.Message;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ChatMessageUtil {
	private static final Cache<String, List<Message>> cache;

	static {
		cache = Caffeine.newBuilder()
				.initialCapacity(100)
				.maximumSize(1000)
				.expireAfterWrite(60 * 60 * 1000, TimeUnit.MINUTES)
				.build();
	}

	public void put(String userId, List<Message> messages) {
		cache.put(userId, messages);
	}

	public List<Message> get(String userId) {
		return cache.getIfPresent(userId);
	}
}

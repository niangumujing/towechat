package com.ngmj.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.ngmj.caffeine.ChatMessageUtil;
import com.ngmj.config.AliyunConfig;
import com.ngmj.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "toWechat.app.platform", havingValue = "aliyun")
public class AliyunServiceImpl implements AiService {
	private final AliyunConfig aliyunConfig;
	private final Generation generation;
	private final AppConfig appConfig;
	private final ChatMessageUtil chatMessageUtil;
	private final Map<String, Queue<String>> userQueues = new ConcurrentHashMap<>();

	// 定义一个内部类保存用户状态
	private static class UserContext {
		StringBuilder finalContent = new StringBuilder();
		StringBuilder allContent = new StringBuilder();
		boolean isFirst = true;
	}

	private final Map<String, UserContext> userContexts = new ConcurrentHashMap<>();
	private final Map<String, Boolean> userProcessingStatus = new ConcurrentHashMap<>();

	@Override
	public String callWithMessage(String from, String textMessageContent) {
		Queue<String> queue = userQueues.get(from);
		if (queue != null && !queue.isEmpty()) {
			// 重新设置清理任务
			scheduleQueueCleanup(from, 300);
			// 取出一条发送
			return queue.poll();
		}
		if (userProcessingStatus.getOrDefault(from, false)) {
			return "请稍候，ai正在生成";
		}
		userProcessingStatus.put(from, true);
		Message userMessage = Message.builder().role(Role.USER.getValue())
				.content(textMessageContent)
				.build();
		List<Message> messages = chatMessageUtil.get(from);
		if (messages != null) {
			messages.add(userMessage);
		} else {
			messages = new ArrayList<>();
			messages.add(userMessage);
			chatMessageUtil.put(from, messages);
		}
		GenerationParam generationParam = GenerationParam.
				builder()
				.apiKey(aliyunConfig.getApiKey())
				.model(aliyunConfig.getModel())
				.messages(messages)
				.resultFormat(GenerationParam.ResultFormat.TEXT)
				.incrementalOutput(true)
				.build();
		CompletableFuture.runAsync(() -> {
			try {
				generation.streamCall(generationParam)
						.blockingForEach(message -> handleGenerationResult(from, message));
			} catch (NoApiKeyException | InputRequiredException e) {
				throw new RuntimeException(e);
			}
		});
		return "ai开始生成，结束后才能继续生成";
	}


	private void handleGenerationResult(String from, GenerationResult message) {
		UserContext ctx = userContexts.computeIfAbsent(from, k -> new UserContext());
		Queue<String> queue = userQueues.computeIfAbsent(from, k -> new LinkedList<>());
		String content = message.getOutput().getChoices().get(0).getMessage().getContent();

		if (!content.isEmpty()) {
			ctx.finalContent.append(content);
			ctx.allContent.append(content);
			// 如果内容达到限制，截断并加入队列
			if (ctx.finalContent.length() >= appConfig.getDefaultMaxChars()) {
				String segment = ctx.finalContent.substring(0, appConfig.getDefaultMaxChars());
				queue.offer(segment);
				ctx.finalContent.delete(0, appConfig.getDefaultMaxChars()); // 剩余内容保留
				log.info("截断内容：{}", segment);
			}
			ctx.isFirst = false;
			System.out.print(content);
			return;
		}
		if (!ctx.isFirst && content.isEmpty()) {
			StringBuilder append = ctx.finalContent.append("                   ai结束生成");
			queue.offer(append.toString());
			// 清空内容
			ctx.finalContent.setLength(0);
			// 移除用户上下文
			userContexts.remove(from);
			List<Message> messages = chatMessageUtil.get(from);
			messages.add(Message.builder().role(Role.ASSISTANT.getValue()).content(ctx.allContent.toString()).build());
			userProcessingStatus.put(from, false);
			chatMessageUtil.put(from, messages);
			// 设置初始清理任
			scheduleQueueCleanup(from, 300);
		}
	}

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	// 延迟清理队列
	public void scheduleQueueCleanup(String from, long delayInSeconds) {
		scheduler.schedule(() -> {
			userQueues.remove(from); // 清除用户的队列
			userContexts.remove(from); // 清除用户的上下文
			userProcessingStatus.put(from, false); // 重置处理状态
		}, delayInSeconds, TimeUnit.SECONDS);
	}
}



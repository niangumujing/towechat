package com.ngmj.controller;

import com.ngmj.common.enums.MessageType;
import com.ngmj.config.AppConfig;
import com.ngmj.entity.po.TextMessage;
import com.ngmj.service.AiService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("wx")
public class WeChatController {

	private final AppConfig appConfig;

	private final AiService aiService;

	// 验证微信服务器有效性
	@GetMapping()
	public String validate(
			@RequestParam("signature") String signature,
			@RequestParam("timestamp") String timestamp,
			@RequestParam("nonce") String nonce,
			@RequestParam("echostr") String echostr) {

		// 1. 将token、timestamp、nonce按字典序排序
		String[] arr = {appConfig.getToken(), timestamp, nonce};
		Arrays.sort(arr);

		// 2. SHA1加密
		String sha1 = DigestUtils.sha1Hex(String.join("", arr));
		return sha1.equals(signature) ? echostr : "";
	}

	/**
	 * 处理接收到的微信文本消息，根据消息内容调用对应AI服务生成回复
	 *
	 * @param textMessage 接收到的微信消息对象，包含消息内容、发送方等信息
	 * @return TextMessage 包装后的回复消息对象
	 * @throws Exception 处理过程中可能抛出的业务异常或系统异常
	 */
	@PostMapping()
	public TextMessage receptMeg(@RequestBody TextMessage textMessage) throws Exception {
		if (!textMessage.getMsgType().equals(MessageType.TEXT.getType())) {
			return getReturnMessage(textMessage, "暂不支持除文本外的消息类型");
		}

		if (aiService == null) {
			return getReturnMessage(textMessage, "暂不支持该平台");
		}
		return getReturnMessage(textMessage, aiService.callWithMessage(textMessage.getFromUserName(), textMessage.getContent()));
	}

	/**
	 * 创建并返回一个回复消息对象
	 * 该方法用于生成一个回复文本消息，根据接收到的消息和最终的回复内容
	 * 主要功能包括设置消息内容、接收方用户名、发送方用户名、创建时间及消息类型
	 *
	 * @param textMessage  接收到的文本消息对象，用于获取回复消息的接收方和发送方信息
	 * @param finalContent 回复消息的最终内容
	 * @return 返回一个填充好相关信息的文本消息对象
	 */
	private static TextMessage getReturnMessage(TextMessage textMessage, String finalContent) {
		// 创建一个新的文本消息对象作为回复消息
		TextMessage returnMessage = new TextMessage();

		// 设置回复消息的内容为最终的回复内容
		returnMessage.setContent(finalContent);

		// 设置回复消息的接收方用户名为原消息的发送方用户名
		returnMessage.setToUserName(textMessage.getFromUserName());

		// 设置回复消息的发送方用户名为原消息的接收方用户名
		returnMessage.setFromUserName(textMessage.getToUserName());

		// 设置回复消息的创建时间为当前系统时间戳
		returnMessage.setCreateTime(System.currentTimeMillis());

		// 设置回复消息的消息类型为文本类型
		returnMessage.setMsgType(MessageType.TEXT.getType());

		// 返回填充好信息的回复消息对象
		return returnMessage;
	}


}

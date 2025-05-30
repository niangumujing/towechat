package com.ngmj.config;


import com.alibaba.dashscope.aigc.generation.Generation;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Getter
@Configuration
public class AliyunConfig {
	@Value("${toWechat.aliyun.apiKey}")
	private String apiKey;
	@Value("${toWechat.aliyun.model}")
	private String model;

	@Bean
	public Generation generation() {
		return new Generation();
	}


}

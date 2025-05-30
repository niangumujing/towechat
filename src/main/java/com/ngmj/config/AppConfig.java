package com.ngmj.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class AppConfig {
	@Value("${toWechat.app.token}")
	private String token;
	@Value("${toWechat.app.defaultMaxChars}")
	private Integer defaultMaxChars;
	@Value("${toWechat.app.platform}")
	private String platform;

}

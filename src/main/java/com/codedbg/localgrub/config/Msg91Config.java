package com.codedbg.localgrub.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "msg91")
public class Msg91Config {
    @Value("authKey")
    private String authKey;
//    private String senderId;
    @Value("templateId")
    private String templateId;
//    private String flowId;
    @Value("widgetId")
    private String widgetId;
}

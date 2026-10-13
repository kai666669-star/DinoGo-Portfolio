package com.dinogo.sysmsg.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "sysmsg.email.enabled", havingValue = "true")
public class SysmsgEmailSchedulingConfig {
}

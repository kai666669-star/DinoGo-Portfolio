package com.dinogo.sysmsg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 啟用提交後非同步工作，使訂單原交易提交後，sysmsg 的 REQUIRED 方法在新執行緒
 * 開啟獨立交易；通知失敗不會回滾已提交的 Sales 訂單。
 */
@Configuration
@EnableAsync
public class SysmsgAsyncConfig {
}

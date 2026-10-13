package com.dinogo.sysmsg.dto.response;

//msg-首頁通知未讀// 首頁通知徽章只需要未讀總數，避免載入完整收件匣。
public record UnreadCountResponse(long unreadCount) {
}

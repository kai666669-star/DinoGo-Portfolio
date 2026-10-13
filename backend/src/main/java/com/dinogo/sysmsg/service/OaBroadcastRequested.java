package com.dinogo.sysmsg.service;

/** OA Send 提交後觸發的簡易非同步廣播事件。 */
public record OaBroadcastRequested(Integer sendId) {}

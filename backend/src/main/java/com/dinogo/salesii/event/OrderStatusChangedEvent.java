package com.dinogo.salesii.event;

//rev+msg-start，總共1次修改，第1次//
/**
 * Sales 模組交易內發布的中立訂單狀態事件。
 * 只攜帶 orderId，Review 與 Sysmsg listener 必須重新讀取權威訂單狀態。
 */
public record OrderStatusChangedEvent(Integer orderId) {
}
//rev+msg-end，總共1次修改，第1次//

package com.dinogo.sysmsg.service;

/** Immutable data required to submit one message to the Email provider. */
public record EmailDeliveryCommand(
        Integer recordChannelId,
        String recipientEmail,
        String subject,
        String content) {
}

package com.dinogo.sysmsg.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.dinogo.sysmsg.entity.NotificationType;

class EmailSubscriptionPolicyTest {

    @Test
    void orderTrueAllowsEmail() {
        assertTrue(EmailSubscriptionPolicy.allows(NotificationType.ORDER, true));
    }

    @Test
    void orderFalseRejectsEmail() {
        assertFalse(EmailSubscriptionPolicy.allows(NotificationType.ORDER, false));
    }

    @Test
    void orderNullDefaultsToAllow() {
        assertTrue(EmailSubscriptionPolicy.allows(NotificationType.ORDER, null));
    }

    @Test
    void marketingTrueAllowsEmail() {
        assertTrue(EmailSubscriptionPolicy.allows(NotificationType.MARKETING, true));
    }

    @Test
    void marketingFalseRejectsEmail() {
        assertFalse(EmailSubscriptionPolicy.allows(NotificationType.MARKETING, false));
    }

    @Test
    void marketingNullRejectsEmail() {
        assertFalse(EmailSubscriptionPolicy.allows(NotificationType.MARKETING, null));
    }
}

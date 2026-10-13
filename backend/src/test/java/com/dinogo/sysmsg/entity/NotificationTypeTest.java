package com.dinogo.sysmsg.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NotificationTypeTest {

    @Test
    void memberAcAndScAreOrderNotifications() {
        assertEquals(NotificationType.ORDER,
                NotificationType.resolve("AC-001", false));
        assertEquals(NotificationType.ORDER,
                NotificationType.resolve("SC-001", false));
    }

    @Test
    void sellerAsIsOrderNotification() {
        assertEquals(NotificationType.ORDER,
                NotificationType.resolve("AS-001", true));
    }

    @Test
    void systemPrefixesAreMarketingForTheirAllowedRecipients() {
        assertEquals(NotificationType.MARKETING,
                NotificationType.resolve("OA-001", false));
        assertEquals(NotificationType.MARKETING,
                NotificationType.resolve("OA-001", true));
        assertEquals(NotificationType.MARKETING,
                NotificationType.resolve("OC-001", false));
        assertEquals(NotificationType.MARKETING,
                NotificationType.resolve("OS-001", true));
    }

    @Test
    void rejectsOrderPrefixForWrongRecipientType() {
        assertThrows(IllegalArgumentException.class,
                () -> NotificationType.resolve("AC-001", true));
        assertThrows(IllegalArgumentException.class,
                () -> NotificationType.resolve("AS-001", false));
        assertThrows(IllegalArgumentException.class,
                () -> NotificationType.resolve("SC-001", true));
    }

    @Test
    void rejectsSystemPrefixForWrongRecipientType() {
        assertThrows(IllegalArgumentException.class,
                () -> NotificationType.resolve("OC-001", true));
        assertThrows(IllegalArgumentException.class,
                () -> NotificationType.resolve("OS-001", false));
    }

    @Test
    void rejectsUnknownOrMalformedPrefix() {
        assertThrows(IllegalArgumentException.class,
                () -> NotificationType.resolve("XX-001", false));
        assertThrows(IllegalArgumentException.class,
                () -> NotificationType.resolve(null, false));
        assertThrows(IllegalArgumentException.class,
                () -> NotificationType.resolve("A", false));
    }
}

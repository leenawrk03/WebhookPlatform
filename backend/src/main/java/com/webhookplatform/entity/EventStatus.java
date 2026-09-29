package com.webhookplatform.entity;

public enum EventStatus {
    RECEIVED,
    PROCESSING,
    DELIVERED,
    PARTIALLY_DELIVERED,
    FAILED,
    NO_DESTINATION
}

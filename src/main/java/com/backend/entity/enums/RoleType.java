package com.backend.entity.enums;

public enum RoleType {
    USER, STAFF, COORDINATOR, ADMIN,
    // Legacy values kept temporarily so existing databases can be migrated safely.
    MEMBER, LEADER, MENTOR, JUDGE
}

package com.smarturl.hub.analytics.ua;

public record UserAgentInfo(String deviceType, String os, String browser) {

    public static final String UNKNOWN = "unknown";

    public static UserAgentInfo unknown() {
        return new UserAgentInfo(UNKNOWN, UNKNOWN, UNKNOWN);
    }
}

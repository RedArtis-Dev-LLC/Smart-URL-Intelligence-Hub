package com.smarturl.hub.analytics.ua;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua_parser.Client;
import ua_parser.Parser;

@Slf4j
@Component
public class UserAgentParser {

    private final Parser parser = new Parser();

    public UserAgentInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UserAgentInfo.unknown();
        }
        try {
            Client client = parser.parse(userAgent);
            String browser = nullSafe(client.userAgent != null ? client.userAgent.family : null);
            String os = nullSafe(client.os != null ? client.os.family : null);
            String deviceType = classifyDevice(nullSafe(client.device != null ? client.device.family : null));
            return new UserAgentInfo(deviceType, os, browser);
        } catch (RuntimeException ex) {
            log.debug("Failed to parse user agent [{}]: {}", userAgent, ex.getMessage());
            return UserAgentInfo.unknown();
        }
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? UserAgentInfo.UNKNOWN : value;
    }

    private static String classifyDevice(String deviceFamily) {
        if (deviceFamily == null || UserAgentInfo.UNKNOWN.equals(deviceFamily)) {
            return UserAgentInfo.UNKNOWN;
        }
        String lower = deviceFamily.toLowerCase();
        if ("other".equals(lower) || "spider".equals(lower)) {
            return "desktop";
        }
        if (lower.contains("ipad") || lower.contains("tablet") || lower.contains("kindle")) {
            return "tablet";
        }
        return "mobile";
    }
}

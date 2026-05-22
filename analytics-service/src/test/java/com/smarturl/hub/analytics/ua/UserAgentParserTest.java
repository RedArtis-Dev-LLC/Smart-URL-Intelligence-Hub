package com.smarturl.hub.analytics.ua;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UserAgentParser Unit Tests")
class UserAgentParserTest {

    private final UserAgentParser parser = new UserAgentParser();

    @Nested
    @DisplayName("parse() tests")
    class ParseTests {

        @Test
        @DisplayName("Should return unknown for null user-agent")
        void shouldReturnUnknown_WhenUserAgentIsNull() {
            // When
            var result = parser.parse(null);

            // Then
            assertThat(result).isEqualTo(UserAgentInfo.unknown());
        }

        @Test
        @DisplayName("Should return unknown for blank user-agent")
        void shouldReturnUnknown_WhenUserAgentIsBlank() {
            // When
            var result = parser.parse("   ");

            // Then
            assertThat(result).isEqualTo(UserAgentInfo.unknown());
        }

        @Test
        @DisplayName("Should classify desktop Chrome on Windows")
        void shouldClassifyDesktopChromeOnWindows() {
            // Given
            var ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

            // When
            var result = parser.parse(ua);

            // Then
            assertThat(result.browser()).contains("Chrome");
            assertThat(result.os()).contains("Windows");
            assertThat(result.deviceType()).isEqualTo("desktop");
        }

        @Test
        @DisplayName("Should classify iPhone Safari as mobile")
        void shouldClassifyMobileSafariOnIphone() {
            // Given
            var ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";

            // When
            var result = parser.parse(ua);

            // Then
            assertThat(result.browser()).contains("Safari");
            assertThat(result.deviceType()).isEqualTo("mobile");
        }

        @Test
        @DisplayName("Should classify iPad as tablet")
        void shouldClassifyIpadAsTablet() {
            // Given
            var ua = "Mozilla/5.0 (iPad; CPU OS 17_2 like Mac OS X) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";

            // When
            var result = parser.parse(ua);

            // Then
            assertThat(result.deviceType()).isEqualTo("tablet");
        }
    }
}

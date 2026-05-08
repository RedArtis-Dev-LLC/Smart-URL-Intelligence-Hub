package com.smarturl.hub.common.autoconfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommonLibAutoConfigurationIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    void actuatorHealth_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_returns401WithoutGatewayHeaders() throws Exception {
        mockMvc.perform(get("/whoami"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_succeedsWhenGatewayHeadersPresent() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/whoami")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", "alice@example.com")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(userId.toString())));
    }

    @Test
    void validationFailure_returns400ProblemDetail() throws Exception {
        UUID userId = UUID.randomUUID();
        String invalidBody = "{\"email\":\"\",\"password\":\"\"}";

        mockMvc.perform(post("/echo")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.instance").value("/echo"));
    }

    @Test
    void accessDenied_returns403ProblemDetail() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/forbidden")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void unhandledException_returns500WithoutLeakingDetails() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/boom")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("internal blow-up"))));
    }
}

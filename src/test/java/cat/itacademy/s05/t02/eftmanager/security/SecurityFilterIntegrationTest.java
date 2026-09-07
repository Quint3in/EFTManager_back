package cat.itacademy.s05.t02.eftmanager.security;

import cat.itacademy.s05.t02.eftmanager.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityFilterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpoint_withMalformedToken_returnsForbiddenNotServerError() throws Exception {
        mockMvc.perform(get("/api/user/me")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withGarbageAuthorizationHeader_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/user/me")
                        .header("Authorization", "NotEvenBearerFormat"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withTamperedSignature_returnsForbidden() throws Exception {
        String tamperedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtldXNlciJ9.aW52YWxpZC1zaWduYXR1cmU";
        mockMvc.perform(get("/api/user/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isForbidden());
    }
}
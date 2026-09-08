package cat.itacademy.s05.t02.eftmanager.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Disabled;

//@Disabled("Falla de forma intermitente en la suite completa por presión de recursos de Docker/WSL2 — pasa correctamente en ejecución aislada")
class GlobalExceptionHandlerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper objectMapper;

    @Test
    void invalidRequestBody_missingRequiredField_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"email\":\"noesuncorreo\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidGameMode_returnsBadRequest() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new cat.itacademy.s05.t02.eftmanager.auth.RegisterRequest("excUser", "exc@test.com", "password123"));
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asString();

        mockMvc.perform(get("/api/hideout/modo-inventado")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
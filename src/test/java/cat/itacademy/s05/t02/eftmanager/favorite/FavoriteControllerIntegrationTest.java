package cat.itacademy.s05.t02.eftmanager.favorite;

import cat.itacademy.s05.t02.eftmanager.auth.RegisterRequest;
import cat.itacademy.s05.t02.eftmanager.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Disabled;

//@Disabled("Falla de forma intermitente en la suite completa por presión de recursos de Docker/WSL2 — funciona correctamente en ejecución aislada")
class FavoriteControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    private String registerAndGetToken(String username) throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterRequest(username, username + "@test.com", "password123"));
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asString();
    }

    @Test
    void addFavorite_thenListInSameMode_returnsIt() throws Exception {
        String token = registerAndGetToken("favUser1");

        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer " + token)
                        .param("mode", "pvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"5449016a4bdc2d6f028b456f\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token)
                        .param("mode", "pvp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("5449016a4bdc2d6f028b456f"));
    }

    @Test
    void favoriteAddedInPvp_doesNotAppearInPve() throws Exception {
        String token = registerAndGetToken("favUser2");

        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer " + token)
                        .param("mode", "pvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"5449016a4bdc2d6f028b456f\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token)
                        .param("mode", "pve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void removeFavorite_thenListing_returnsEmpty() throws Exception {
        String token = registerAndGetToken("favUser3");
        String itemId = "5449016a4bdc2d6f028b456f";

        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer " + token)
                        .param("mode", "pvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"" + itemId + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/favorites/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .param("mode", "pvp"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token)
                        .param("mode", "pvp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void addFavorite_withoutToken_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/favorites")
                        .param("mode", "pvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"5449016a4bdc2d6f028b456f\"}"))
                .andExpect(status().isForbidden());
    }
}
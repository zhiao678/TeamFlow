package com.teamflow.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("recruitment-test")
@Transactional
class AuthApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registersLogsInAndReturnsAccountMetadata() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username":"new-user","password":"password123","displayName":"New User"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.platformRole").value("USER"))
                .andExpect(jsonPath("$.data.user.accountStatus").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).path("data").path("token").asText();
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("new-user"))
                .andExpect(jsonPath("$.data.platformRole").value("USER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"new-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    void updatesProfileAndSkillTags() throws Exception {
        String token = registerAndGetToken("profile-user");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName":"Profile User",
                                  "department":"计算机学院",
                                  "grade":"2026级",
                                  "bio":"Java 和 Vue",
                                  "tagIds":[1,2,2]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Profile User"))
                .andExpect(jsonPath("$.data.department").value("计算机学院"))
                .andExpect(jsonPath("$.data.skills", hasSize(2)))
                .andExpect(jsonPath("$.data.skills", containsInAnyOrder("Java", "Vue")));

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skills", containsInAnyOrder("Java", "Vue")));
    }

    @Test
    void rejectsInvalidProfileTagWithoutChangingExistingTags() throws Exception {
        String token = registerAndGetToken("invalid-tag-user");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content("{\"tagIds\":[1,999]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skills", hasSize(0)));
    }

    @Test
    void returnsUnifiedUnauthorizedResponseAndRequiresLoginForLogout() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.message").value("unauthorized"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        String token = registerAndGetToken("logout-user");
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void returnsUnifiedForbiddenResponseForNonAdminUser() throws Exception {
        String token = registerAndGetToken("normal-user");

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.message").value("forbidden"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void rejectsDuplicateUsernameAfterNormalization() throws Exception {
        registerAndGetToken("normalized-user");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username":" normalized-user ","password":"password123","displayName":"Duplicate"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    void rejectsLoginForDisabledAccountWithUnifiedForbiddenResponse() throws Exception {
        registerAndGetToken("disabled-user");
        jdbcTemplate.update(
                "UPDATE `user` SET account_status = 'DISABLED' WHERE username = ?",
                "disabled-user"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"disabled-user","password":"password123"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.message").value("account is disabled"));
    }

    private String registerAndGetToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username
                                + "\",\"password\":\"password123\",\"displayName\":\"Test User\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

package com.teamflow.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
class RecruitmentApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registersLogsInAndReadsCurrentUser() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username":"new-user","password":"password123","displayName":"New User"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerResponse).path("data").path("token").asText();
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("new-user"))
                .andExpect(jsonPath("$.data.displayName").value("New User"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"new-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    void protectsCurrentUserAndRejectsDuplicateUsername() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username":"alice","password":"password123","displayName":"Another Alice"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    void listsAndCreatesRecruitmentsWithOwnerMembership() throws Exception {
        String token = registerAndGetToken("owner-one", "Alice");

        mockMvc.perform(get("/api/recruitments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(0)));

        String response = mockMvc.perform(post("/api/recruitments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"categoryId":1,"title":"Java 课程组队","purpose":"完成课程汇报",
                                 "description":"寻找队友","maxMembers":3,"tagIds":[1],"publish":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("RECRUITING"))
                .andExpect(jsonPath("$.data.currentMembers").value(1))
                .andExpect(jsonPath("$.data.remainingSlots").value(2))
                .andExpect(jsonPath("$.data.tags", hasSize(1)))
                .andReturn().getResponse().getContentAsString();

        long recruitmentId = objectMapper.readTree(response).path("data").path("id").asLong();
        mockMvc.perform(get("/api/recruitments/" + recruitmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerDisplayName").value("Alice"));
        mockMvc.perform(get("/api/recruitments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void supportsDraftPublishUpdateAndClose() throws Exception {
        String token = registerAndGetToken("owner-two", "Alice Two");
        String response = mockMvc.perform(post("/api/recruitments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"categoryId":1,"title":"Draft recruitment","purpose":"测试流程","maxMembers":2,"publish":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/recruitments/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"description\":\"已更新\",\"maxMembers\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("已更新"));
        mockMvc.perform(post("/api/recruitments/" + id + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECRUITING"));
        mockMvc.perform(post("/api/recruitments/" + id + "/close")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reason\":\"完成测试\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closeReason").value("完成测试"));
    }

    private String registerAndGetToken(String username, String displayName) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username
                                + "\",\"password\":\"password123\",\"displayName\":\""
                                + displayName + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }
}

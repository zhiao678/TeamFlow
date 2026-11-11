package com.teamflow.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import com.teamflow.backend.service.NotificationService;

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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                .andExpect(jsonPath("$.data.records", hasSize(0)));

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
                .andExpect(jsonPath("$.data.records", hasSize(1)));
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

    @Test
    void supportsDiscoveryFiltersAndFavorites() throws Exception {
        String token = registerAndGetToken("discover-user", "Discover User");
        String firstResponse = mockMvc.perform(post("/api/recruitments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"categoryId":1,"title":"Java 筛选招募","purpose":"测试发现","maxMembers":4,"tagIds":[1,2],"publish":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(firstResponse).path("data").path("id").asLong();
        mockMvc.perform(post("/api/recruitments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"categoryId":1,"title":"第二条筛选招募","purpose":"测试排序","maxMembers":5,"tagIds":[2],"publish":true}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/recruitments/" + id + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/recruitments")
                        .header("Authorization", "Bearer " + token)
                        .param("tagIds", "1,2")
                        .param("minRemainingSlots", "3")
                        .param("sort", "POPULAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(2)))
                .andExpect(jsonPath("$.data.records[0].id").value(id))
                .andExpect(jsonPath("$.data.records[0].favorited").value(true));

        mockMvc.perform(post("/api/recruitments/" + id + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/recruitments/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(true));
        mockMvc.perform(get("/api/recruitments/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(id));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/recruitments/" + id + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void doesNotAllowDraftFavorites() throws Exception {
        String token = registerAndGetToken("draft-favorite-user", "Draft Favorite User");
        String response = mockMvc.perform(post("/api/recruitments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"categoryId":1,"title":"Private draft","purpose":"Draft test","maxMembers":2,"publish":false}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(post("/api/recruitments/" + id + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void supportsNotificationReadWorkflow() throws Exception {
        String token = registerAndGetToken("notification-user", "Notification User");
        long userId = objectMapper.readTree(mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
        String applicantToken = registerAndGetToken("notification-applicant", "Applicant");
        long applicantId = objectMapper.readTree(mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + applicantToken))
                .andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
        String recruitmentResponse = mockMvc.perform(post("/api/recruitments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"categoryId":1,"title":"通知测试招募","purpose":"通知测试","maxMembers":2,"publish":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long recruitmentId = objectMapper.readTree(recruitmentResponse).path("data").path("id").asLong();
        jdbcTemplate.update("""
                INSERT INTO application (recruitment_id, applicant_id, introduction, status)
                VALUES (?, ?, 'Interested', 'PENDING')
                """, recruitmentId, applicantId);
        Long applicationId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM application", Long.class);
        notificationService.create(userId, null, "APPLICATION_SUBMITTED", "application:1:submitted",
                "APPLICATION", null, applicationId, "收到新的入队申请", "有学生申请加入你的招募");
        notificationService.create(userId, null, "APPLICATION_SUBMITTED", "application:1:submitted",
                "APPLICATION", null, applicationId, "重复事件", "重试不得重复创建通知");

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].read").value(false))
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        String notificationResponse = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        long notificationId = objectMapper.readTree(notificationResponse).path("data")
                .path("records").get(0).path("id").asLong();
        String otherToken = registerAndGetToken("other-notification-user", "Other User");
        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        notificationService.create(userId, null, "APPLICATION_WITHDRAWN", "application:2:withdrawn",
                "APPLICATION", null, applicationId, "申请已撤回", "申请人撤回了申请");
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token)
                        .param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(0)))
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
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

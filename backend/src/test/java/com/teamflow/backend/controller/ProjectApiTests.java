package com.teamflow.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamflow.backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("recruitment-test")
@Transactional
class ProjectApiTests {

    @DynamicPropertySource
    static void projectTestDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:teamflow_project_api_test;"
                + "MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private NotificationService notificationService;

    @Test
    void formsProjectWithTeamAndRevokesRemovedMembers() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Account owner = register("project-owner-" + suffix, "Project Owner");
        Account member = register("project-member-" + suffix, "Project Member");
        Account outsider = register("project-outsider-" + suffix, "Project Outsider");
        long recruitmentId = createRecruitment(owner, suffix);
        jdbcTemplate.update("""
                INSERT INTO team_member (recruitment_id, user_id, role, status)
                VALUES (?, ?, 'MEMBER', 'ACTIVE')
                """, recruitmentId, member.id());

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/recruitments/" + recruitmentId + "/form-team")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TEAMED"));

        mockMvc.perform(post("/api/recruitments/" + recruitmentId + "/form-team")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isConflict());

        Long projectId = jdbcTemplate.queryForObject(
                "SELECT id FROM project WHERE recruitment_id = ?", Long.class, recruitmentId);
        org.assertj.core.api.Assertions.assertThat(projectId).isNotNull();
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_member WHERE project_id = ? AND status = 'ACTIVE'",
                Integer.class, projectId)).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project WHERE recruitment_id = ?", Integer.class, recruitmentId))
                .isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT description FROM project WHERE id = ?", String.class, projectId))
                .isEqualTo("Complete a project\n\nTeam project details\n\nProject requirements");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE recruitment_id = ? AND event_type = 'RECRUITMENT_TEAMED'",
                Integer.class, recruitmentId)).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT project_role FROM project_member WHERE project_id = ? AND user_id = ?",
                String.class, projectId, owner.id())).isEqualTo("ADMIN");

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].recruitmentId").value(recruitmentId))
                .andExpect(jsonPath("$.data[0].recruitmentTitle").value("Project " + suffix))
                .andExpect(jsonPath("$.data[0].currentUserRole").value("ADMIN"));

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerId").value(owner.id()))
                .andExpect(jsonPath("$.data.memberCount").value(2));
        mockMvc.perform(get("/api/projects/" + projectId + "/members")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].teamRole").value("LEADER"));
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/recruitments/" + recruitmentId + "/members/" + member.id())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/projects/" + projectId + "/members")
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isForbidden());
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM project_member WHERE project_id = ? AND user_id = ?",
                String.class, projectId, member.id())).isEqualTo("REMOVED");
        jdbcTemplate.update("""
                UPDATE project_member SET status = 'ACTIVE', left_at = NULL
                WHERE project_id = ? AND user_id = ?
                """, projectId, member.id());
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void projectFailureRollsBackFormationAndPendingApplicationExpiry() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Account owner = register("rollback-owner-" + suffix, "Rollback Owner");
        Account applicant = register("rollback-applicant-" + suffix, "Rollback Applicant");
        long recruitmentId = createRecruitment(owner, suffix);
        jdbcTemplate.update("""
                INSERT INTO application (recruitment_id, applicant_id, introduction, status)
                VALUES (?, ?, 'Waiting', 'PENDING')
                """, recruitmentId, applicant.id());
        jdbcTemplate.update("""
                INSERT INTO project (recruitment_id, owner_id, name, status)
                VALUES (?, ?, ?, 'ACTIVE')
                """, recruitmentId, owner.id(), "Existing " + suffix);

        try {
            mockMvc.perform(post("/api/recruitments/" + recruitmentId + "/form-team")
                            .header("Authorization", bearer(owner.token())))
                    .andExpect(status().isConflict());

            org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM recruitment WHERE id = ?", String.class, recruitmentId))
                    .isEqualTo("RECRUITING");
            org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM application WHERE recruitment_id = ?", String.class, recruitmentId))
                    .isEqualTo("PENDING");
            org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM conversation WHERE recruitment_id = ?", Integer.class, recruitmentId))
                    .isZero();
            org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM project WHERE recruitment_id = ?", Integer.class, recruitmentId))
                    .isEqualTo(1);
        } finally {
            cleanup(recruitmentId, owner.id(), applicant.id());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void notificationFailureAfterProjectCreationRollsBackEntireFormation() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Account owner = register("late-failure-owner-" + suffix, "Late Failure Owner");
        Account applicant = register("late-failure-applicant-" + suffix, "Late Failure Applicant");
        long recruitmentId = createRecruitment(owner, suffix);
        jdbcTemplate.update("""
                INSERT INTO application (recruitment_id, applicant_id, introduction, status)
                VALUES (?, ?, 'Waiting', 'PENDING')
                """, recruitmentId, applicant.id());

        doThrow(new IllegalStateException("forced notification failure"))
                .when(notificationService)
                .create(anyLong(), nullable(Long.class), eq("RECRUITMENT_TEAMED"), anyString(),
                        eq("RECRUITMENT"), eq(recruitmentId), isNull(), anyString(), anyString());

        try {
            mockMvc.perform(post("/api/recruitments/" + recruitmentId + "/form-team")
                            .header("Authorization", bearer(owner.token())))
                    .andExpect(status().isInternalServerError());

            org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM recruitment WHERE id = ?", String.class, recruitmentId))
                    .isEqualTo("RECRUITING");
            org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM application WHERE recruitment_id = ?", String.class, recruitmentId))
                    .isEqualTo("PENDING");
            org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM project WHERE recruitment_id = ?", Integer.class, recruitmentId))
                    .isZero();
            org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM project_member pm JOIN project p ON p.id = pm.project_id "
                            + "WHERE p.recruitment_id = ?", Integer.class, recruitmentId))
                    .isZero();
            org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM conversation WHERE recruitment_id = ?", Integer.class, recruitmentId))
                    .isZero();
            org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM notification WHERE recruitment_id = ?", Integer.class, recruitmentId))
                    .isZero();
        } finally {
            cleanup(recruitmentId, owner.id(), applicant.id());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentFormationCreatesOnlyOneProjectAndConversation() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Account owner = register("concurrent-owner-" + suffix, "Concurrent Owner");
        long recruitmentId = createRecruitment(owner, suffix);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = executor.submit(() -> formTeamWhenReleased(start, recruitmentId, owner.token()));
            Future<Integer> second = executor.submit(() -> formTeamWhenReleased(start, recruitmentId, owner.token()));
            start.countDown();

            org.assertj.core.api.Assertions.assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(200, 409);
            org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM project WHERE recruitment_id = ?", Integer.class, recruitmentId))
                    .isEqualTo(1);
            org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM conversation WHERE recruitment_id = ?", Integer.class, recruitmentId))
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
            cleanup(recruitmentId, owner.id(), owner.id());
        }
    }

    @Test
    void approvedMemberExitRevokesProjectAndTeamConversationAccess() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Account owner = register("exit-owner-" + suffix, "Exit Owner");
        Account member = register("exit-member-" + suffix, "Exit Member");
        long recruitmentId = createRecruitment(owner, suffix);
        jdbcTemplate.update("""
                INSERT INTO team_member (recruitment_id, user_id, role, status)
                VALUES (?, ?, 'MEMBER', 'ACTIVE')
                """, recruitmentId, member.id());
        mockMvc.perform(post("/api/recruitments/" + recruitmentId + "/form-team")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk());

        Long projectId = jdbcTemplate.queryForObject(
                "SELECT id FROM project WHERE recruitment_id = ?", Long.class, recruitmentId);
        Long conversationId = jdbcTemplate.queryForObject(
                "SELECT id FROM conversation WHERE recruitment_id = ?", Long.class, recruitmentId);
        String exitResponse = mockMvc.perform(post("/api/recruitments/" + recruitmentId + "/exit-requests")
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long exitRequestId = read(exitResponse).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/member-exit-requests/" + exitRequestId + "/approve")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM project_member WHERE project_id = ? AND user_id = ?",
                String.class, projectId, member.id())).isEqualTo("LEFT");
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(member.token()))
                        .contentType("application/json")
                        .content("{\"clientMessageId\":\"after-exit\",\"content\":\"should fail\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void leaderTransferUpdatesProjectOwnerAndAccess() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Account owner = register("transfer-owner-" + suffix, "Transfer Owner");
        Account successor = register("transfer-successor-" + suffix, "Transfer Successor");
        long recruitmentId = createRecruitment(owner, suffix);
        jdbcTemplate.update("""
                INSERT INTO team_member (recruitment_id, user_id, role, status)
                VALUES (?, ?, 'MEMBER', 'ACTIVE')
                """, recruitmentId, successor.id());

        mockMvc.perform(post("/api/recruitments/" + recruitmentId + "/form-team")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk());
        Long projectId = jdbcTemplate.queryForObject(
                "SELECT id FROM project WHERE recruitment_id = ?", Long.class, recruitmentId);

        String request = mockMvc.perform(post("/api/recruitments/" + recruitmentId + "/leader-exit-requests")
                        .header("Authorization", bearer(owner.token()))
                        .contentType("application/json")
                        .content("{\"successorId\":" + successor.id() + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long transferId = read(request).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/leader-transfers/" + transferId + "/accept")
                        .header("Authorization", bearer(successor.token())))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT owner_id FROM project WHERE id = ?", Long.class, projectId))
                .isEqualTo(successor.id());
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM project_member WHERE project_id = ? AND user_id = ?",
                String.class, projectId, owner.id())).isEqualTo("LEFT");
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", bearer(successor.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerId").value(successor.id()))
                .andExpect(jsonPath("$.data.currentUserRole").value("ADMIN"));
    }

    private void cleanup(long recruitmentId, long ownerId, long applicantId) {
        jdbcTemplate.update("DELETE FROM message WHERE conversation_id IN "
                + "(SELECT id FROM conversation WHERE recruitment_id = ?)", recruitmentId);
        jdbcTemplate.update("DELETE FROM conversation_member WHERE conversation_id IN "
                + "(SELECT id FROM conversation WHERE recruitment_id = ?)", recruitmentId);
        jdbcTemplate.update("DELETE FROM conversation WHERE recruitment_id = ?", recruitmentId);
        jdbcTemplate.update("DELETE FROM project_member WHERE project_id IN "
                + "(SELECT id FROM project WHERE recruitment_id = ?)", recruitmentId);
        jdbcTemplate.update("DELETE FROM project WHERE recruitment_id = ?", recruitmentId);
        jdbcTemplate.update("DELETE FROM application WHERE recruitment_id = ?", recruitmentId);
        jdbcTemplate.update("DELETE FROM team_member WHERE recruitment_id = ?", recruitmentId);
        jdbcTemplate.update("DELETE FROM notification WHERE recipient_id IN (?, ?)", ownerId, applicantId);
        jdbcTemplate.update("DELETE FROM user_profile WHERE user_id IN (?, ?)", ownerId, applicantId);
        jdbcTemplate.update("DELETE FROM recruitment WHERE id = ?", recruitmentId);
        jdbcTemplate.update("DELETE FROM user WHERE id IN (?, ?)", ownerId, applicantId);
    }

    private long createRecruitment(Account owner, String suffix) throws Exception {
        String response = mockMvc.perform(post("/api/recruitments")
                        .header("Authorization", bearer(owner.token()))
                        .contentType("application/json")
                        .content("{\"categoryId\":1,\"title\":\"Project " + suffix
                                + "\",\"purpose\":\"Complete a project\",\"description\":\"Team project details\","
                                + "\"requirements\":\"Project requirements\","
                                + "\"maxMembers\":4,\"publish\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return read(response).path("data").path("id").asLong();
    }

    private int formTeamWhenReleased(CountDownLatch start, long recruitmentId, String token) throws Exception {
        start.await();
        return mockMvc.perform(post("/api/recruitments/" + recruitmentId + "/form-team")
                        .header("Authorization", bearer(token)))
                .andReturn().getResponse().getStatus();
    }

    private Account register(String username, String displayName) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username
                                + "\",\"password\":\"password123\",\"displayName\":\""
                                + displayName + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = read(response).path("data");
        return new Account(data.path("user").path("id").asLong(), data.path("token").asText());
    }

    private JsonNode read(String response) throws Exception {
        return objectMapper.readTree(response);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Account(Long id, String token) {
    }
}

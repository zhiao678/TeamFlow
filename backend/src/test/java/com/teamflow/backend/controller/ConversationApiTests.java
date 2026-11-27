package com.teamflow.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
class ConversationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsOnePrivateConversationAndSupportsIdempotentMessages() throws Exception {
        Account owner = register("conversation-owner", "Conversation Owner");
        Account member = register("conversation-member", "Conversation Member");

        String firstConversation = mockMvc.perform(post("/api/conversations/private")
                        .header("Authorization", bearer(owner.token()))
                        .contentType("application/json")
                        .content("{\"targetUserId\":" + member.id() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("DIRECT"))
                .andExpect(jsonPath("$.data.otherUserId").value(member.id().intValue()))
                .andExpect(jsonPath("$.data.otherUserDisplayName").value("Conversation Member"))
                .andReturn().getResponse().getContentAsString();
        long conversationId = read(firstConversation).path("data").path("id").asLong();

        mockMvc.perform(post("/api/conversations/private")
                        .header("Authorization", bearer(owner.token()))
                        .contentType("application/json")
                        .content("{\"userId\":" + member.id() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(conversationId));

        String firstMessage = mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(owner.token()))
                        .contentType("application/json")
                        .content("{\"clientMessageId\":\"client-1\",\"content\":\"hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("hello"))
                .andExpect(jsonPath("$.data.senderId").value(owner.id().intValue()))
                .andReturn().getResponse().getContentAsString();
        long firstMessageId = read(firstMessage).path("data").path("id").asLong();

        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(owner.token()))
                        .contentType("application/json")
                        .content("{\"clientId\":\"client-1\",\"content\":\"hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(firstMessageId));

        String secondMessage = mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(owner.token()))
                        .contentType("application/json")
                        .content("{\"clientMessageId\":\"client-2\",\"content\":\"second\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long secondMessageId = read(secondMessage).path("data").path("id").asLong();

        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(member.token()))
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].id").value(firstMessageId))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.unreadCount").value(2));

        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(member.token()))
                        .param("afterMessageId", String.valueOf(firstMessageId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].id").value(secondMessageId))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.hasMore").value(false));

        mockMvc.perform(patch("/api/conversations/" + conversationId + "/read")
                        .header("Authorization", bearer(member.token()))
                        .contentType("application/json")
                        .content("{\"lastReadMessageId\":" + firstMessageId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastReadMessageId").value(firstMessageId))
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        mockMvc.perform(patch("/api/conversations/" + conversationId + "/read")
                        .header("Authorization", bearer(member.token()))
                        .contentType("application/json")
                        .content("{\"messageId\":" + secondMessageId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        Account outsider = register("conversation-outsider", "Outsider");
        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(get("/api/conversations/99999/messages")
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void blocksPrivateConversationAndPreventsRemovedMemberFromSending() throws Exception {
        Account owner = register("blocked-owner", "Blocked Owner");
        Account member = register("blocked-member", "Blocked Member");
        String response = mockMvc.perform(post("/api/conversations/private")
                        .header("Authorization", bearer(owner.token()))
                        .contentType("application/json")
                        .content("{\"targetUserId\":" + member.id() + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long conversationId = read(response).path("data").path("id").asLong();

        jdbcTemplate.update("INSERT INTO user_block (blocker_id, blocked_id) VALUES (?, ?)", owner.id(), member.id());
        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(member.token()))
                        .contentType("application/json")
                        .content("{\"clientMessageId\":\"blocked-1\",\"content\":\"blocked\"}"))
                .andExpect(status().isForbidden());

        jdbcTemplate.update("""
                UPDATE conversation_member SET status = 'REMOVED', left_at = CURRENT_TIMESTAMP
                WHERE conversation_id = ? AND user_id = ?
                """, conversationId, owner.id());
        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(owner.token()))
                        .contentType("application/json")
                        .content("{\"clientMessageId\":\"removed-1\",\"content\":\"removed\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void sendsMessagesInTeamConversationForActiveMembers() throws Exception {
        Account owner = register("team-owner", "Team Owner");
        Account member = register("team-member", "Team Member");
        String recruitmentResponse = mockMvc.perform(post("/api/recruitments")
                        .header("Authorization", bearer(owner.token()))
                        .contentType("application/json")
                        .content("{\"categoryId\":1,\"title\":\"Team chat\",\"purpose\":\"chat\",\"maxMembers\":3,\"publish\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long recruitmentId = read(recruitmentResponse).path("data").path("id").asLong();
        jdbcTemplate.update("INSERT INTO team_member (recruitment_id, user_id, role, status) VALUES (?, ?, 'MEMBER', 'ACTIVE')",
                recruitmentId, member.id());
        jdbcTemplate.update("INSERT INTO conversation (type, recruitment_id, status) VALUES ('TEAM', ?, 'ACTIVE')", recruitmentId);
        long conversationId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM conversation", Long.class);
        jdbcTemplate.update("INSERT INTO conversation_member (conversation_id, user_id, status) VALUES (?, ?, 'ACTIVE')",
                conversationId, owner.id());
        jdbcTemplate.update("INSERT INTO conversation_member (conversation_id, user_id, status) VALUES (?, ?, 'ACTIVE')",
                conversationId, member.id());

        mockMvc.perform(get("/api/conversations").header("Authorization", bearer(member.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].type").value("TEAM"))
                .andExpect(jsonPath("$.data[0].recruitmentId").value(recruitmentId));
        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(member.token()))
                        .contentType("application/json")
                        .content("{\"clientMessageId\":\"team-1\",\"content\":\"team hello\",\"sourceRecruitmentId\":" + recruitmentId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("team hello"))
                .andExpect(jsonPath("$.data.sourceRecruitmentId").value(recruitmentId));
        String teamMessageResponse = mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].content").value("team hello"))
                .andExpect(jsonPath("$.data.unreadCount").value(1))
                .andReturn().getResponse().getContentAsString();
        long teamMessageId = read(teamMessageResponse).path("data").path("records").get(0).path("id").asLong();
        mockMvc.perform(patch("/api/conversations/" + conversationId + "/read")
                        .header("Authorization", bearer(member.token()))
                        .contentType("application/json")
                        .content("{\"lastReadMessageId\":" + teamMessageId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        jdbcTemplate.update("""
                UPDATE team_member SET status = 'REMOVED', left_at = CURRENT_TIMESTAMP
                WHERE recruitment_id = ? AND user_id = ?
                """, recruitmentId, member.id());
        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(member.token()))
                        .contentType("application/json")
                        .content("{\"clientMessageId\":\"team-2\",\"content\":\"should fail\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/conversations/" + conversationId + "/read")
                        .header("Authorization", bearer(member.token()))
                        .contentType("application/json")
                        .content("{\"lastReadMessageId\":" + teamMessageId + "}"))
                .andExpect(status().isForbidden());
    }

    private Account register(String username, String displayName) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"password123\",\"displayName\":\"" + displayName + "\"}"))
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

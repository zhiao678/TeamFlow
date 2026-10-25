package com.teamflow.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the actual HTTP, validation, service and SQL path against the demo database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class TaskApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listsOnlyTasksFromTheRequestedProject() throws Exception {
        mockMvc.perform(get("/api/projects/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[*].projectId", everyItem(is(1))));

        mockMvc.perform(get("/api/projects/2/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(5));
    }

    @Test
    void returnsNotFoundForAMissingProjectOrTask() throws Exception {
        mockMvc.perform(get("/api/projects/999999/tasks"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/projects/999999/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Demo\",\"creatorId\":1}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/tasks/999999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createsAnUnassignedTaskWithDefaultStatusAndPriority() throws Exception {
        String response = mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  Demo task  \",\"creatorId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.title").value("Demo task"))
                .andExpect(jsonPath("$.data.projectId").value(1))
                .andExpect(jsonPath("$.data.creatorId").value(1))
                .andExpect(jsonPath("$.data.status").value("TODO"))
                .andExpect(jsonPath("$.data.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.data.assigneeId", nullValue()))
                .andExpect(jsonPath("$.data.completedAt", nullValue()))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).path("data").path("id").asLong();
        assertEquals("TODO", jdbcTemplate.queryForObject(
                "SELECT status FROM task WHERE id = ?", String.class, taskId));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT deleted FROM task WHERE id = ?", Integer.class, taskId));
    }

    @Test
    void createsAnAssignedTaskWithPriorityAndDeadline() throws Exception {
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Assigned task","creatorId":1,"assigneeId":2,
                                 "description":"Check the complete SQL path","priority":"HIGH",
                                 "dueAt":"2026-12-01T18:00:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.assigneeId").value(2))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.dueAt").value("2026-12-01T18:00:00"));
    }

    @Test
    void rejectsACreatorWhoIsNotAProjectMember() throws Exception {
        mockMvc.perform(post("/api/projects/2/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Outside project\",\"creatorId\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnAssigneeWhoIsNotAProjectMember() throws Exception {
        mockMvc.perform(post("/api/projects/2/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Outside assignee\",\"creatorId\":1,\"assigneeId\":3}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"title\":\"  \",\"creatorId\":1}",
            "{\"creatorId\":1}",
            "{\"title\":\"Demo\"}",
            "{\"title\":\"Demo\",\"creatorId\":0}",
            "{\"title\":\"Demo\",\"creatorId\":1,\"assigneeId\":0}",
            "{\"title\":\"Demo\",\"creatorId\":1,\"priority\":\"URGENT\"}"
    })
    void rejectsInvalidCreateRequests(String request) throws Exception {
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"status\":\"INVALID\"}", "{\"status\":\"\"}", "{}"})
    void rejectsInvalidStatusRequests(String request) throws Exception {
        mockMvc.perform(patch("/api/tasks/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordsCompletionAndClearsItWhenTheTaskIsReopened() throws Exception {
        mockMvc.perform(patch("/api/tasks/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.completedAt", notNullValue()));
        assertNotNull(completedAt(1));

        mockMvc.perform(patch("/api/tasks/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.completedAt", nullValue()));
        // Read SQL directly so a stale MyBatis entity cannot conceal a missing null update.
        assertNull(completedAt(1));
        assertEquals("IN_PROGRESS", jdbcTemplate.queryForObject(
                "SELECT status FROM task WHERE id = 1", String.class));
    }

    @Test
    void repeatedDonePreservesTheOriginalCompletionTime() throws Exception {
        Timestamp original = completedAt(3);
        assertNotNull(original);

        mockMvc.perform(patch("/api/tasks/3/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());

        assertEquals(original, completedAt(3));
    }

    @Test
    void excludesLogicallyDeletedTasksAndRejectsTheirUpdates() throws Exception {
        jdbcTemplate.update("UPDATE task SET deleted = 1 WHERE id = 1");

        mockMvc.perform(get("/api/projects/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));
        mockMvc.perform(patch("/api/tasks/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());

        assertEquals("TODO", jdbcTemplate.queryForObject(
                "SELECT status FROM task WHERE id = 1", String.class));
    }

    @Test
    void rejectsInvalidPathIds() throws Exception {
        mockMvc.perform(get("/api/projects/0/tasks"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/projects/-1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Demo\",\"creatorId\":1}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/tasks/0/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesAllThreeTaskOperationsInOpenApi() throws Exception {
        String response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode paths = objectMapper.readTree(response).path("paths");

        assertNotNull(paths.get("/api/projects/{projectId}/tasks"));
        assertNotNull(paths.path("/api/projects/{projectId}/tasks").get("get"));
        assertNotNull(paths.path("/api/projects/{projectId}/tasks").get("post"));
        assertNotNull(paths.path("/api/tasks/{taskId}/status").get("patch"));
    }

    @Test
    void allowsFrontendPatchPreflight() throws Exception {
        mockMvc.perform(options("/api/tasks/1/status")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173"));
    }

    private Timestamp completedAt(long taskId) {
        return jdbcTemplate.queryForObject(
                "SELECT completed_at FROM task WHERE id = ?", Timestamp.class, taskId);
    }
}

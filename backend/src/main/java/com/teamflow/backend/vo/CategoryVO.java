package com.teamflow.backend.vo;

public record CategoryVO(Long id, String name, Long parentId, Integer sortOrder) {
}

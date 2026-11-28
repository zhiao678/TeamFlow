package com.teamflow.backend.vo;

import java.util.List;

public record TaskPageVO(List<TaskVO> records, int page, int pageSize, long total, TaskStatisticsVO statistics) {
}

package com.teamflow.backend.vo;

import java.util.List;

public record RecruitmentPageVO(List<RecruitmentVO> records, int page, int pageSize, long total) {
}

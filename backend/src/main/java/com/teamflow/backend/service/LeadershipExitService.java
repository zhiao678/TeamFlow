package com.teamflow.backend.service;

import com.teamflow.backend.vo.LeaderTransferVO;

import java.util.List;

public interface LeadershipExitService {
    LeaderTransferVO create(Long recruitmentId, Long successorId);

    List<LeaderTransferVO> listMine();

    LeaderTransferVO accept(Long id);

    LeaderTransferVO reject(Long id);
}

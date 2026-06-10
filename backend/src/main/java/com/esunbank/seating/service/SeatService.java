package com.esunbank.seating.service;

import com.esunbank.seating.common.dto.AssignmentRequest;
import com.esunbank.seating.common.dto.SeatDTO;

import java.util.List;

/**
 * 業務層：座位相關服務介面。
 */
public interface SeatService {

    /** 取得各樓層座位佈局 */
    List<SeatDTO> getSeatingLayout();

    /** 批次套用座位異動 (Transaction)，回傳更新後佈局 */
    List<SeatDTO> applyAssignments(AssignmentRequest request);
}

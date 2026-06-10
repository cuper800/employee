package com.esunbank.seating.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 共用層：批次座位異動請求 (送出按鈕)。
 */
public class AssignmentRequest {

    @NotNull(message = "assignments 不可為空")
    @Valid
    private List<SeatAssignmentDTO> assignments;

    public List<SeatAssignmentDTO> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<SeatAssignmentDTO> assignments) {
        this.assignments = assignments;
    }
}

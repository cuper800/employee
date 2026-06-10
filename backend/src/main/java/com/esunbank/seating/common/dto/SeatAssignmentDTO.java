package com.esunbank.seating.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 共用層：單筆座位異動。
 * floorSeatSeq 為 null 代表將該員工的座位清除為空位。
 */
public class SeatAssignmentDTO {

    /** 員編，固定 5 碼數字 (同時防止 SQL Injection 異常輸入) */
    @NotBlank(message = "員編不可為空")
    @Pattern(regexp = "^[0-9]{5}$", message = "員編格式錯誤，需為 5 碼數字")
    private String empId;

    /** 目標座位序號，null 代表清除座位 */
    private Integer floorSeatSeq;

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public Integer getFloorSeatSeq() {
        return floorSeatSeq;
    }

    public void setFloorSeatSeq(Integer floorSeatSeq) {
        this.floorSeatSeq = floorSeatSeq;
    }
}

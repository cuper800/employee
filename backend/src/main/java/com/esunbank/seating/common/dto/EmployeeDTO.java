package com.esunbank.seating.common.dto;

/**
 * 共用層：員工資料傳輸物件 (供下拉選單)。
 */
public class EmployeeDTO {

    private String empId;
    private String name;
    private String email;
    /** 目前座位序號，未就座為 null */
    private Integer floorSeatSeq;

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getFloorSeatSeq() {
        return floorSeatSeq;
    }

    public void setFloorSeatSeq(Integer floorSeatSeq) {
        this.floorSeatSeq = floorSeatSeq;
    }
}

package com.esunbank.seating.common.dto;

/**
 * 共用層：座位資料傳輸物件 (含佔用員工資訊)。
 * empId / empName 為 null 表示空位。
 */
public class SeatDTO {

    private Integer floorSeatSeq;
    private Integer floorNo;
    private Integer seatNo;
    private String empId;
    private String empName;

    public Integer getFloorSeatSeq() {
        return floorSeatSeq;
    }

    public void setFloorSeatSeq(Integer floorSeatSeq) {
        this.floorSeatSeq = floorSeatSeq;
    }

    public Integer getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Integer floorNo) {
        this.floorNo = floorNo;
    }

    public Integer getSeatNo() {
        return seatNo;
    }

    public void setSeatNo(Integer seatNo) {
        this.seatNo = seatNo;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }
}

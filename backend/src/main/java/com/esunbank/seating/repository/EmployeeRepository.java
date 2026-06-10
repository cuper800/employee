package com.esunbank.seating.repository;

import com.esunbank.seating.common.dto.EmployeeDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 資料層：員工資料存取，一律透過 Stored Function 進行。
 */
@Repository
public class EmployeeRepository {

    private final JdbcTemplate jdbcTemplate;

    public EmployeeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<EmployeeDTO> EMPLOYEE_MAPPER = (rs, rowNum) -> {
        EmployeeDTO dto = new EmployeeDTO();
        String empId = rs.getString("emp_id");
        dto.setEmpId(empId == null ? null : empId.trim());
        dto.setName(rs.getString("name"));
        dto.setEmail(rs.getString("email"));
        int seq = rs.getInt("floor_seat_seq");
        dto.setFloorSeatSeq(rs.wasNull() ? null : seq);
        return dto;
    };

    /**
     * 取得所有員工 (供下拉選單)。
     * 使用參數化的 Stored Function 呼叫，避免 SQL Injection。
     */
    public List<EmployeeDTO> findAll() {
        return jdbcTemplate.query("SELECT * FROM fn_get_employees()", EMPLOYEE_MAPPER);
    }
}

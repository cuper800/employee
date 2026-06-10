package com.esunbank.seating.repository;

import com.esunbank.seating.common.dto.SeatDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 資料層：座位資料存取，一律透過 Stored Procedure / Function 進行。
 */
@Repository
public class SeatRepository {

    private final JdbcTemplate jdbcTemplate;

    public SeatRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<SeatDTO> SEAT_MAPPER = (rs, rowNum) -> {
        SeatDTO dto = new SeatDTO();
        dto.setFloorSeatSeq(rs.getInt("floor_seat_seq"));
        dto.setFloorNo(rs.getInt("floor_no"));
        dto.setSeatNo(rs.getInt("seat_no"));
        String empId = rs.getString("emp_id");
        dto.setEmpId(empId == null ? null : empId.trim());
        dto.setEmpName(rs.getString("emp_name"));
        return dto;
    };

    /**
     * 讀取各樓層座位 (含佔用員工)。
     */
    public List<SeatDTO> findLayout() {
        return jdbcTemplate.query("SELECT * FROM fn_get_seating_layout()", SEAT_MAPPER);
    }

    /**
     * 批次套用座位異動。
     * 以參數化方式將 JSON 字串轉型為 jsonb 傳入 Stored Procedure，
     * 由 SP 在同一交易內完成多筆異動 (防止 SQL Injection)。
     *
     * @param assignmentsJson 形如 [{"empId":"12006","floorSeatSeq":3}, ...] 的 JSON 字串
     */
    public void applyAssignments(String assignmentsJson) {
        jdbcTemplate.update("CALL sp_apply_seat_assignments(?::jsonb)", assignmentsJson);
    }
}

package com.esunbank.seating.service.impl;

import com.esunbank.seating.common.dto.AssignmentRequest;
import com.esunbank.seating.common.dto.SeatAssignmentDTO;
import com.esunbank.seating.common.dto.SeatDTO;
import com.esunbank.seating.common.exception.BusinessException;
import com.esunbank.seating.repository.SeatRepository;
import com.esunbank.seating.service.SeatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 業務層：座位服務實作。
 * applyAssignments 以 @Transactional 包覆，確保多筆異動 (多資料列) 全部成功或全部回滾。
 */
@Service
public class SeatServiceImpl implements SeatService {

    private static final Pattern EMP_ID_PATTERN = Pattern.compile("^[0-9]{5}$");

    private final SeatRepository seatRepository;
    private final ObjectMapper objectMapper;

    public SeatServiceImpl(SeatRepository seatRepository, ObjectMapper objectMapper) {
        this.seatRepository = seatRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatDTO> getSeatingLayout() {
        return seatRepository.findLayout();
    }

    @Override
    @Transactional
    public List<SeatDTO> applyAssignments(AssignmentRequest request) {
        if (request == null || request.getAssignments() == null) {
            throw new BusinessException("座位異動資料不可為空");
        }

        List<SeatAssignmentDTO> assignments = request.getAssignments();

        // 業務驗證：員編格式、員工不可重複、座位不可重複指派
        Set<String> seenEmp = new HashSet<>();
        Set<Integer> seenSeat = new HashSet<>();
        for (SeatAssignmentDTO a : assignments) {
            if (a.getEmpId() == null || !EMP_ID_PATTERN.matcher(a.getEmpId()).matches()) {
                throw new BusinessException("員編格式錯誤，需為 5 碼數字：" + a.getEmpId());
            }
            if (!seenEmp.add(a.getEmpId())) {
                throw new BusinessException("同一員工重複指派：" + a.getEmpId());
            }
            // 每位員工只能佔用一個座位，故同一座位也不可被指派給多人
            if (a.getFloorSeatSeq() != null && !seenSeat.add(a.getFloorSeatSeq())) {
                throw new BusinessException("同一座位重複指派：" + a.getFloorSeatSeq());
            }
        }

        // 序列化為 JSON 傳入 Stored Procedure (於同一交易內完成批次異動)
        try {
            String json = objectMapper.writeValueAsString(assignments);
            seatRepository.applyAssignments(json);
        } catch (JsonProcessingException e) {
            throw new BusinessException("座位異動資料格式錯誤", e);
        }

        return seatRepository.findLayout();
    }
}

package com.esunbank.seating.controller;

import com.esunbank.seating.common.dto.ApiResponse;
import com.esunbank.seating.common.dto.AssignmentRequest;
import com.esunbank.seating.common.dto.SeatDTO;
import com.esunbank.seating.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 展示層：座位 RESTful API。
 */
@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    /** 取得各樓層座位佈局 */
    @GetMapping
    public ApiResponse<List<SeatDTO>> getSeats() {
        return ApiResponse.ok(seatService.getSeatingLayout());
    }

    /** 送出：批次套用座位異動 (Transaction)，回傳更新後佈局 */
    @PostMapping("/assignments")
    public ApiResponse<List<SeatDTO>> applyAssignments(@Valid @RequestBody AssignmentRequest request) {
        return ApiResponse.ok("座位更新成功", seatService.applyAssignments(request));
    }
}

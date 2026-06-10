package com.esunbank.seating.controller;

import com.esunbank.seating.common.dto.ApiResponse;
import com.esunbank.seating.common.dto.EmployeeDTO;
import com.esunbank.seating.service.EmployeeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 展示層：員工 RESTful API。
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /** 取得員工清單 (供下拉選單) */
    @GetMapping
    public ApiResponse<List<EmployeeDTO>> getEmployees() {
        return ApiResponse.ok(employeeService.getAllEmployees());
    }
}

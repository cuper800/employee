package com.esunbank.seating.service;

import com.esunbank.seating.common.dto.EmployeeDTO;

import java.util.List;

/**
 * 業務層：員工相關服務介面。
 */
public interface EmployeeService {

    List<EmployeeDTO> getAllEmployees();
}

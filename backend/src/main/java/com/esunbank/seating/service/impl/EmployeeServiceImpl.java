package com.esunbank.seating.service.impl;

import com.esunbank.seating.common.dto.EmployeeDTO;
import com.esunbank.seating.repository.EmployeeRepository;
import com.esunbank.seating.service.EmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 業務層：員工服務實作。
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findAll();
    }
}

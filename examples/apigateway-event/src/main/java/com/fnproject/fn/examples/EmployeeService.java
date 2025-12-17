package com.fnproject.fn.examples;

import java.util.Optional;

public class EmployeeService {

    public ResponseEmployee createEmployee(RequestEmployee requestEmployee, Optional<String> id) {
        if (requestEmployee == null) {
            throw new IllegalArgumentException("requestEmployee must not be null");
        }
        if (!id.isPresent()) {
            throw new IllegalArgumentException("id must not be null");
        }
        ResponseEmployee employee = new ResponseEmployee();
        employee.setId(Integer.parseInt(id.get()));
        employee.setName(requestEmployee.getName());
        return employee;
    }
}

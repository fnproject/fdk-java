package com.fnproject.fn.examples;

import java.util.Objects;

public class ResponseEmployee {
    private Integer id;
    private String name;

    public ResponseEmployee() {}

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponseEmployee employee = (ResponseEmployee) o;
        return id.equals(employee.id) &&
            Objects.equals(name, employee.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
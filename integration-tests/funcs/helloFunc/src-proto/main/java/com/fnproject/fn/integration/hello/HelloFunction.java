package com.fnproject.fn.integration.hello;

public class HelloFunction {
    public String handleRequest(String input) {
        String name = (input == null || input.isEmpty()) ? "world" : input;
        return "Hello, " + name + "!";
    }
}
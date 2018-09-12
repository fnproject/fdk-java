package com.fnproject.fn.integration.test2;



public class PlainFunction {


    public String handleRequest(String input) {
        String name = (input == null || input.isEmpty()) ? "world"  : input;

        return "hello , " + name + "!";
    }
}

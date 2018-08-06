package com.example.fn;

import com.fnproject.fn.api.*;


public class CorsFunction {


    @FnConfiguration
    public static void init(RuntimeContext rc) {
        rc.setInvoker(new CorsInvoker());
    }

    public String handleRequest(String input) {
        String name = (input == null || input.isEmpty()) ? "world" : input;

        return "Hello, " + name + "!";
    }

}
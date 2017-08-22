package com.fnproject.fn.runtime.testfns;

public class ErrorMessages {

    public class NoMethodsClass {
    }

    public class OneMethodClass {
        public String theMethod(String x){
            return x;
        }
    }

    public static class OtherMethodsClass {
        public String takesAnInteger(Integer x){
            return "It was " + x;
        }
    }

}

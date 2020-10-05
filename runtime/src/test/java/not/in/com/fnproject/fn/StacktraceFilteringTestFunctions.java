/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package not.in.com.fnproject.fn;

import com.fnproject.fn.api.FnConfiguration;

public class StacktraceFilteringTestFunctions {

    public static class ExceptionInConstructor {

        public ExceptionInConstructor(){
            throw new RuntimeException("Whoops I wrote a constructor which throws");
        }

        public String invoke(){
            throw new RuntimeException("This should not be called");
        }
    }

    public static class DeepExceptionInConstructor {

        public DeepExceptionInConstructor(){
            // "Deep" means only one level deep...
            naughtyMethod();
        }

        private void naughtyMethod() {
            throw new RuntimeException("Inside a method called by the constructor");
        }

        public String invoke(){
            throw new RuntimeException("This should not be called");
        }
    }

    public static class NestedExceptionInConstructor {

        public NestedExceptionInConstructor(){
            try {
                naughtyMethod();
            } catch (Exception e){
                throw new RuntimeException("Oh no!", e);
            }
        }

        private int naughtyMethod() {
            int a = 1;
            int b = (a*a>0?0:0);
            return a/b;  //   Explicitly putting "1/0" is a compile-time error
        }

        public String invoke(){
            throw new RuntimeException("This should not be called");
        }
    }

    public static class ExceptionInConfiguration {

        @FnConfiguration
        public void config(){
            throw new RuntimeException("Config fail");
        }

        public void invoke(){}
    }

    public static class DeepExceptionInConfiguration {

        @FnConfiguration
        public void config(){
            throwDeep(20);
        }

        private void throwDeep(int n){
            if (n==0) {
                throw new RuntimeException("Deep config fail");
            }
            throwDeep(n-1);
        }


        public void invoke(){}
    }


    public static class CauseStackTraceInResult {


        private void throwTwo(){

            try{
                throwOne();
            }catch(Exception e){
                throw new RuntimeException("Throw two",e);
            }
        }

        private void throwOne(){
                throw new RuntimeException("Throw one");
        }


        public void invoke(){
            throwTwo();
        }
    }
    public static class NestedExceptionInConfiguration {

        @FnConfiguration
        public void config(){
            throwDeep(3);
        }

        private void throwDeep(int n){
            if (n==0) {
                throw new RuntimeException("Deep config fail");
            }
            try {
                throwDeep(n - 1);
            } catch (Exception e){
                throw new RuntimeException("nested at " + n, e);
            }
        }


        public void invoke(){}
    }
}

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

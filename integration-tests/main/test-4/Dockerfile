FROM fnproject/fn-java-runtime:latest
WORKDIR /function
COPY target/*.jar /function/app/
CMD ["com.fnproject.fn.integration.ExerciseEverything::handleRequest"]

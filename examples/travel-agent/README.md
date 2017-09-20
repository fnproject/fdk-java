# Travel Agent fn (flow) demo

1. Get latest code
   1. Install fn cli latest
   1. `docker pull fnproject/functions:latest`
   1. `docker pull fnproject/completer:latest`
   1. `docker pull fnproject/completer:ui`
   1. `docker pull fnproject/tteggel:bristol`
  
1. Clean local environment
   1. `./scripts/stop.sh`
   1. `sudo rm -rf ./data`
  (it seems that `./data` is written to by a privileged container hence `sudo`)
   1. Remove the bits we are going to live code: `rm -rf ./flight/book ./trip`

1. Initialise local environment
   1. `./scripts/start.sh`

1. Your first function
   1. `mkdir -p ./flight/book && cd ./flight/book`
   1. `fn init --runtime=java`
   1. Import to intellij and show boilerplate
   1. Edit `func.yaml`:
      1. Add docker repo `fn-example` to `name:` field
      1. Add `path:` field set to `/flight/book`
   1. `fn run`
   1. `fn apps create travel`
   1. `fn deploy --local --app travel`
   1. `fn call travel /flight/book`

1. It is tested
   1. See `src/test/java/com/example/fn/HelloFunctionTest.java`

1. Call it something sensible
   1. Rename/refactor (shift-F6) `HelloFunction` to `FlightFunction`
   1. Rename/refactor `HelloFunctionTest` to `FlightFunctionTest`
   1. Show that entrypoint in `func.yaml` has changed too
   1. Rename/refactor method `handleRequest` to `book`
   1. Rename the `artifactId` in `pom.xml` to `flight-book`
   1. Everything still works: `fn run`
     (This takes a little longer as `pom.xml` changes invalidate cached layers)

1. Handle JSON easily (and strongly typed)
   1. Create an inner-class of `FlightFunction` to define input schema: 
      ```java
       public static class FlightBookingRequest {
           public Date departureTime;
           public String flightCode;
       }   
 
       public static class FlightBookingResponse {
           public String confirmation;
       }
       ```
   1. Change signature of `book` method to take `FlightBookingRequest`:
      ```java
      public FlightBookingResponse book(FlightBookingRequest input) {
          FlightBookingResponse response = new FlightBookingResponse();
          response.confirmation = "DANFGJ";
          return response;
      }
      ```
   1. Change `FlightFunctionTest` to test invalid input is not deserialised:
      ```java
      @Test
      public void shouldRejectNullInput() {
          setupGoodConfig();
          testing.givenEvent().enqueue();
          testing.thenRun(FlightFunction.class, "book");
          String stderr = testing.getStdErrAsString();
    
          String expected = "An exception was thrown during Input Coercion:";
          assertEquals(expected, stderr.substring(0, expected.length()));
      }
      ```
   1. Run tests in intellij. See the green. 
   1. Test for valid input:
      ```java
      private static final FlightFunction.FlightBookingRequest test_data = new FlightFunction.FlightBookingRequest();
      private static final ObjectMapper defaultMapper = new ObjectMapper();
      {
          test_data.departureTime = new Date(System.currentTimeMillis());
          test_data.flightCode = "BA12345";
      }
    
      private void setupGoodData() throws JsonProcessingException {
          testing.givenEvent().withBody(defaultMapper.writeValueAsBytes(test_data)).enqueue();
      }
    
      @Test
      public void shouldAcceptValidInput() throws JsonProcessingException {
          setupGoodData();
          testing.thenRun(FlightFunction.class, "book");            
          String stderr = testing.getStdErrAsString();
          assertEquals("", stderr);
      }
      ```
   1. Before we can run in `fn` we need some test data. Create `sample-payload.json` with some test data:
      ```json
      {
        "departureTime": "2017-10-01",
        "flightCode": "BA286"
      }
      ```
   1. Then `cat sample-payload.json | fn call travel /flight/book` gives us a nice easy way to test the function in a 
      production-like context.
   1. Data binding is customizble:
      1. Tweak the JSON mapping configuration (using Jackson under the hood)
      1. Write your own data bindings to support e.g. protobuf, gRPC, your own custom format
   
1. We can easily access configuration
   1. In `FlightFunction`:
      ```java
      @FnConfiguration
      public void configure(RuntimeContext ctx) {
          String airlineApiUrl = ctx.getConfigurationByKey("FLIGHT_API_URL")
                  .orElseThrow(() -> new RuntimeException("No URL endpoint was provided."));
   
          String airlineApiSecret = ctx.getConfigurationByKey("FLIGHT_API_SECRET")
                  .orElseThrow(() -> new RuntimeException("No secret was provided."));
      }
      ```
   1. And test that we handle it properly:
      ```java
      private void setupGoodConfig() {
          testing.setConfig("FLIGHT_API_URL", "http://localhost:3000/flight");
          testing.setConfig("FLIGHT_API_SECRET", "shhh");
      }
   
      @Test
      public void shouldFailWithNoConfig() throws JsonProcessingException {
          setupGoodData();
          testing.thenRun(FlightFunction.class, "book");
          String stderr = testing.getStdErrAsString();
    
          String expected = "Error invoking configuration method: configure\n" +
                  "Caused by: java.lang.RuntimeException: No URL endpoint was provided.";
          assertEquals(expected, stderr.substring(0, expected.length()));
      }
      ```
   1. Correct the regressions in the previous tests by adding call to `setupGoodConfig()`
   1. We can provide config at the appropriate level of granularity: app, route and by API, CLI or in `func.yaml`:
      ```yaml
      config:
        FLIGHT_API_URL: http://172.17.0.1:3001/flight
      ```
      ```sh
      fn routes config set travel /flight/book FLIGHT_API_SECRET "shhhh"
      fn deploy --app travel --local
      ```
   
1. Calling our partner API
   1. Our airline partners have provided a nice Java API. TODO: publish and include via pom?   
      ```sh
      cp -r ../../demo/goplacesairlines src/main/java/com/
      ```
      Add these in `pom.xml`:
      ```xml
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>fluent-hc</artifactId>
            <version>RELEASE</version>
        </dependency>
      ```
   1. Let's instantiate it in the `configure` method:
      ```java
      this.apiClient = new GoPlacesAirlines(airlineApiUrl, airlineApiSecret);
      ```
   1. So our `book` method can call the API:
      ```java
      public FlightBookingResponse book(FlightBookingRequest input) {
          FlightBookingResponse response = new FlightBookingResponse();
          GoPlacesAirlines.BookingResponse goPlacesResult = apiClient.bookFlight(input.flightCode, input.departureTime);
          response.confirmation = goPlacesResult.confirmation;
          return response;
      }
      ```
   1. Need to remove the shouldAcceptValidInput test at this point. TODO: maybe don't do that test in the first place?  

1. Introducing the fake API server
   1. All being well the last `fn call` will have returned a result:
      ```json
      {"confirmation":"boom"}
      ```
      Where did this come from?
   1. We are running some fake APIs on port 3001 so that we can have some offline APIs to call.
   1. We are running a live web dashboard on port 3000 so that we can see what calls our functions are making.
   1. Open `http://localhost:3000`.
   1. Show the `POST` request under the **Book Flight** heading. Click on it to show request/response details. This
      represents the call that our function made to our partners API. You can see that we sent the secret successfully.
   1. This is where the `boom` came from and was passed back to our function via the partner's SDK.
   1. The fake API is dumb. It will just return a single canned response for each endpoint that it fakes out. We can
      change the canned response (more on this later).
   1. Do some more `cat sample-payload.json | fn call travel /flight/book` - maybe change the payload. See the requests
      appear on the UI.

1. Use the best language for the job:
   1. `cd ../..`
   1. Not everyone provides a good Java SDK :(
   1. Our hotel provider, for example, has an excellent ruby SDK.
   1. We have created some `/hotel/` and `/car/` and `/email/` functions.
   1. The hotel functions use Ruby and the car ones use nodejs.
   1. They simply forward the requests to the fake APIs and are bare-minimum StackOverflow copy-pasta to do so. Show at 
      your own risk!
   1. We can deploy them as an app:
      ```sh
      fn deploy --all
      ```
   1. Then we will need to configure them:
      ```sh
      export DOCKER_LOCALHOST=$(docker inspect --type container -f '{{.NetworkSettings.Gateway}}' functions)
      
      fn routes config set travel /hotel/book HOTEL_API_URL "http://$DOCKER_LOCALHOST:3001/hotel"
      fn routes config set travel /car/book CAR_API_URL "http://$DOCKER_LOCALHOST:3001/car"
      ```
   1. And now we can call them in the same way:
      ```sh
      cat hotel/book/sample-payload.json | fn call travel /hotel/book
      cat car/book/sample-payload.json | fn call travel /car/book
      ```
   1. Check the fake API dashboard to see that our functions have forwarded the requests properly.

1. Go with the fn flow:
   1. We want to deploy a new function to handle booking *trips*. A trip will allow reliable booking of a flight, hotel 
   and car rental with one API call.
   1. `mkdir trip && cd trip`
   1. `fn init --runtime=java`
   1. Re-import maven projects in intellij
   1. Refactor/rename as before:
      1. `pom.xml`
      1. Class names
      1. `handleRequest` method name
      1. `func.yaml`
   1. Define the JSON message formats that the trip function will be talking:
      1. Create a new package `messages`.
      1. Create a new class package `TripRequest` in `messages`:
         ```java
         public class TripRequest implements Serializable {
             public FlightRequest flight;
             public HotelRequest hotel;
             public CarRentalRequest carRental;
         
             public static class FlightRequest implements Serializable {
                 public Date departureTime;
                 public String flightCode;
             }
         
             public static class HotelRequest implements Serializable {
                 public String city;
                 public String hotel;
             }
         
             public static class CarRentalRequest implements Serializable {
                 public String model;
             }
         }
         ```
      1. Create a new class `BookingResponse` in `messages` package:
         ```java
         public class BookingResponse implements Serializable {
             public String confirmation;
         }
         ```
   1. Change the signature of our `book` method to use these new classes:
      ```java
      public void book(TripRequest input) {
      ```
   1. Create some wrappers to make calling our functions easy:
      1. Copy in the `JsonHelper` class. This provides some utility methods for using functions that consume and produce JSON.
         ```sh
         cp ../demo/JsonHelper.java src/main/java/com/example/fn
         ```
      1. Create a new class `Functions`.
      1. Add wrappers for our flight, hotel and car functions that we created earlier:
         ```java
         public static FlowFuture<BookingResponse> bookFlight(TripRequest.FlightRequest req) {
             return JsonHelper.wrapJsonFunction("./flight/book", req, BookingResponse.class);
         }
     
         public static FlowFuture<BookingResponse> bookHotel(TripRequest.HotelRequest req) {
             return JsonHelper.wrapJsonFunction("./hotel/book", req, BookingResponse.class);
         }
     
         public static FlowFuture<BookingResponse> bookCar(TripRequest.CarRentalRequest req) {
             return JsonHelper.wrapJsonFunction("./car/book", req, BookingResponse.class);
         }
         ```
         These describe the path to the functions and provide type-safe access to the json that they consume and produce.
   1. The current flow is retrieved using `Flow f = Flows.currentFlow();`. Add this to the top of the `book` method.
   1. Now we can call these functions and compose the results in a number of different ways. For example in parallel:
      ```java
      f.allOf(Functions.bookFlight(input.flight),
              Functions.bookHotel(input.hotel),
              Functions.bookCar(input.carRental));
      ```
   1. Introduce and show Flow UI.
   1. Or we can call one after the other. Replace the above statement with:
      ```java
      Functions.bookFlight(input.flight)
          .thenAccept((flightResponse) -> Functions.bookHotel(input.hotel)
              .thenAccept((hotelResponse) -> Functions.bookCar(input.carRental)
                  .whenComplete((carResponse, error) -> System.out.println(carResponse.confirmation))));
      ```
      The other stages that we're seeing are glue code (see `JsonHelper` class). Fn Flow provides a way to run glue code inline with the rest of 
      your flow while maintaining 'serverless' properties. This keeps your functions clean and composable.
1. Coping with failure
   1. Let's say that our hotel booking partner fills up a hotel in between us showing it as available to our customer 
      and the customer asking to book it. We might have already booked our flight - oh no! We need a rebust way to handle failure 
      cases. We are going to need some compensating actions.
   1. Firstly let's create some functions that allow us simulate those. We will create endpoints that enable us to cancel a
      flight, hotel or car rental booking. In fact when we did that `deploy --all` earlier on we already created them!
      We just need some wrappers. So in `Functions`:
      ```java
      public static FlowFuture<BookingResponse> cancelFlight(TripRequest.FlightRequest req) {
          return JsonHelper.wrapJsonFunction("./flight/cancel", req, BookingResponse.class);
      }
    
      public static FlowFuture<BookingResponse> cancelHotel(TripRequest.HotelRequest req) {
          return JsonHelper.wrapJsonFunction("./hotel/cancel", req, BookingResponse.class);
      }
    
      public static FlowFuture<BookingResponse> cancelCar(TripRequest.CarRentalRequest req) {
          return JsonHelper.wrapJsonFunction("./car/cancel", req, BookingResponse.class);
      }

      public static FlowFuture<Void> sendEmail(EmailRequest req) {
          return JsonHelper.wrapJsonFunction("./email", req);
      }
      ```
   1. We are also going to have a way to email our users to keep them informed hence the email fn above.
   1. Also define that `EmailRequest` class in the `messages` package so that we know how to talk to our email provider:
      ```java
      public class EmailRequest implements Serializable {
          public String message;
      }
      ```
   1. We can use the `exceptionallyCompose` method to add on to our chain of functions some more work to handle errors:
      ```java
      Functions.bookFlight(input.flight)
          .thenCompose((flightResponse) -> Functions.bookHotel(input.hotel)
              .thenCompose((hotelResponse) -> Functions.bookCar(input.carRental)
                  .whenComplete((carResponse, error) -> System.out.println(carResponse.confirmation))
                  .exceptionallyCompose((e) -> {Functions.cancelCar(input.carRental); return f.failedFuture(e);}))
              .exceptionallyCompose((e) -> {Functions.cancelHotel(input.hotel); return f.failedFuture(e);}))
          .exceptionallyCompose((e) -> {Functions.cancelFlight(input.flight); return f.failedFuture(e);});

      ```
   1. Send an email with confirmation numbers when we succeed:
      ```java
      .whenComplete((carResponse,r)->Functions.sendEmail(composeEmail(flightResponse, hotelResponse, carResponse)))
      ```
      With this helper:
      ```java
      private EmailRequest composeEmail(BookingResponse flightResponse,
                                        BookingResponse hotelResponse,
                                        BookingResponse carResponse) {
          EmailRequest result = new EmailRequest();
          result.message = "Flight confirmation: " + flightResponse.confirmation + "\n" +
                           "Hotel confirmation: " +  hotelResponse.confirmation + "\n" +
                           "Car rental confirmation: " + carResponse.confirmation;
          return result;
      ```
   1. Send an email on failure:
      ```java
       .exceptionally((e) -> {Functions.sendEmail(new EmailRequest());return null;});
      ```
1. Composable programming FTW
   1. Small self-contained functions.
   1. No knowledge of overall flow and no special handling of input and output for previous / next stages.
   1. Overall flow programmed in one place.
   1. Glue code alongside flow code.
   1. No 'programming' in JSON / BEPL. A real programming language.
      1. Testable.
      1. Great local dev experience.
   1. Higher-order abstractions. e.g. Retry:
      ```java
      Functions.bookFlight(input.flight)
          .thenCompose((flightResponse) -> Functions.bookHotel(input.hotel)
              .thenCompose((hotelResponse) -> Functions.bookCar(input.carRental)
                  .whenComplete((carResponse,r)->Functions.sendEmail(composeEmail(flightResponse, hotelResponse, carResponse)))
                  .exceptionallyCompose((e) -> {Retry.exponentialWithJitter(() -> Functions.cancelCar(input.carRental));return f.failedFuture(e);}))
              .exceptionallyCompose((e) -> {Retry.exponentialWithJitter(() -> Functions.cancelHotel(input.hotel));return f.failedFuture(e);}))
          .exceptionallyCompose((e) -> {Retry.exponentialWithJitter(() -> Functions.cancelFlight(input.flight));return f.failedFuture(e);})       
      .exceptionally((e) -> {Functions.sendEmail(new EmailRequest());return null;});
      ```
# Experimental support tools for functions native images

This is an optional module that can be added to native-image builds that resolves some common issues related to
native-image handling in oracle functions.

This library is _experimental_ - it may change in behaviour and may not work in many cases. 

Currently, this contains graal native-image configuration files which enable common use cases for :

* Oracle Cloud Infrastructure java client support in native images
* Graal native reflection config for general Jersey client support (specifically tested for the OCI client use case, may work in other cases)
* Graal native reflection config for  BouncyCastle crypto in native images 

It also includes :

* A dynamic graal-native feature which automatically adds classes referenced in Jackson-databind annotations - this removes the need to have to manually add model classes that are referenced via Jackson annotations like `@JsonSubTypes.Type` `@JsonDeserlize` etc. 

# Enabling the feature in a native build:

Generate a native build function (replace 1.0.121 with the appropriate fdk-java version):

```
fn init --init-image fnproject/fn-java-native-init:jdk11-1.0.121  graalfn
```

Edit your pom file and add this library as a dependency:

```xml
<dependency>
    <groupId>com.fnproject.fn</groupId>
    <artifactId>experimental-native-image-support</artifactId>
    <version>${fdk.version}</version>
    <scope>runtime</scope>
</dependency>
```

Edit the generated `Dockerfile` and add the following to enable the feature:

```
    --features=com.fnproject.fn.nativeimagesupport.JacksonFeature \
```
You may also need to add the following flags if they are not already set: 
```
    --allow-incomplete-classpath \
    --enable-all-security-services \
    --enable-url-protocols=https \
    --report-unsupported-elements-at-runtime \
```

You may see "WARNING:..." messages during the build, these are expected and should not cause issues. 

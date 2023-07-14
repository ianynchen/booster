# booster-web

Booster Web

## Purpose 

To return a response with either a normal response body, or an error message.

## Getting started

You don't need to do anything in order for booster-web to work. Just write your controllers using 
standard Spring ```RestController``` annotation, and let it return Mono<Either<Throwable, T>> objects, where **T** is 
the response body type.

[ResposneHandler](src/main/java/io/github/booster/web/handler/ResponseHandler.java) will automatically
convert your Mono<Either<Throwable, T>> return values into [WebResponse](src/main/java/io/github/booster/web/handler/response/WebResponse.java) object.

You can use a Java Throwable to report the error, in which case booster-web will package it into an error with
INTERNAL_SERVER_ERROR message. If you want more control over the error message, you can pass in a [WebException](src/main/java/com/ld/booster/web/handler/response/WebException.java).

Springfox integration is also added to automatically convert ```Mono<Either<Throwable, T>>``` return 
types to ```WebResponse<T>``` for clearer documentation.

## Response Structure

Each response has either a **response** section or an **error** section depending 
on whether it is a normal response or response with errors.

**error** contains the error populated from upstream services or from current service internally.

## Response Construction

Returning **Mono<Either<Throwable, T>>** in **RestController** will result in a **[ResponseEntity<WebResponse<?>>](src/main/java/com/ld/booster/web/handler/response/WebResponse.java)**
constructed automatically:

1. If **Either** is a right value but right hand side is null, or is a left value but left hand side is null, an empty response will be created with status code of 200.
2. If **Either** is a right value and right hand side is non-null, a 200 response will be created with the body being the response object.
3. if **Either** is a left value and left hand side is non-null, an error will be created with corresponding status code.

### Exception Handling

In order to leverage response handling, one needs to create a subclass of **ExceptionHandler**
with the type of **Exception** one wants to handle, and mark this class as a **bean**.

Booster will pick all such **ExceptionHandler** classes and use them to convert certain
exceptions to HTTP responses. A default handler is provided to convert any **Throwable** to 
an HTTP internal server error response.

If the default handler is not enough, provide your own handler. Any subclass exceptions 
will be handled by your handler as well. All handlers are evaluated in sequence and the 
first one that can handle the type of exception will be used to do the conversion. 

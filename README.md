# Web Engineering 2020-2021 / URL Shortener

[![Build Status](https://travis-ci.com/andrewknoll/UrlShortener.svg?branch=master)](https://travis-ci.com/github/andrewknoll/UrlShortener)

This is the start repository for the project developed in this course. 

The __project__ is a [Spring Boot](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/) application that offers a minimum set of functionalities shared by all subprojects.

* __Short URL creation service__:  `POST /` creates a shortened URL from a URL in the request parameter `url`.
* __Redirection service__: `GET /{id}` redirects the request to a URL associated with the parameter `id`.
* __Database service__: Persistence and retrieval of `ShortURL` and `Click` objects.


The application can be run in Linux and macOS as follows:

```
$ ./gradlew :app:bootRun
```
or in Windows

```
$ gradle.bat :app:bootRun
```

In the case of the qr functionality, 2 instances of QR have to be initialized. To do so:
```
$ ./gradlew :qr:bootRun -Pport=8179
$ ./gradlew :qr:bootRun -Pport=8180
```
or in Windows

```
$ gradle.bat :qr:bootRun -Pport=8179
$ gradle.bat :qr:bootRun -Pport=8180
```
Ports 8179 and 8190 are the default ones, and the value of the ip is set to "localhost".
Should you require changing these ports/ip, change their values in app/src/main/resources/application.properties

Gradle will compile project and then run it
Now you have a shortener service running at port 8080. 
You can test that it works as follows:

```bash
$ curl -v -d "url=http://www.unizar.es/" -X POST http://localhost:8080/link
> POST / HTTP/1.1
> User-Agent: curl/7.37.1
> Host: localhost:8080
> Accept: */*
> Content-Length: 25
> Content-Type: application/x-www-form-urlencoded
>
* upload completely sent off: 25 out of 25 bytes
< HTTP/1.1 201 Created
< Server: Apache-Coyote/1.1
< Location: http://localhost:8080/6bb9db44
< Content-Type: application/json;charset=UTF-8
< Transfer-Encoding: chunked
<
* Connection #0 to host localhost left intact
{"hash":"6bb9db44","target":"http://www.unizar.es/","uri":"http://localhost:8080/6bb9db44",
"sponsor":null,"created":"2019-09-10","owner":"112b6444-0a05-4e48-a13f-27ddf23349e2","mode":307,
"safe":true,"ip":"0:0:0:0:0:0:0:1","country":null}%
```

Now, we can navigate to the shortened URL.

```bash
$ curl -v http://localhost:8080/6bb9db44
> GET /6bb9db44 HTTP/1.1
> User-Agent: curl/7.37.1
> Host: localhost:8080
> Accept: */*
>
< HTTP/1.1 307 Temporary Redirect
< Server: Apache-Coyote/1.1
< Location: http://www.unizar.es/
< Content-Length: 0
<
```

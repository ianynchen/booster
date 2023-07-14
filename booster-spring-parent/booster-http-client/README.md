# Booster Http Client

Booster HTTP client based on Spring WebFlux.

## Getting started

## Features 

1. Simplified client creation:
   1. Automatic response compression enablement 
   2. Automatic inclusion of connection pool metrics 
   3. Automatic inclusion of HTTP client metrics 
   4. Optional SSL handshake timeout
2. Simplified invocation interface that takes a ```HttpClientRequestContext``` object.
3. Standard headers used by Helios services included in ```UserContext```
4. Support invoking HttpClient as a ```Task```

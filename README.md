# OpenConext-biometric-idp

[![Build Status](https://travis-ci.org/oharsta/OpenConext-biometric-idp.svg)](https://travis-ci.org/oharsta/OpenConext-biometric-idp)
[![codecov.io](https://codecov.io/github/oharsta/OpenConext-biometric-idp/coverage.svg)](https://codecov.io/github/oharsta/OpenConext-biometric-idp)

OpenConext Biometric Idp

## [Getting started](#getting-started)

### [System Requirements](#system-requirements)

- Java 8
- Maven 3

## [Building and running](#building-and-running)

### [The biometric IdP](#biometric-idp)

This project uses Spring Boot and Maven. To run locally, type:

```bash
mvn spring-boot:run -Drun.jvmArguments="-Dspring.profiles.active=dev"
```

When developing, it's convenient to just execute the applications main-method, which is in [Application](src/main/java/biometric/Application.java).

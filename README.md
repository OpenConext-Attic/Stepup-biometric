# OpenConext-biometric-idp

[![Build Status](https://travis-ci.org/SURFnet/stepup-biometric.svg)](https://travis-ci.org/SURFnet/stepup-biometric)
[![codecov.io](https://codecov.io/github/SURFnet/stepup-biometric/coverage.svg)](https://codecov.io/github/SURFnet/stepup-biometric)

SURFnet stepup-biometric IdP

## [Getting started](#getting-started)

### [System Requirements](#system-requirements)

- Java 8
- Maven 3

### [Building and running](#building-and-running)

This project uses Spring Boot and Maven. To run locally, type:

```bash
mvn spring-boot:run -Drun.jvmArguments="-Dspring.profiles.active=dev"
```

When developing, it's convenient to just execute the applications main-method, which is in [Application](src/main/java/idp/Application.java).

### [Testing](#testing)

There are Spring Boot [integration tests](https://codecov.io/github/oharsta/OpenConext-biometric-idp) and when you run the Application with the ```dev``` profile
then you can test the GUI at [http://localhost:8080](http://localhost:8080). The ```dev``` profile ensures that you will be logged automatically using the
[MockSAMLAuthnFilter](src/main/java/idp/saml/mock/MockSAMLAuthnFilter.java) and the BioMetric API is mocked using the [MockBioMetric](src/main/java/idp/biometric/mock/MockBioMetric.java).

The default behaviour is that 25 polls are made and the status will be completed. Use the browser developers tool to see the SAML message that is posted.

### [Deployment](#deployment)

We use Ansible for deployment. See the inline documentation of [application.properties](src/main/resources/application.properties) for all the environment dependend variables.

### [Metadata](#metadata)

The biometric IdP publishes its [metadata](http://localhost:8080/metadata).

### [Private signing keys and public certificates](#signing-keys)

The OpenSaml library needs a private DSA key to sign the SAML request and the public certificates from the Strong Authentication Service Provider (SA SP). The
public certificate of the SA SP can be copied from the [metadata](https://sa-gw.test.surfconext.nl/gssp/tiqr/metadata).

The private / public key for the Biometric IdP can be generated:

```bash
openssl req -subj '/O=Organization, CN=biometric/' -newkey rsa:2048 -new -x509 -days 3652 -nodes -out biometric.crt -keyout biometric.pem
```

The Java KeyStore expects a pkcs8 DER format for RSA private keys so we have to re-format that key:

```bash
openssl pkcs8 -nocrypt  -in biometric.pem -topk8 -out biometric.der
```

Remove the whitespace, heading and footer from the biometric.crt and biometric.der:

```bash
cat biometric.der |head -n -1 |tail -n +2 | tr -d '\n'; echo
cat biometric.crt |head -n -1 |tail -n +2 | tr -d '\n'; echo
```

Above commands work on linux distributions. On mac you can issue the same command with `ghead` after you install `coreutils`:

```bash
brew install coreutils

cat biometric.der |ghead -n -1 |tail -n +2 | tr -d '\n'; echo
cat biometric.crt |ghead -n -1 |tail -n +2 | tr -d '\n'; echo
```


Add the Biometric key pair to the application.properties file:

```bash
biometric.private.key=${output from cleaning the der file}
biometric.public.certificate=${output from cleaning the crt file}
```

Add the SA SP certificate to the application.properties file:

```bash
sa.public.certificate=${copy & paste from the metadata}
```

# OpenConext-biometric

[![Build Status](https://travis-ci.org/SURFnet/stepup-biometric.svg)](https://travis-ci.org/SURFnet/stepup-biometric)
[![codecov.io](https://codecov.io/github/SURFnet/stepup-biometric/coverage.svg)](https://codecov.io/github/SURFnet/stepup-biometric)

SURFnet step-up biometric IdP

## [Getting started](#getting-started)

### [System Requirements](#system-requirements)

- Java 8
- Maven 3

### [Building and running](#building-and-running)

This project uses Spring Boot and Maven. To run locally, type:

```bash
mvn spring-boot:run -Drun.jvmArguments="-Dspring.profiles.active=dev"
```

When developing, it's convenient to just execute the applications main-method, which is in [Application](src/main/java/biometric/Application.java).

### [Testing](#testing)

There are Spring Boot [integration tests](https://codecov.io/github/oharsta/OpenConext-biometric-idp) and when you run the Application with the ```dev``` profile
then you can test the GUI at [http://localhost:8080](http://localhost:8080). The ```dev``` profile ensures that you will be logged automatically using the
[MockSAMLAuthnFilter](src/main/java/biometric/saml/mock/MockSAMLAuthnFilter.java) and the BioMetric API is mocked using the [MockBioMetric](src/main/java/biometric/api/mock/MockBioMetric.java).

The default behaviour is that 25 polls are made and the status will be completed. Use the browser developers tool to see the SAML message that is posted.

### [Metadata](#metadata)

The biometric IdP publishes its [metadata](http://localhost:8080/metadata).

### [Deployment](#deployment)

We use Ansible for deployment. See the inline documentation of [application.properties](src/main/resources/application.properties) for all the environment dependend variables.

#### [Install Ansible](#install_ansible)

Ansible is the configuration tool we use to describe our servers. To
install for development (version must be >= 2.0):

```bash
brew install python
pip install --upgrade setuptools
pip install --upgrade pip
brew linkapps
brew install ansible
pip install python-keyczar==0.71c
```

This playbook uses a custom vault, defined in filter_plugins/custom_plugins.py in order to encrypt data. We think this is a better solution than the ansible-vault because it allows us to do fine grained encryption instead of a big ansible-vault file.
Also, the creator of ansible-vault admits his solution is not the way to go. See [this blogpost](http://jpmens.net/2014/02/22/my-thoughts-on-ansible-s-vault/).

Retrieve the surfconext-ansible-keystore from a colleague and put it on an encrypted disk partition, to keep it safe even in case of laptop-loss. Here's how to [create an encrypted folder](http://apple.stackexchange.com/questions/129720/how-can-i-encrypt-a-folder-in-os-x-mavericks) on your Mac.

This is how the keystore is created (you don't have to do this because it already exists for this project). See [this blogpost](http://www.saltycrane.com/blog/2011/10/notes-using-keyczar-and-python/) for example.

`keyczart create --location=$ENCRYPTED_VOLUME_HOME/surfconext-ansible-keystore --purpose=crypt`

`keyczart addkey --location=$ENCRYPTED_VOLUME_HOME/surfconext-ansible-keystore --status=primary`

Create the symlink so that our playbook can find the AES key on your encrypted volume:

`ln -s $ENCRYPTED_VOLUME_HOME/surfconext-ansible-keystore ~/.surfconext-ansible-keystore`

#### [Structure](#structure)
The main playbook is `biometric.yml`. Its inventories are kept in the ansible folder (e.g. test).

#### [How to deploy](#how_to_deploy)
You can use the standard ansible-playbook command with optional tags to limit the deployment time.

```bash
ansible-playbook -i test -K biometric.yml --tags "biometric" -u centos
```

Replace the -i variable with the environment where you want to deploy to and change centos to your username on the environment.

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

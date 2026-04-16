Works on Apereo CAS V 8.0.0-RC3 - https://github.com/apereo/cas

For older versions of CAS, please check the branch corresponding to your CAS version : 
* for CAS 7.3.x, check branch [7.3.x](https://github.com/EsupPortail/esup-otp-cas/tree/7.3.x)
* for CAS 7.2.x, check branch [7.2.x](https://github.com/EsupPortail/esup-otp-cas/tree/7.2.x)

Require [esup-otp-api](https://github.com/EsupPortail/esup-otp-api/) $\ge$ v2.2.3

## Config

### cas.properties

add the following:

```
# MFA Esup Otp Authentication
cas.authn.mfa.triggers.global.global-provider-id=mfa-esupotp

# Add translations, you will need to check what are the default from CAS "Message Bundles" properties
cas.messageBundle.baseNames=classpath:custom_messages,classpath:messages,classpath:esupotp_message

# Add your esup-otp-api in Content-Security-Policy:
cas.http-web-request.header.content-security-policy=script-src 'self' 'unsafe-inline' 'unsafe-eval' https://esup-otp-api.univ-ville.fr/; object-src 'none'; worker-src 'self' blob: 'unsafe-inline' 
```
If you want to trust devices for 7 days, you can add this in cas.properties
```
cas.authn.mfa.trusted.core.device-registration-enabled=true                                                                                                                                         
cas.authn.mfa.trusted.core.auto-assign-device-name=true
cas.authn.mfa.trusted.device-fingerprint.cookie.max-age=P7D
```
with auto-assign-device-name, user will not have to choose a name for his device in a web form, it will be automatically assigned.  

### esupotp.properties

Create esupotp.properties in same directory as cas.properties
```
##
# Esup Otp Authentication
#
esupotp[0].name=mfa-esupotp
esupotp[0].rank=0
esupotp[0].urlApi=http://my-api.com:8081
esupotp[0].usersSecret=changeit
esupotp[0].apiPassword=changeit
esupotp[0].byPassIfNoEsupOtpMethodIsActive=false
esupotp[0].otpManagerUrl=https://esup-otp-manager.univ.fr/
esupotp[0].failureMode=CLOSED
```

`name` is the required identifier and is also used as the provider `id`.
To add more ESUP-OTP instances, duplicate the `esupotp[0]` block with `esupotp[1]`, `esupotp[2]`, ... and give each one a distinct `name`.
For each ESUP-OTP provider, Spring exposes a bean with the name `esupOtpService-<name>`, so for example with `mfa-esupotp` you get `esupOtpService-mfa-esupotp` in custom Groovy code.
Spring also exposes the map bean `esupOtpServices`, indexed by each configured `name`.

In esupotp.properties you can also use usual Multifactor Authentication Bypass configurations described here https://apereo.github.io/cas/7.3.x/mfa/Configuring-Multifactor-Authentication-Bypass.html

So for example you can setup bypass with groovy script :
```
esupotp.bypass.groovy.location=file:/etc/cas/config/mfaGroovyBypass.groovy
```

/etc/cas/config/mfaGroovyBypass.groovy :
``` groovy
import java.util.*

def boolean run(authentication, principal, registeredService, provider, logger, httpRequest, ... other_args) {

    if(registeredService.id == 10 && "cn=for.appli-sensible.supervisor,ou=groups,dc=univ-ville,dc=fr" in principal.attributes.memberOf) {
      return true;
    }

    return false;
}
```

### cas/build.gradle

add
``` groovy
...

dependencies {
    ...
    implementation "org.esup-portail:esup-otp-cas:v2.2.0-cas_v7.3.x"
}
```

### log4j2.xml

add
```
<AsyncLogger name="org.esupportail.cas.adaptors.esupotp" level="debug" additivity="false" includeLocation="true">
    <AppenderRef ref="casConsole"/>
    <AppenderRef ref="casFile"/>
</AsyncLogger>
```

If you want to use an untagged version, you can use jitpack.io :

Add in cas/build.gradle
``` groovy
...
repositories {
    ...
    maven {
        url "https://jitpack.io"
    }
}
...

dependencies {
    ...
    implementation "com.github.EsupPortail:esup-otp-cas:master"
}
```

TIPS: Look for https://jitpack.io/#EsupPortail/esup-otp-cas and check the available version you can use


If you want to package locally, with JDK 21 :
```
./gradlew clean build
```

## publishing on central maven repository

This part is only for developers, if you want to publish on central maven repository, you need to have a sonatype account and be a member of the group org.esup-portail. 

esup-otp-cas use jrelease plugin to publish on maven central repository.

You have to configure also your ~/.jreleaser/config.yml file with your sonatype credentials.

See https://jreleaser.org/guide/latest/examples/maven/maven-central.html#_gradle

Next, to publish on central maven repository, with JDK 21, and after setting version on gradle.properties :
```
./gradlew clean build publish jreleaserFullRelease
```

## development environment with docker

You can use docker to setup a development environment with CAS and esup-otp-cas.

For that, run
```
docker compose -f src/etc/docker-compose.yml up
``` 

Next, you can access esup-otp-manager on http://localhost:4000/
It will be redirected to CAS for authentication on http://localhost:8080/cas/login?service=http://localhost:4000/

You can use login/password : 
* admin/pass
* joe/pass
* jack/pass


Note that some ports on your host must be free : 
 * 8080 for CAS, 
 * 3000 for esup-otp-api, 
 * 4000 for esup-otp-manager
 * 3980 for openldap
 * 27017 for mongodb
 * 5005 for remote debugging of CAS

-> you can debug CAS with remote debugging on port 5005, and you can set breakpoints in esup-otp-cas code to see how it works.

If you want to reset the environment (rebuild esup-otp-api/esup-otp-manager, reset mongodb and rebuild esup-otp-cas with your changes...), you can run
```
docker compose -f src/etc/docker-compose.yml down -v
docker compose -f src/etc/docker-compose.yml up --build
```

## Screenshots

![ESUP-OTP-CAS - Phone Authentication](src/etc/esup-otp-cas-1.png)

![ESUP-OTP-CAS - Grid Authentication](src/etc/esup-otp-cas-2.png)


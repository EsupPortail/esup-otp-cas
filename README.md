Works on Apereo CAS V 7.0.10.1 - https://github.com/apereo/cas

## Config

In esupotp.properties

```
##
# Esup Otp Authentication
#
esupotp.rank=0
esupotp.urlApi=http://my-api.com:8081
esupotp.usersSecret=changeit
esupotp.apiPassword=changeit
esupotp.byPassIfNoEsupOtpMethodIsActive=false
esupotp.failureMode=CLOSED
```

In cas.properties

```
# MFA Esup Otp Authentication
cas.authn.mfa.globalProviderId=mfa-esupotp

# Add translations, you will need to check what are the default from CAS "Message Bundles" properties
cas.messageBundle.baseNames=classpath:custom_messages,classpath:messages,classpath:esupotp_message
```
If you want to trust devices for 7 days, you can add this in cas.properties
```
cas.authn.mfa.trusted.core.device-registration-enabled=true                                                                                                                                         
cas.authn.mfa.trusted.core.auto-assign-device-name=true
cas.authn.mfa.trusted.device-fingerprint.cookie.max-age=604800
```
with auto-assign-device-name, user will not have to choose a name for his device in a web form, it will be automatically assigned.  

In esupotp.properties you can also use usual Multifactor Authentication Bypass configurations described here https://apereo.github.io/cas/6.3.x/mfa/Configuring-Multifactor-Authentication-Bypass.html

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

In cas/build.gradle

``` groovy
...

dependencies {
    ...
    implementation "org.esup-portail:esup-otp-cas:v1.2.5-cas_v7.0.x"
}
```

In log4j2.xml
```
<AsyncLogger name="org.esupportail.cas.adaptors.esupotp" level="debug" additivity="false" includeLocation="true">
    <AppenderRef ref="casConsole"/>
    <AppenderRef ref="casFile"/>
</AsyncLogger>
```

Note for authors : to publish on central maven repository, with JDK 21, and after setting version on gradle.properties :
```
./gradlew clean build publish
```

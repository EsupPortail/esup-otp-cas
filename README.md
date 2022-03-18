Works on CAS V6.4.0

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
esupotp.trustedDeviceEnabled=true
esupotp.isDeviceRegistrationRequired=false
esupotp.deviceRegistrationExpirationInDays=7
# 11,12,13,14,18,21 are services ids : if we configure CAS to do ESUP-OTP MFA on these services but the user does not have an esup-otp factor enabled, then we bypass esup-otp
esupotp.byPassServicesIfNoEsupOtpMethodIsActive=11,12,13,14,18,21
```

With esupotp.trustedDeviceEnabled=true, esupotp.isDeviceRegistrationRequired=false and esupotp.deviceRegistrationExpirationInDays=7
we auto register device for MFA (and without form) during 7 days (trusted devices/browsers - hack on cas-server-support-trusted-mfa)

In cas.properties

```
# MFA - google authenticator
cas.authn.mfa.globalProviderId=mfa-esupotp

# Add translations, you will need to check what are the default from CAS "Message Bundles" properties
cas.messageBundle.baseNames=classpath:custom_messages,classpath:messages,classpath:esupotp_message
```

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
repositories {
  ...
  maven {
        url "https://jitpack.io"
    }
}
...

dependencies {
    ...
    implementation "com.github.EsupPortail:esup-otp-cas:6.4.x-SNAPSHOT"
}
```

    TIPS: Look for https://jitpack.io/#EsupPortail/esup-otp-cas and check the available version you can use - here ad586a2475 is ok with 6.3.3 but maybe it's not the last one

In log4j2.xml
```
<AsyncLogger name="org.esupportail.cas.adaptors.esupotp" level="debug" additivity="false" includeLocation="true">
    <AppenderRef ref="casConsole"/>
    <AppenderRef ref="casFile"/>
</AsyncLogger>
```

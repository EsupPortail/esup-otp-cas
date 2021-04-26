Works on CAS V6.0.8.1

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
esupotp.byPassIfNoEsupOtpMethodIsActive=true
esupotp.trustedDeviceEnabled=true
esupotp.isDeviceRegistrationRequired=false
```

In cas.properties

```
# MFA - ESUP-OTP
cas.authn.mfa.globalProviderId=mfa-esupotp

# Add translations, you will need to check what are the default from CAS "Message Bundles" properties
cas.messageBundle.baseNames=classpath:custom_messages,classpath:messages,classpath:esupotp_message
```

In cas/build.gradle

``` groovy
// Tell to springboot to use that version, if not gradle will download 2 versions 2014**** and 20160810, 
// but it will use 2014**** on runtime
ext['json.version'] = 20160810

dependencies {
    compile "org.apereo.cas:cas-server-webapp:${project.'cas.version'}@war"
    
    // Becareful: Conflict with other packages, needs to be first on the list
    compile 'com.github.EsupPortail:esup-otp-cas:6.0.x-SNAPSHOT'
}
```

    TIPS: Look for https://jitpack.io/#EsupPortail/esup-otp-cas and check the available version you can use

In log4j2.xml
```
<AsyncLogger name="org.esupportail.cas.adaptors.esupotp" level="debug" additivity="false" includeLocation="true">
    <AppenderRef ref="casConsole"/>
    <AppenderRef ref="casFile"/>
</AsyncLogger>
```

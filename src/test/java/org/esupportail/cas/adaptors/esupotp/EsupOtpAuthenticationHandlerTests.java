package org.esupportail.cas.adaptors.esupotp;

import javax.security.auth.login.FailedLoginException;

import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.esupportail.cas.config.EsupOtpConfigurationProperties;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.RequestContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EsupOtpAuthenticationHandlerTests {
    private final EsupOtpService otpService = mock(EsupOtpService.class);
    private final PrincipalFactory principalFactory = mock(PrincipalFactory.class);
    private final EsupOtpAuthenticationHandler handler = new EsupOtpAuthenticationHandler(
        "esup-otp-test", principalFactory, new EsupOtpConfigurationProperties(), otpService);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectsMissingOrBlankOtpBeforeCallingEsupOtp(String token) {
        var credential = new EsupOtpCredential();
        credential.setToken(token);

        var failure = assertThrows(FailedLoginException.class,
            () -> handler.doAuthentication(credential, null));

        assertEquals("Missing OTP code", failure.getMessage());
        verifyNoInteractions(otpService, principalFactory);
    }

    @Test
    void rejectsProviderIdWhenEsupOtpDeniesIt() throws Exception {
        var credential = new EsupOtpCredential();
        credential.setToken("mfa-esupotp");
        when(otpService.verifyOtp("joe", "mfa-esupotp")).thenReturn(new JSONObject().put("code", "Error"));

        var principal = mock(Principal.class);
        when(principal.getId()).thenReturn("joe");
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        var conversation = new LocalAttributeMap<>();
        conversation.put("authentication", authentication);
        var context = mock(RequestContext.class);
        when(context.getConversationScope()).thenReturn(conversation);

        RequestContextHolder.setRequestContext(context);
        try {
            var failure = assertThrows(FailedLoginException.class,
                () -> handler.doAuthentication(credential, null));

            assertEquals("Failed to authenticate OTP", failure.getMessage());
            verify(otpService).verifyOtp("joe", "mfa-esupotp");
            verifyNoInteractions(principalFactory);
        } finally {
            RequestContextHolder.setRequestContext(null);
        }
    }
}

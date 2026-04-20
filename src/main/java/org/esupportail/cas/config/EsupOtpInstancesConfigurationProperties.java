package org.esupportail.cas.config;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Root wrapper for all ESUP-OTP instance configurations.
 *
 * <p>Binds the {@code esupotp[*]} list from the application properties into a typed list.
 * Each entry in the list corresponds to one ESUP-OTP API instance and maps to an
 * {@link EsupOtpConfigurationProperties} object.
 *
 * <p>Example configuration (in {@code application.properties} or {@code cas.properties}):
 * <pre>
 * esupotp[0].name=mfa-esupotp-simple
 * esupotp[0].urlApi=https://...
 * esupotp[0].usersSecret=secret
 * esupotp[0].apiPassword=password
 * esupotp[0].otpManagerUrl=https://...
 * </pre>
 *
 * <p>Add more indexed blocks ({@code esupotp[1]}, {@code esupotp[2]}, ...) if you need more instances.
 * This class is registered via {@link EsupOtpAutoConfiguration}.
 */
@ConfigurationProperties(prefix = "esupotp")
@Getter
@Setter
public class EsupOtpInstancesConfigurationProperties {

    /**
     * List of ESUP-OTP instance configurations.
     */
    private List<EsupOtpConfigurationProperties> esupotp = new ArrayList<>();
}

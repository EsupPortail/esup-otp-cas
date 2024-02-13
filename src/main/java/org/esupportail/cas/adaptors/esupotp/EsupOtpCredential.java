package org.esupportail.cas.adaptors.esupotp;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apereo.cas.authentication.credential.AbstractCredential;

/**
 * This is {@link EsupOtpCredential}.
 *
 * @author Alex Bouskine
 * @since 5.0.0
 */
public class EsupOtpCredential extends AbstractCredential {
	
    private static final long serialVersionUID = -7570600701132111037L;

    private String token;

    private String transport;

    private String method;

    private String uid;

    private String userHash;

	/**
     * Instantiates a new Esup otp token credential.
     */
    public EsupOtpCredential() {
    }

    @Override
    public String getId() {
        return this.token;
    }


    public String getToken() {
        return this.token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public String getTransport() {
        return this.transport;
    }

    public void setTransport(final String transport) {
        this.transport = transport;
    }

    public String getMethod() {
        return this.method;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    public String getUid() {
        return this.uid;
    }

    public void setUid(final String uid) {
        this.uid = uid;
    }

    public String getUserHash() {
        return this.userHash;
    }

    public void setUserHash(final String userHash) {
        this.userHash = userHash;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("token", token)
            .append("transport", transport)
            .append("method", method)
            .append("uid", uid)
            .append("userHash", userHash)
            .toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(token)
            .append(transport)
            .append(method)
            .append(uid)
            .append(userHash)
            .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EsupOtpCredential other = (EsupOtpCredential) obj;
        return new EqualsBuilder()
            .append(token, other.token)
            .append(transport, other.transport)
            .append(method, other.method)
            .append(uid, other.uid)
            .append(userHash, other.userHash)
            .isEquals();
    }

    public boolean isValid() {
        return this.token != null;
    }
}

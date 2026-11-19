package org.esupportail.cas.adaptors.esupotp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Calendar;
import java.util.TimeZone;

import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.TimeValue;
import org.apereo.cas.util.http.HttpRequestUtils;
import org.apereo.cas.util.http.SimpleHttpClient;
import org.apereo.cas.util.http.SimpleHttpClientFactoryBean;
import org.esupportail.cas.config.EsupOtpConfigurationProperties;
import org.json.JSONObject;
import org.springframework.beans.factory.DisposableBean;

/**
 * Client for ESUP-OTP API operations that verify codes and retrieve user information.
 */
public class EsupOtpService implements DisposableBean {
	private final EsupOtpConfigurationProperties esupOtpConfigurationProperties;
	private final SimpleHttpClient httpClient;

	public EsupOtpService(EsupOtpConfigurationProperties properties) {
		this.esupOtpConfigurationProperties = properties;
		var factory = new SimpleHttpClientFactoryBean();
		factory.setConnectionTimeout(timeoutMillis(properties.getConnectionTimeout()));
		factory.setSocketTimeout(timeoutMillis(properties.getReadTimeout()));
		factory.setResponseTimeout(timeoutMillis(properties.getReadTimeout()));
		factory.setRetryStrategy(new DefaultHttpRequestRetryStrategy(0, TimeValue.ZERO_MILLISECONDS));
		factory.setRedirectsEnabled(false);
		this.httpClient = factory.getObject();
	}

	private static long timeoutMillis(Duration timeout) {
		if (timeout == null || timeout.isNegative() || timeout.isZero() || timeout.toMillis() == 0) {
			throw new IllegalArgumentException("MFA HTTP timeouts must be at least one millisecond");
		}
		return timeout.toMillis();
	}

	/**
	 * Submits an OTP for verification by the ESUP-OTP API.
	 *
	 * @param uid the CAS principal identifier
	 * @param otp the submitted one-time code
	 * @return the API response, which may report that the code was rejected
	 * @throws IOException if the request fails, returns a non-success HTTP status,
	 *                     or yields a missing or invalid JSON body
	 */
	public JSONObject verifyOtp(String uid, String otp) throws IOException {
		return execute("/protected/users/" + uid + "/" + otp + "/", true);
	}

	/**
	 * Retrieves ESUP-OTP user information, including whether the user has an enabled method.
	 *
	 * @param uid the CAS principal identifier
	 * @return the user information returned by the API
	 * @throws IOException if the request fails, returns a non-success HTTP status,
	 *                     or yields a missing or invalid JSON body
	 * @throws NoSuchAlgorithmException if a digest algorithm required to compute the user hash is unavailable
	 */
	public JSONObject getUserInfos(String uid) throws IOException, NoSuchAlgorithmException {
		return execute("/users/" + uid + "/" + getUserHash(uid), false);
	}

	private JSONObject execute(String path, boolean verification) throws IOException {
		// Never propagate transport exceptions: their messages can contain the OTP URL.
		try {
			String baseUrl = esupOtpConfigurationProperties.getUrlApi().replaceAll("/+$", "");
			HttpUriRequestBase request = verification
				? new HttpPost(baseUrl + path) : new HttpGet(baseUrl + path);
			if (verification) {
				request.setHeader("Authorization", "Bearer " + esupOtpConfigurationProperties.getApiPassword());
			}
			// CAS buffers the body; Apache releases the network response on success and failure.
			try (var response = httpClient.wrappedHttpClient().execute(request,
					HttpRequestUtils.HTTP_CLIENT_RESPONSE_HANDLER)) {
				if (response.getCode() < 200 || response.getCode() >= 300) {
					throw new IOException("Unexpected MFA HTTP status: " + response.getCode());
				}
				if (response.getEntity() == null) {
					throw new IOException("Empty MFA response");
				}
				return new JSONObject(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
			}
		} catch (Exception e) {
			throw new IOException(verification
				? "MFA verification request failed" : "MFA user information request failed");
		}
	}

	@Override
	public void destroy() {
		httpClient.destroy();
	}

	public String getUserHash(String uid) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md5Md = MessageDigest.getInstance("MD5");
		String md5 = (new HexBinaryAdapter()).marshal(md5Md.digest(esupOtpConfigurationProperties.getUsersSecret().getBytes()));
		md5 = md5.toLowerCase();
		String salt = md5 + getSalt(uid);
		MessageDigest sha256Md = MessageDigest.getInstance("SHA-256");
		String userHash = (new HexBinaryAdapter()).marshal(sha256Md.digest(salt.getBytes()));
		userHash = userHash.toLowerCase();
		return userHash;
	}

	public String getSalt(String uid) {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		String salt = uid + day + hour;
		return salt;
	}

}

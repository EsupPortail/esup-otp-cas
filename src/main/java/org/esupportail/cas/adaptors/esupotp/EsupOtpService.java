package org.esupportail.cas.adaptors.esupotp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.esupportail.cas.config.EsupOtpConfigurationProperties;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class EsupOtpService {

	EsupOtpConfigurationProperties esupOtpConfigurationProperties;


	public JSONObject verifyOtp(String uid, String otp) throws IOException {
		String url = esupOtpConfigurationProperties.getUrlApi() + "/protected/users/" + uid + "/" + otp + "/";
		URL obj = new URL(url);
		HttpURLConnection con = null;
		con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Authorization", "Bearer " + esupOtpConfigurationProperties.getApiPassword());
		log.info("Mfa-esupotp request send to [{}]", (String) url);
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		log.info("Connection success to [{}]", (String) url);

		return new JSONObject(response.toString());
	}

	public List<Map<String, String>> getTransports(List<EsupOtpMethod> methods) {
		List<Map<String, String>> transports = new ArrayList<>();
		//Map<Integer, Map> transports = new HashMap<>();
		Map <String, String> transport;

		for (EsupOtpMethod method : methods) {
			if (method.getActive() && method.getTransports().size() > 0) {
				for(String transportName : method.getTransports() ){                
					transport = new HashMap<>();  
					transport.put("method", method.getName());
					transport.put("transport", transportName);
					transports.add(transport);
				}
			}
		}
		log.info("Size [{}] [{}]",  + transports.size(), transports.toString());

		return transports;
	}

	public JSONObject getUserInfos(String uid) throws IOException, NoSuchAlgorithmException {
		String url = esupOtpConfigurationProperties.getUrlApi() + "/users/" + uid + "/" + getUserHash(uid);
		URL obj = new URL(url);
		HttpURLConnection con = null;
		con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		log.info("mfa-esupotp request send to [{}]", url.toString());
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		log.debug("getUserInfos({}) : {}", uid, response.toString());

		return new JSONObject(response.toString());
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
	
    public Boolean bypass(List<EsupOtpMethod> methods) throws JSONException, IOException {
        Boolean bypass = true;
        for (EsupOtpMethod method : methods) {
            if (method.getActive()) {
                bypass = false;
            }
        }
        return bypass;
    }


}

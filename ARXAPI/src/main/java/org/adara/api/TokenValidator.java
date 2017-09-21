package org.adara.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

import org.json.JSONObject;
/*
 * author kbatharia
 */
public class TokenValidator {

	private PrintWriter out;
	private static final Logger logger = Logger.getLogger(TokenValidator.class.getName());
	private static TokenValidator tokenValidation;
	private String LoggedInUser;

	private TokenValidator() {

	}

	public static TokenValidator getInstance() {

		if (tokenValidation == null) {

			tokenValidation = new TokenValidator();
		}
		return tokenValidation;

	}

	public JSONObject authenticateWithOpenAm(String token) throws IOException {
		logger.info(" start authenticateWithOpenAm() method with token :" + token);

		final Properties properties = new Properties();
		try (final InputStream stream = this.getClass().getResourceAsStream("/ARX.properties")) {
			properties.load(stream);

		} catch (Exception Ex) {
			out.println("problem reading openAM");
			logger.warning("problem reading  openAM details from ARX properties");
		}

		String openAmServerIp = properties.getProperty("openam.server.ipaddress");
		String openAmServerDnsName = properties.getProperty("openam.server.dnsname");
		String openAmServerPort = properties.getProperty("openam.server.port");
		URL url = new URL("http://" + openAmServerDnsName + ":" + openAmServerPort + "/openam/json/sessions/" + token
				+ "?_action=validate");
		logger.info("    OpenAM URL is formed inout from ARX.properties     :     " + url);
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		// httpCon.setRequestProperty("X-OpenAM-Username", userName);
		// httpCon.setRequestProperty("X-OpenAM-Password", password);
		httpCon.setRequestMethod("POST");
		httpCon.setDoOutput(true);
		BufferedReader in = null;
		StringBuffer OpenAMresponse = null;
		try {
			in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
			OpenAMresponse = new StringBuffer();
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				OpenAMresponse.append(inputLine);
			}
		} catch (Exception Ex) {
			logger.warning("Connection with openam is not formed :   " + url);
			out.println("Connection with openam is not formed:  "+ "http://" + openAmServerDnsName + ":" + openAmServerPort );
			in.close();
		}

		JSONObject json = new JSONObject(OpenAMresponse.toString());
		logger.info(" End  authenticateWithOpenAm() method with JSON retrieved from OpenAM :" + json);
		return json;

	}

	public boolean isValid(String token) throws IOException {
		logger.info(" start   isValid() method with token  passed  for  OpenAM :" + token);
		if (token != null) {
			JSONObject jsonfromOpenAM = getInstance().authenticateWithOpenAm(token);
			if (jsonfromOpenAM.getBoolean("valid"))
				setLoggedInUserFromToken(jsonfromOpenAM.getString("uid"));

			logger.info(" End   isValid() method with boolean value if token is valid :"
					+ jsonfromOpenAM.getBoolean("valid"));
			return jsonfromOpenAM.getBoolean("valid");
		} else {
			logger.warning(" token  passed  for  OpenAM is null :" + token);
			return false;
		}

	}

	public String getLoggedInUserFromToken() {
		logger.info(" start/end  getLoggedInUserFromToken():" + LoggedInUser);
		return this.LoggedInUser;

	}

	private void setLoggedInUserFromToken(String user) {
		logger.info(" start/end  setLoggedInUserFromToken():" + user);
		this.LoggedInUser = user;

	}

	public void setPrintWriter(PrintWriter printWriter) {

		this.out = printWriter;

	}

}

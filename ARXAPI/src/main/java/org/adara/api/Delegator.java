package org.adara.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;
/*
 * author kbatharia
 */
public class Delegator {
	private static final Logger logger = Logger.getLogger(Delegator.class.getName());
	private  static Delegator delegator;
	private Delegator() {
		
	}
	public  boolean validateDelegator(String loggedinUser, String effective_user) {
		final Properties properties = new Properties();
		try (final InputStream stream = this.getClass().getResourceAsStream("/ARX.properties")) {
			properties.load(stream);

		} catch (Exception Ex) {
			logger.warning("problem reading  XidentitityHub details from ARX properties");
			//out.println("problem reading  XidentitityHub details from ARX properties");
		}

		String xidentityhubDnsName = properties.getProperty("xidentityhub.server.ipaddress");
		String xidentityhubPort = properties.getProperty("xidentityhub.server.port");
		String strUrl = "http://" + xidentityhubDnsName + ":" + xidentityhubPort + "/delegators";
		logger.info("----loggenuser---: " + loggedinUser + "----effective_user: " + effective_user);
		logger.info("---xidentityhub server url---:  " + strUrl);
		URL url;
		HttpURLConnection httpCon = null;
		try {
			url = new URL(strUrl);
			httpCon = (HttpURLConnection) url.openConnection();
			// ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			// nameValuePairs.add(new BasicNameValuePair("yourReqVar", Value);
			httpCon.setRequestProperty("username", effective_user);
			// httpCon.setRequestProperty("X-OpenAM-Password", password);
			httpCon.setRequestMethod("POST");
			httpCon.setDoOutput(true);
			httpCon.setDoInput(true);

			String input = "username=" + loggedinUser;
			OutputStream os = httpCon.getOutputStream();
			os.write(input.getBytes("UTF-8"));
			os.flush();

		} catch (MalformedURLException e) {
			logger.warning("MalformedURL for XIdentitityHub  :" + strUrl);
		} catch (IOException e) {
			//out.println("connectivity issue with XIdentitityHub URL :" + strUrl);
			logger.warning("connectivity issue with XIdentitityHub URL :" + strUrl);
		}

		BufferedReader in = null;
		StringBuffer XIresponse = null;
		try {
			in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
			XIresponse = new StringBuffer();
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				XIresponse.append(inputLine);
			}
		} catch (Exception Ex) {
			logger.warning("Improper response from XIdentityHub " + Ex);

			try {
				in.close();
			} catch (Exception ex) {
				logger.warning("unable to close input stream XIdentityHub " + ex);
			}
		}

		logger.info("XIresponse:" + XIresponse.toString());

		String[] delgatorList = XIresponse.toString().split(",");

		for (int i = 0; i < delgatorList.length; i++) {

			if (effective_user.equals(delgatorList[i].replaceAll("[^a-zA-Z0-9]", ""))) {
				logger.info(" Is Loggedin user - " + loggedinUser
						+ " allowed to mask on behalf of effective user - " + effective_user+ ":  "+ "true"
						);
				return true;
			}

		}
		logger.info(" Is Loggedin user - " + loggedinUser
				+ " allowed to mask on behalf of effective user - " + effective_user+ ":  "+ "false"
				);
		return false;

	}
public static Delegator  getDelegator() {
	
	if(delegator==null) {
		delegator=new  Delegator();
	}return delegator;
}
}

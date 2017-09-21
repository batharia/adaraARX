package org.adara.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.AttributeType.Hierarchy.DefaultHierarchy;
import org.deidentifier.arx.Data.DefaultData;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;
import org.json.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import com.couchbase.client.java.document.JsonDocument;
/*
 * author kbatharia
 */

/**
 * Servlet implementation class MaskJson
 */
@WebServlet("/user/UnMaskingAPI")
public class UnMaskingAPI extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private HttpServletResponse response;
	private HttpServletRequest request;
	private static PrintWriter out;
	private CustomPrintWriter printWriter;
	private DefaultData data;
	private List<String> attributeNamelist;
	private List<String> attributevaulelist;
	private List<String> maskVaulelist;
	private List<DefaultHierarchy> defaultHierarchylist;
	private String maskLevel;
	InputProcessor IBean;
	private CouchBaseExecutor couchBaseExecutor;
	private static final Logger logger = Logger.getLogger(UnMaskingAPI.class.getName());

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public UnMaskingAPI() {

		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.getWriter().append("GET  is not supported , Please try POST	 ");

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("application/json");

		this.request = request;
		this.response = response;
		logger.info("start of  doPost() method with request : " + request);

		initilize();

		IBean = new InputProcessor(request);

		if (TokenValidator.getInstance().isValid(IBean.getToken())) {
			IBean.setLoggedinUser(TokenValidator.getInstance().getLoggedInUserFromToken());

			logger.info("----Token is vaildated and retrieved loggedin user is : " + IBean.getLoggedinUser());

			unmaskingWithURLInput();

			unmaskingWithBucketInput();

		}

		else {
			out.println("'token' is invalid ");
			logger.warning("'token' is invalid ");
			out.close();
		}

		logger.info("End of doPost() method with request : " + request);

	}

	private void unmaskingWithBucketInput() {

		logger.info("start  maskingWithBucketInput() with Bucket  :" + IBean.getBucketName());
		// jDcoument = null;
		if (IBean.getBucketName() != null) {
			paramterCheckBeforeProcessing();

			JsonDocument jDcoument = null;
			try {
				jDcoument = getDocumentsFromCouchbase();
			} catch (InvalidPasswordException e) {
				out.println("InvalidPassword: " + IBean.getBucketPassword());
				logger.warning("InvalidPassword: " + IBean.getBucketPassword());
			}
			if (jDcoument != null) {
				// when no effective user is passed , simply mask
				if (!effectiveUserCheck()) {

					arxinput(jDcoument);
					dounmask();
				}

				else {
					logger.info("Found Effective user for delegation flow: " + IBean.getEffective_user());
					// we have effective user , need to validate first
					boolean isDelgationListContainsLoggedinUser = Delegator.getDelegator()
							.validateDelegator(IBean.getLoggedinUser(), IBean.getEffective_user());

					if (isDelgationListContainsLoggedinUser) {
						arxinput(jDcoument);
						dounmask();
					} else
						out.println(IBean.getEffective_user() + " is not valid effective user.");
				}

			} else {

				logger.warning("Retrieved JSONdocument from couchbase is null " + jDcoument);
				out.println("Retrieved JSONdocument from couchbase is null " + jDcoument);

			}

		}
		logger.info("End  maskingWithBucketInput() with Document ID   :" + IBean.getDocID());

	}

	private void unmaskingWithURLInput() {

		logger.info("start  maskingWithURLInput() with URL :" + IBean.getDburl());

		if (IBean.getDburl() != null) {

			JSONObject jObject = couchBaseExecutor.couchbaseDBProcessingWithoutPassword(IBean.getDburl());

			if (jObject != null) {

				// if no effective user , do masking directly
				if (!effectiveUserCheck()) {
					logger.info("No effectiveUser in  maskingWithURLInput() :" + !effectiveUserCheck());
					arxinput(jObject);
					dounmask();
				}
				// validate that effective user prior to masking
				else {

					boolean isDelgationListContainsLoggedinUser;
					logger.info("Found Effective user for delegation flow: " + IBean.getEffective_user());

					isDelgationListContainsLoggedinUser = Delegator.getDelegator()
							.validateDelegator(IBean.getLoggedinUser(), IBean.getEffective_user());
					logger.info(" Is Loggedin user - " + IBean.getLoggedinUser()
							+ " allowed to mask on behalf of effective user - " + IBean.getEffective_user() + ":  "
							+ isDelgationListContainsLoggedinUser);
					if (isDelgationListContainsLoggedinUser) {
						arxinput(jObject);
						dounmask();
					} else {
						out.println("Invalid Effective user: " + IBean.getEffective_user());
					}
				}
			} else {
				logger.warning("Retrieved JSONdocument from Couchbase is : " + jObject);
				out.println("Retrieved JSONdocument from Couchbase is  : " + jObject);
			}

		}
		logger.info("End maskingWithURLInput()  ");

	}

	private boolean effectiveUserCheck() {
		boolean isEffectiveUser = false;

		if (IBean.getEffective_user() != null) {
			isEffectiveUser = true;
		}
		return isEffectiveUser;
	}

	private JsonDocument getDocumentsFromCouchbase() throws InvalidPasswordException {
		JsonDocument jDcoument = null;
		try {
			// For specific bucket
			if (IBean.getBucketPassword() != null) {

				jDcoument = couchBaseExecutor.getFromSpecificBucket(IBean.getBucketName(), IBean.getBucketPassword(),
						IBean.getDocID());
			}
			// for specific bucket without password
			else {

				jDcoument = couchBaseExecutor.getFromSpecificBucket(IBean.getBucketName(), "", IBean.getDocID());
			}
			// From default docuemnt with only document id as input.
			if (IBean.isDefaultBucket()) {

				jDcoument = couchBaseExecutor.getFromDefaultBucket(IBean.getDocID());
			}
		} catch (InvalidPasswordException Ex) {
			out.println("Invalid BucketName/BucketPassword");
		} catch (Exception Ex) {
			out.println("Invalid Bucket Details : Exception is --" + Ex);
		}
		return jDcoument;

	}

	private void arxinput(JSONObject jObject) {

		// convert json to list
		if (jObject != null) {
			convertJsonToList(jObject);
		}

		else {
			out.println("Please check  if  Couch base server  is up and running and requested docuement exists ");
			logger.info(
					"Please check  if  Couch base server  is up and running and requested docuement exists , since retrieved jSONObject is : "
							+ jObject);

		}
	}

	private void arxinput(JsonDocument jDocument) {

		// convert json to list
		if (jDocument != null) {
			convertJsonToList(jDocument);
		}

		else {
			out.println("Please check  if  Couch base server  is up and running and requested docuement exists ");
			logger.info(
					"Please check  if  Couch base server  is up and running and requested docuement exists , since retrieved jSONObject is : "
							+ jDocument);

		}
	}

	private void paramterCheckBeforeProcessing() {
		if (IBean.getToken() == null) {
			out.println("'token' is null or not passed");
			logger.warning("'token' is null or not passed");
			out.close();
		}

		else if ((IBean.getBucketName() == null) && !IBean.isDefaultBucket()) {
			out.println("'bucketName' is null or not passed");
			logger.warning("'bucketName' is null or not passed");
			out.close();
		} else if ((IBean.getDocID() == null)) {
			out.println("'docID' is null or not passed");
			logger.warning("'docID' is null or not passed");
			out.close();
		} else if (IBean.getMaskLevel() == null) {
			/*
			 * out.println("'mask level' is null or not passed");
			 * logger.warning("'mask level' is null or not passed"); out.close();
			 */}

	}

	private void initilize() {

		attributeNamelist = new ArrayList<String>();
		attributevaulelist = new ArrayList<String>();
		maskVaulelist = new ArrayList<String>();
		defaultHierarchylist = new ArrayList<DefaultHierarchy>();
		couchBaseExecutor = new CouchBaseExecutor();
		printWriter = new CustomPrintWriter(response);
		try {
			out = printWriter.getPrintWriter();
			TokenValidator.getInstance().setPrintWriter(out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public JSONObject authenticateWithOpenAm(String token) throws IOException {
		// logger.info(String.format("Authenticating user '%s' with OpenAm", userName));
		// vaultService.setLoggedInUserName(userName);

		// Resource r = new ClassPathResource("/vault.properties");
		// Properties props = PropertiesLoaderUtils.loadProperties(r);
		// boolean authTrue = false;

		final Properties properties = new Properties();
		try (final InputStream stream = this.getClass().getResourceAsStream("/ARX.properties")) {
			properties.load(stream);

		} catch (Exception Ex) {
			out.println("problem reading openAM");
		}

		String openAmServerIp = properties.getProperty("openam.server.ipaddress");
		String openAmServerDnsName = properties.getProperty("openam.server.dnsname");
		String openAmServerPort = properties.getProperty("openam.server.port");
		URL url = new URL("http://" + openAmServerDnsName + ":" + openAmServerPort + "/openam/json/sessions/" + token
				+ "?_action=validate");
		// curl --request POST --header "Content-Type: application/json"
		// http://localhost:8081/openam/json/sessions/AQIC5wM2LY4SfcxkhWhhsZQeG4tWMB_idKAZR3GDl59EZmI.*AAJTSQACMDEAAlNLABM2OTg5NjQ4ODk0ODYyNzE2ODAxAAJTMQAA*?_action=validate

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
			out.println("Connection with openam is not formed");
			in.close();
		}
		// out.println("openAMREsponse:-"+OpenAMresponse);
		// logger.info("response:" + response.toString());

		JSONObject json = new JSONObject(OpenAMresponse.toString());
		// authTrue=json.getBoolean("valid");
		// out.println("------------------valid-:::"+json);
		// out.println(json.getBoolean("valid"));

		return json;

	}

	private void dounmask() {
		// Create Data for masking
		data = Data.create();
		data.add(attributeNamelist.toArray(new String[attributeNamelist.size()]));
		data.add(attributevaulelist.toArray(new String[attributevaulelist.size()]));

		// create Hierarchy dynamically
		createHierarchy();

		// Create an instance of the anonymizer
		ARXAnonymizer anonymizer = new ARXAnonymizer();
		ARXConfiguration config = ARXConfiguration.create();
		config.addPrivacyModel(new KAnonymity(1));
		config.setMaxOutliers(0d);
		config.setQualityModel(Metric.createHeightMetric());

		// Now anonymize
		ARXResult result = null;
		try {
			result = anonymizer.anonymize(data, config);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// if (request.getParameter("unmask") != null &&
		// request.getParameter("unmask").equals("true")) {
		print(data.getHandle().iterator());
		// }

		// Process results
		/*
		 * out.println("                                                            ");
		 * out.println(" ----------- Data are transformed or Masked Now-------------:");
		 * out.println("                                                             ");
		 *//*
			 * else { print(result.getOutput(false).iterator());
			 * out.println("                                                             ");
			 * out.println("                                                             ");
			 */
		out.close();
	}

	private JSONObject couchDBProcessing(String couchDBurl) {

		URL url;
		JSONObject jsonObject = null;
		try {
			url = new URL(couchDBurl);
			URLConnection conn = url.openConnection();
			InputStream is = conn.getInputStream();
			// File targetFile = new File("targetFile.txt");
			// OutputStream outStream = new FileOutputStream(targetFile);

			BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			StringBuilder responseStrBuilder = new StringBuilder();

			String inputStr;
			while ((inputStr = streamReader.readLine()) != null)
				responseStrBuilder.append(inputStr);

			jsonObject = new JSONObject(responseStrBuilder.toString());
			// out.println(jsonObject.toString());

			// returns the json object
			return jsonObject;

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonObject;

	}

	private void createHierarchy() {
		Iterator<String> attributeNameItr = attributeNamelist.iterator();

		Iterator<String> attributeValueItr = attributevaulelist.iterator();

		// Iterator<String> maskingItr = maskVaulelist.iterator();

		for (int i = 0; i < attributeNamelist.size(); i++) {
			DefaultHierarchy def = Hierarchy.create();
			defaultHierarchylist.add(def);
		}

		Iterator<DefaultHierarchy> itr = defaultHierarchylist.iterator();

		Iterator<String> maskItr1 = maskVaulelist.iterator();

		while (attributeNameItr.hasNext()) {
			DefaultHierarchy defaultHierrarchy = itr.next();
			String name = attributeNameItr.next();
			String value = attributeValueItr.next();
			String maskingValue = maskItr1.next();
			int maskingLevel = 0;
			try {
				maskingLevel = Integer.parseInt(maskingValue);
			} catch (Exception e) {
				out.println("Invalid masking levels input");
				out.close();
			}
			int absMaskinglevel = Math.abs(maskingLevel);
			// out.println(maskingLevel);
			if (absMaskinglevel <= value.length()) {
				if (maskingLevel > 0)
					defaultHierrarchy.add(value, value.substring(0, value.length() - maskingLevel)
							+ value.substring(value.length() - maskingLevel, value.length()).replaceAll("(?s).", "*"));
				else

					defaultHierrarchy.add(value, value.substring(0, absMaskinglevel).replaceAll("(?s).", "*")
							+ value.substring(absMaskinglevel, value.length()));

			} else {
				defaultHierrarchy.add(value, value.substring(0, value.length()).replaceAll("(?s).", "*"));

			}
			data.getDefinition().setAttributeType(name, defaultHierrarchy);
			data.getDefinition().setMinimumGeneralization(name, 1);
			data.getDefinition().setMaximumGeneralization(name, 1);
		}

	}

	private void convertJsonToList(JSONObject jsonobject) {
		JSONObject outerJson = jsonobject.getJSONObject("json");

		TreeSet<String> set1 = new TreeSet<String>(outerJson.toMap().keySet());

		Iterator<String> iterator = set1.iterator();
		String[] filterAttributes;
		if (IBean.getFilterAttributes() != null) {
			filterAttributes = IBean.getFilterAttributes().split(",");
			for (int i = 0; i < filterAttributes.length; i++) {
				String str1 = filterAttributes[i];
				attributeNamelist.add(str1);
				String value;
				try {
					value = outerJson.getString(str1);
					if (value != null)
						attributevaulelist.add(outerJson.getString(str1));
					else {

						out.println("'Invalid Attribute :  " + str1 + "  for unmasking ");
					}
				} catch (Exception Ex) {
					out.println("'Invalid Attribute :  " + str1 + "  for unmasking ");
				}
			}

		} else {
			while (iterator.hasNext()) {
				String str = iterator.next();
				attributeNamelist.add(str);
				if (outerJson.getString(str) != null)
					attributevaulelist.add(outerJson.getString(str));
				else
					out.println("'Null value for  Attribute :  " + str + "  for unmasking ");
			}
		}

		/*
		 * String[] maskingLevelArray = IBean.getMaskLevel().split(",");
		 * 
		 * for (int i = 0; i < maskingLevelArray.length; i++) { String str1 =
		 * maskingLevelArray[i]; maskVaulelist.add(str1); }
		 * 
		 * while (attributeNamelist.size() > maskVaulelist.size()) {
		 * maskVaulelist.add("1000"); }
		 */

		String[] maskingLevelArray = { "0" };

		for (int i = 0; i < maskingLevelArray.length; i++) {
			String str1 = maskingLevelArray[i];
			maskVaulelist.add(str1);
		}

		while (attributeNamelist.size() > maskVaulelist.size()) {
			maskVaulelist.add("0");
		}
	}

	private void convertJsonToList(JsonDocument jdocument) {

		TreeSet<String> set1 = new TreeSet<String>(jdocument.content().getNames());
		String[] filterAttributes;
		Iterator<String> iterator = set1.iterator();
		if (IBean.getFilterAttributes() != null) {
			filterAttributes = IBean.getFilterAttributes().split(",");
			for (int i = 0; i < filterAttributes.length; i++) {
				String str1 = filterAttributes[i];
				attributeNamelist.add(str1);
				if (jdocument.content().getString(str1) != null)
					attributevaulelist.add(jdocument.content().getString(str1));
				else {

					out.println("Invalid Attribute :  '" + str1 + "'  for unmasking ");
				}
			}

		} else {
			while (iterator.hasNext()) {
				String str = iterator.next();
				attributeNamelist.add(str);
				if (jdocument.content().getString(str) != null)
					attributevaulelist.add(jdocument.content().getString(str));
				else {
					attributevaulelist.add("                          ");
					out.println("'Null value for  Attribute :  " + str + "  for unmasking ");
					;
				}
			}
		}

		/*
		 * String[] maskingLevelArray = IBean.getMaskLevel().split(",");
		 * 
		 * for (int i = 0; i < maskingLevelArray.length; i++) { String str1 =
		 * maskingLevelArray[i]; maskVaulelist.add(str1); }
		 * 
		 * while (attributeNamelist.size() > maskVaulelist.size()) {
		 * maskVaulelist.add("1000"); }
		 */

		String[] maskingLevelArray = { "0" };

		for (int i = 0; i < maskingLevelArray.length; i++) {
			String str1 = maskingLevelArray[i];
			maskVaulelist.add(str1);
		}

		while (attributeNamelist.size() > maskVaulelist.size()) {
			maskVaulelist.add("0");
		}

	}

	private List<String> getAttributeList() {

		return attributeNamelist;

	}

	private List<String> getAttributeValueList() {

		return attributevaulelist;

	}

	private List<String> getMaskingLevelList() {

		return maskVaulelist;

	}

	// To be implemented later based upon requirement
	private AttributeType getMaskingType(String maskingType) {

		if (maskingType == null) {

			return AttributeType.IDENTIFYING_ATTRIBUTE;
		}
		if (maskingType.equalsIgnoreCase("CompleteMasking")) {

			return AttributeType.IDENTIFYING_ATTRIBUTE;
		} else if (maskingType.equalsIgnoreCase("NoMasking")) {

			return AttributeType.INSENSITIVE_ATTRIBUTE;
		} else if (maskingType.equalsIgnoreCase("FirstHalfMasking")) {

			return AttributeType.INSENSITIVE_ATTRIBUTE;
		}

		else if (maskingType.equalsIgnoreCase("LastHalfMasking")) {

			return AttributeType.INSENSITIVE_ATTRIBUTE;
		} else if (maskingType.equalsIgnoreCase("RandomMasking")) {

			return AttributeType.INSENSITIVE_ATTRIBUTE;
		} else if (maskingType.equalsIgnoreCase("AlternativeMasking")) {

			return AttributeType.INSENSITIVE_ATTRIBUTE;
		}
		return AttributeType.IDENTIFYING_ATTRIBUTE;

	}

	public void setPrintWriter(PrintWriter out) {

		try {
			out = response.getWriter();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.out = out;
	}

	public static PrintWriter getPrintWriter() {
		return out;
	}

	// To print result.
	private void print(Iterator<String[]> iterator) {
		while (iterator.hasNext()) {
			out.print("           ");
			out.println(Arrays.toString(iterator.next()));
			out.println("                                                             ");
		}
	}

}

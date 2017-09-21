package org.adara.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream.PutField;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.InvalidPasswordException;

/*
 * author kbatharia
 */
public class CouchBaseExecutor {
	
	private static final Logger logger = Logger.getLogger(CouchBaseExecutor.class.getName());
	public static String couchbaseBucketName;
	public static String couchbaseBucketPassword;
	public static String couchbaseDocumentName;
	public CouchBaseExecutor() {
		final Properties properties = new Properties();
		

		try {
			final InputStream stream = this.getClass().getResourceAsStream("/ARX.properties") ;
			properties.load(stream);
			couchbaseBucketName =properties.getProperty("couchbase.server.bucket.name");
			couchbaseBucketPassword =properties.getProperty("couchbase.server.bucket.password");
		     couchbaseDocumentName=properties.getProperty("couchbase.server.bucket.document");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logger.info("couchbase server address:"+ properties.getProperty("couchbase.server.ipaddress"));
	}
	public static void main(String args[]) throws Exception{
	
		CouchBaseExecutor c = new CouchBaseExecutor();
		
		System.out.println("classpath"+System.getProperty("java.classpath"));
		
		JsonDocument jd=c.getFromSpecificBucket("MaskingBucket1", "test123", "10");
		//jd.content().getNames();
		TreeSet<String> set1 = new TreeSet(jd.content().getNames());
		System.out.println(set1);
		//System.out.println(c.getFromDefaultBucket("9"));
	}


	public void putInDefaultBucket(String loggedInUser, JsonObject secret) {
		logger.info("Inserting new document in default bucket" + secret.toString());
		Cluster cluster = BucketCluster.getInstance();
		logger.info("cluster : "+cluster);
		Bucket bucket = cluster.openBucket();
		logger.info("bucket : "+bucket);
		JsonDocument doc = JsonDocument.create(loggedInUser, secret);
		logger.info("doc :"+ doc.toString());
		bucket.upsert(doc);
		logger.info("document inserted");
	
	}

	public void putInSpecificsBucket(String bucketName, String bucketPassword, String loggedInUser,
			JsonObject secret) {
		Cluster cluster = BucketCluster.getInstance();
		Bucket bucket = cluster.openBucket(bucketName, bucketPassword);
		JsonDocument doc = JsonDocument.create(loggedInUser, secret);
		bucket.upsert(doc);
	}

	public JsonDocument getFromDefaultBucket(String loggedInUser) {
		logger.info("Retrieving the document in default bucket for " + loggedInUser);
		Cluster cluster = BucketCluster.getInstance();
		Bucket bucket = cluster.openBucket();
		return bucket.get(loggedInUser);
	}

	public JsonDocument getFromSpecificBucket(String bucketName, String bucketPassword, String loggedInUser) throws Exception {
		logger.info("Retrieving the document in  bucket for " + bucketName);
		logger.info("Retrieving the document in  bucket with password" + bucketPassword);
		logger.info("Retrieving the document in  for document " + loggedInUser);
		Cluster cluster = BucketCluster.getInstance();
		logger.info("Retrieving cluster " + cluster);
		Bucket bucket = cluster.openBucket(bucketName, bucketPassword);
		logger.info("Retrieving bucket " + bucket);
		return bucket.get(loggedInUser);
	}
	
	public JSONObject couchbaseDBProcessingWithoutPassword(String couchDBurl) {

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


}

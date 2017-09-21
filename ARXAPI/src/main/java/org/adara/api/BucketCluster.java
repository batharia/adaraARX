package org.adara.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;

/*
 * author kbatharia
 */
public class BucketCluster {

	private static Cluster cluster;
	private final static Logger logger = Logger.getLogger(BucketCluster.class.getName());
    private static BucketCluster bucketCluster=new BucketCluster();
	// private constructor to avoid client applications to use constructor
	private BucketCluster() {}

	public static Cluster getInstance() {
	
		final Properties properties = new Properties();
	
		try (final InputStream stream = bucketCluster.getClass().getResourceAsStream("/ARX.properties")) {
			properties.load(stream);
			logger.info("\""+properties.getProperty("couchbase.server.ipaddress")+"\"");
			cluster = CouchbaseCluster.create(properties.getProperty("couchbase.server.ipaddress"));

		} catch (Exception Ex) {
			logger.warning("problem loading  ARX properties "+Ex);
		}
		 
	  return cluster;
	}

}
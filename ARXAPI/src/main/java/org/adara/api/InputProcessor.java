package org.adara.api;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
/*
 * author kbatharia
 */
public class InputProcessor {

	private String maskLevel;
	private String token;
	private String bucketName;
	private String dburl;
	private String bucketPassword;
	private String docID;
	private String effective_user;
	private String eff_user_realm;
	private String loggedinUser;
	private String filterAttributes;
	private boolean  isDefaultBucket;

	private HttpServletRequest request;
	private static final Logger logger = Logger.getLogger(InputProcessor.class.getName());

	public InputProcessor(HttpServletRequest request) {
		this.request = request;
		init();
	}

	private void init() {

		setDburl(request.getParameter("dburl"));
		logger.info("dburl: " + getDburl());
		if(request.getParameter("isDefaultBucket")==null) {
			setDefaultBucket(false);
		}
		else if  (request.getParameter("isDefaultBucket").equals("true")) {
			setDefaultBucket(true);
		}
		
		else {
			setDefaultBucket(false);
		}
			
		logger.info("isDefaultBucket: " + isDefaultBucket());
		
		setMaskLevel(request.getParameter("maskinglevel"));
		logger.info("maskLevel: " + getMaskLevel());
		
		setToken(request.getHeader("X-OpenAM-Token"));
		logger.info("maskLevel: " + getToken());
		
		setEffective_user(request.getHeader("X-Effective-User"));
		logger.info("effective_user: " + getEffective_user());

		setDocID(request.getParameter("docID"));
		logger.info("docID: " + getDocID());
		
		setBucketName(request.getParameter("bucketName"));
		logger.info("bucketName: " + getBucketName());
		
		setBucketPassword(request.getParameter("bucketPassword"));
		logger.info("bucketPassword: " + getBucketPassword());
		
		setFilterAttributes(request.getParameter("filterAttributes"));
		logger.info("FilterAttributes: " + getFilterAttributes());
		// To be implemented based upon requirement.
		//setEff_user_realm(request.getHeader("X-Effective-User-Realm"));
		//logger.info("bucketPassword: " + getEff_user_realm());
		
	}

	public String getMaskLevel() {
		return maskLevel;
	}

	public void setMaskLevel(String maskLevel) {
		this.maskLevel = maskLevel;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getDburl() {
		return dburl;
	}

	public void setDburl(String dburl) {
		this.dburl = dburl;
	}

	public String getBucketPassword() {
		return bucketPassword;
	}

	public void setBucketPassword(String bucketPassword) {
		this.bucketPassword = bucketPassword;
	}

	public String getDocID() {
		return docID;
	}

	public void setDocID(String docID) {
		this.docID = docID;
	}

	public String getEffective_user() {
		return effective_user;
	}

	public void setEffective_user(String effective_user) {
		this.effective_user = effective_user;
	}

	public String getEff_user_realm() {
		return eff_user_realm;
	}

	public void setEff_user_realm(String eff_user_realm) {
		this.eff_user_realm = eff_user_realm;
	}

	public String getLoggedinUser() {
		return loggedinUser;
	}

	public void setLoggedinUser(String loggedinUser) {
		this.loggedinUser = loggedinUser;
	}

	public String getFilterAttributes() {
		return filterAttributes;
	}

	public void setFilterAttributes(String filterAttributes) {
		this.filterAttributes = filterAttributes;
	}
	public boolean isDefaultBucket() {
		return isDefaultBucket;
	}

	public void setDefaultBucket(boolean isDefaultBucket) {
		this.isDefaultBucket = isDefaultBucket;
	}

}

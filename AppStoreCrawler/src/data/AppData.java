package data;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AppData {
	private ConcurrentLinkedQueue<Comment> commentList=null;
	private ConcurrentLinkedQueue<Reviewer> reviewList=null;
	// table columns
	private String appId=null;
	private String appName=null;
	private String developerId=null;
	private String developerName=null;
	private String developerCompany=null;
	private float averageRating=-1;
	private int totalRaters=-1;
	private String ratedSummary=null;
	private int ratingNum[];//5 star rating
	private String currentVersion=null;
	private String requiredAndroidVersion=null;
	private String category=null;
	private String subCategory=null;
	private String price=null;//0 is free
	private String currency=null;
	private String thumbNail=null;
	private String storeLocation=null;//the location of the stores used for
	private String crawlerPoint=null; //the entry point of the crawler, such as the category. top_free, top_paid
	private String description=null;
	private String latestModified=null;
	private String latestAction=null;
	private String installs=null;
	private String contentRated=null;
	private String language=null;
	private String requirements=null;
	private String url=null;
	private String latestReviewId=null;//the latest Review ID
	private String size=null;
	private String permission=null;
	private String contactPhone=null;
	private String contactEmail=null;
	private String contactWebsite=null;
	private String promotedText=null;
	private String promotedScreenshot=null;
	private String promotedVideo=null;
	private String recentChanges=null;
	private int rank=0;
	//for large scale only
	private float ccVal=-2;

	public AppData(String appId){
		this.commentList=new ConcurrentLinkedQueue<Comment>();
		this.reviewList=new ConcurrentLinkedQueue<Reviewer>();
		this.ratingNum=new int[5];
		this.appId=appId;
	}

	public ConcurrentLinkedQueue<Comment> getCommentList() {
		return commentList;
	}

	public void addComment(Comment comment){
		this.commentList.add(comment);
	}
    public void addComments(Collection comments){
        this.commentList.addAll(comments);
    }
	public String getAppId() {
		return appId;
	}

	public float getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(float averageRating) {
		this.averageRating = averageRating;
	}

	public int getTotalRaters() {
		return totalRaters;
	}

	public void setTotalRaters(int totalRaters) {
		this.totalRaters = totalRaters;
	}

	public int getRatingNum(int index) {
		if(index>5||index<1){
			return -1;
		}
		return this.ratingNum[index-1];
	}

	public void setRatingNum(int index,int ratingNum) {
		if(index>5||index<1){
			return;
		}
        if(this.ratingNum==null){
            this.ratingNum=new int[5];
        }
		this.ratingNum[index-1] = ratingNum;
	}
    public boolean isRatingNumSet(){
        return this.getRatingNum()!=null;
    }
	public String getCurrentVersion() {
		return currentVersion;
	}

	public void setCurrentVersion(String currentVersion) {
		this.currentVersion = currentVersion;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public String getRequiredAndroidVersion() {
		return requiredAndroidVersion;
	}

	public void setRequiredAndroidVersion(String requiredAndroidVersion) {
		this.requiredAndroidVersion = requiredAndroidVersion;
	}

	public String getThumbNail() {
		return thumbNail;
	}

	public void setThumbNail(String thumbNail) {
		this.thumbNail = thumbNail;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public int[] getRatingNum() {
        if(ratingNum==null){
            this.ratingNum=new int[5];
        }
		return ratingNum;
	}

	public void setRatingNum(int[] ratingNum) {
		this.ratingNum = ratingNum;
	}

	public String getStoreLocation() {
		return storeLocation;
	}

	public void setStoreLocation(String storeLocation) {
		this.storeLocation = storeLocation;
	}

	public String getCrawlerPoint() {
		return crawlerPoint;
	}

	public void setCrawlerPoint(String crawlerPoint) {
		this.crawlerPoint = crawlerPoint;
	}

	public String getLatestReviewId() {
		return latestReviewId;
	}

	public void setLatestReviewId(String latestReviewId) {
		this.latestReviewId=latestReviewId;
	}
	public ConcurrentLinkedQueue<Reviewer> getReviewList() {
		return reviewList;
	}

	public String getDeveloperId() {
		return developerId;
	}

	public void setDeveloperId(String developerId) {
		this.developerId = developerId;
	}

	public String getDeveloperName() {
		return developerName;
	}

	public void setDeveloperName(String developerName) {
		this.developerName = developerName;
	}

	public String getDeveloperCompany() {
		return developerCompany;
	}

	public void setDeveloperCompany(String developerCompany) {
		this.developerCompany = developerCompany;
	}

	public String getRatedSummary() {
		return ratedSummary;
	}

	public void setRatedSummary(String ratedSummary) {
		this.ratedSummary = ratedSummary;
	}

	public String getSubCategory() {
		return subCategory;
	}

	public void setSubCategory(String subCategory) {
		this.subCategory = subCategory;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLatestModified() {
		return latestModified;
	}

	public void setLatestModified(String latestModified) {
		this.latestModified = latestModified;
	}

	public String getLatestAction() {
		return latestAction;
	}

	public void setLatestAction(String latestAction) {
		this.latestAction = latestAction;
	}

	public String getInstalls() {
		return installs;
	}

	public void setInstalls(String installs) {
		this.installs = installs;
	}

	public String getContentRated() {
		return contentRated;
	}

	public void setContentRated(String contentRated) {
		this.contentRated = contentRated;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getRequirements() {
		return requirements;
	}

	public void setRequirements(String requirements) {
		this.requirements = requirements;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}


	public String getPromotedText() {
		return promotedText;
	}

	public void setPromotedText(String promotedText) {
		this.promotedText = promotedText;
	}

	public String getRecentChanges() {
		return recentChanges;
	}

	public void setRecentChanges(String recentChanges) {
		this.recentChanges = recentChanges;
	}

	public void setCommentList(ConcurrentLinkedQueue<Comment> commentList) {
		this.commentList = commentList;
	}

	public void setReviewList(ConcurrentLinkedQueue<Reviewer> reviewList) {
		this.reviewList = reviewList;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public String getContactWebsite() {
		return contactWebsite;
	}

	public void setContactWebsite(String contactWebsite) {
		this.contactWebsite = contactWebsite;
	}

	public String getPromotedScreenshot() {
		return promotedScreenshot;
	}

	public void setPromotedScreenshot(String promotedScreenshot) {
		this.promotedScreenshot = promotedScreenshot;
	}

	public String getPromotedVideo() {
		return promotedVideo;
	}

	public void setPromotedVideo(String promotedVideo) {
		this.promotedVideo = promotedVideo;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public float getCcVal() {
		return ccVal;
	}

	public void setCcVal(float ccVal) {
		this.ccVal = ccVal;
	}

	@Override
	public String toString() {
		return "AppData [commentList=" + commentList.size() + ", reviewList="
				+ reviewList.size() + ", appId=" + appId + ", appName=" + appName
				+ ", developerId=" + developerId + ", developerName="
				+ developerName + ", developerCompany=" + developerCompany
				+ ", averageRating=" + averageRating + ", totalRaters="
				+ totalRaters + ", ratedSummary=" + ratedSummary
				+ ", ratingNum=" + Arrays.toString(ratingNum)
				+ ", currentVersion=" + currentVersion
				+ ", requiredAndroidVersion=" + requiredAndroidVersion
				+ ", category=" + category + ", subCategory=" + subCategory
				+ ", price=" + price + ", currency=" + currency
				+ ", thumbNail=" + thumbNail + ", storeLocation="
				+ storeLocation + ", crawlerPoint=" + crawlerPoint
				+ ", description=" + description + ", latestModified="
				+ latestModified + ", latestAction=" + latestAction
				+ ", installs=" + installs + ", contentRated=" + contentRated
				+ ", language=" + language + ", requirements=" + requirements
				+ ", url=" + url + ", latestReviewId=" + latestReviewId
				+ ", size=" + size + ", permission=" + permission
				+ ", contactPhone=" + contactPhone + ", contactEmail="
				+ contactEmail + ", contactWebsite=" + contactWebsite
				+ ", promotedText=" + promotedText + ", promotedScreenshot="
				+ promotedScreenshot + ", promotedVideo=" + promotedVideo
				+ ", recentChanges=" + recentChanges + ", rank=" + rank + "]";
	}
}

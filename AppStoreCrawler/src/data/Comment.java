package data;

import java.util.Date;

public class Comment {
	/** e.g.
	 * Nick on February 28, 2012 (Samsung Nexus S with version 3.1.1.1)
	 * Great app!
	 */
	private String reviewer=null; 		//Nick
	private String reviewId=null;		//reviewId=08248536403053554330
	private Date date=null;		  		//February 28, 2012 
	private String device=null;			//Samsung Nexus S
	private String deviceVersion=null;	//version 3.1.1.1
	private float rating=0; 			//
	private String comment=null;		//Great app!
	private String commentTitle=null;
	private float helpfulnessRatio=0;
	private int helpfulnessAgree=0;
	private int helpfulnessTotal=0;
	private String appId=null;
	private String reviewerId=null;
	public String getReviewer() {
		return reviewer;
	}
	public void setReviewer(String reviewer) {
		this.reviewer = reviewer;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getDevice() {
		return device;
	}
	public void setDevice(String device) {
		this.device = device;
	}
	public String getDeviceVersion() {
		return deviceVersion;
	}
	public void setDeviceVersion(String deviceVersion) {
		this.deviceVersion = deviceVersion;
	}
	public float getRating() {
		return rating;
	}
	public void setRating(float rating) {
		this.rating = rating;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public void setReviewId(String reviewId) {
		this.reviewId = reviewId;
	}
	public String getReviewId() {
		return reviewId;
	}
	public void setCommentTitle(String commentTitle) {
		this.commentTitle = commentTitle;
	}
	public String getCommentTitle() {
		return commentTitle;
	}
	public float getHelpfulnessRatio() {
		return helpfulnessRatio;
	}
	public void setHelpfulnessRatio(float helpfulnessRatio) {
		this.helpfulnessRatio = helpfulnessRatio;
	}
	public int getHelpfulnessAgree() {
		return helpfulnessAgree;
	}
	public void setHelpfulnessAgree(int helpfulnessAgree) {
		this.helpfulnessAgree = helpfulnessAgree;
	}
	public int getHelpfulnessTotal() {
		return helpfulnessTotal;
	}
	public void setHelpfulnessTotal(int helpfulnessTotal) {
		this.helpfulnessTotal = helpfulnessTotal;
	}
	public String getAppId() {
		return appId;
	}
	public void setAppId(String appId) {
		this.appId = appId;
	}
	public String getReviewerId() {
		return reviewerId;
	}
	public void setReviewerId(String reviewerId) {
		this.reviewerId = reviewerId;
	}
	@Override
	public String toString() {
		return "Comment [reviewer=" + reviewer + ", reviewId=" + reviewId
				+ ", date=" + date + ", device=" + device + ", deviceVersion="
				+ deviceVersion + ", rating=" + rating + ", comment=" + comment
				+ ", commentTitle=" + commentTitle + ", helpfulnessRatio="
				+ helpfulnessRatio + ", helpfulnessAgree=" + helpfulnessAgree
				+ ", helpfulnessTotal=" + helpfulnessTotal + ", appId=" + appId
				+ ", reviewerId=" + reviewerId + "]";
	}
	
}

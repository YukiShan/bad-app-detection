package data;

import java.util.Calendar;
import java.util.Date;

public class AppRatingInfo {
	private String appId=null;
	private int rating=-1;
	private int today=-1;
	private int month=-1;
	private String title=null;//The name of the app
	private String category=""; //which category like Top All
	private String storeLocation="";//which country US/China
	private String crawlerPoint="";//which paid or free or grossing
	public AppRatingInfo(){
		
	}
	public AppRatingInfo(String category,String storeLocation,String crawlerPoint,String appId,Date date,int rating,String title){
		this.category=category;
		this.storeLocation=storeLocation;
		this.crawlerPoint=crawlerPoint;
		this.appId=appId;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		month = cal.get(Calendar.MONTH)+1;
		today=cal.get(Calendar.DAY_OF_MONTH);
		this.rating=rating;
		this.title=title;
	}
	public String getAppId() {
		return appId;
	}
	public void setAppId(String appId) {
		this.appId = appId;
	}
	public int getRating() {
		return rating;
	}
	public void setRating(int rating) {
		this.rating = rating;
	}
	public int getToday() {
		return today;
	}
	public void setToday(int today) {
		this.today = today;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getStore_location() {
		return storeLocation;
	}
	public void setStore_location(String store_location) {
		this.storeLocation = store_location;
	}
	public String getCrawler_point() {
		return crawlerPoint;
	}
	public void setCrawler_point(String crawler_point) {
		this.crawlerPoint = crawler_point;
	}
	public int getMonth() {
		return month;
	}
	public void setMonth(int month) {
		this.month = month;
	}
	@Override
	public String toString() {
		return "AppRatingInfo [category=" + category + ", storeLocation="
				+ storeLocation + ", crawlerPoint=" + crawlerPoint
				+ ", rating=" + rating + ", appId=" + appId + ", today="
				+ today + ", title=" + title + "]";
	}
	
	
}

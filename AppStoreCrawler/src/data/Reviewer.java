package data;

import java.util.ArrayList;

import utility.SystemLogger;

public class Reviewer {
	private static SystemLogger log = SystemLogger.getLogger(Reviewer.class);

	private String reviewerId=null;
	//put each review ids in the array list but sorted increasing order first.
	private ArrayList<String> appIdsOrded=null;
	//connect all the ratings related to the reviewIdsOrded item to a string
	private ArrayList<String> reviewRatings=null;
	//add all the reveiw date string to a list 
	private ArrayList<String> reviewDateStrings=null;
	private ArrayList<String> developerIdList=null;
	private ArrayList<String> appVersionList=null;
	private int reviewSize=0;
	//the status of this reviewer
	private int status=0;
	//current index -1: default -2: removed
	private int curIndex=-1;
	//it is used to denote the stautus of this reviewers, good or bad
	private NodeStatus nodeStatus=NodeStatus.Unknown;
	private String storeLocation=null;
	private String appId=null;
    public Reviewer(String reviewerId,String storeLocation){
        this(reviewerId);
        this.storeLocation=storeLocation;
    }
	public Reviewer(String reviewerId){
		this.reviewerId=reviewerId;
		appIdsOrded=new ArrayList<String>();
		reviewDateStrings=new ArrayList<String>();
		reviewRatings=new ArrayList<String>();
		developerIdList=new ArrayList<String>();
		appVersionList=new ArrayList<String>();
	}
	
	public void addReviewId(String ...para){
		String appId=para[0];
		String rating=para[1];
		String dateStr=para[2];
		String developerId=para[3];
		String appVersion=para[4];
		if(para.length>4){
			appVersion=para[4];
		}
		if(appIdsOrded.size()==0){
			appIdsOrded.add(appId);
			reviewRatings.add(rating);
			reviewDateStrings.add(dateStr);
			this.developerIdList.add(developerId);
			this.appVersionList.add(appVersion);
		}else{
			appIdsOrded.add("");
			reviewDateStrings.add("");
			reviewRatings.add("");
			this.developerIdList.add("");
			this.appVersionList.add("");
			int i=appIdsOrded.size()-2;
			for( i=appIdsOrded.size()-2;i>=0;i--){
				String currentStr=appIdsOrded.get(i);
				if(compareStrings(currentStr,appId)>0){
					appIdsOrded.set(i+1, currentStr);
					reviewDateStrings.set(i+1, reviewDateStrings.get(i));
					reviewRatings.set(i+1, reviewRatings.get(i));
					this.developerIdList.set(i+1, developerIdList.get(i));
					this.appVersionList.set(i+1, this.appVersionList.get(i));
				}else{
					break;
				}
			}
			appIdsOrded.set(i+1, appId);
			reviewRatings.set(i+1, rating);
			reviewDateStrings.set(i+1, dateStr);
			this.developerIdList.set(i+1,developerId);
			this.appVersionList.set(i+1, appVersion);
		}
		this.reviewSize++;
	}
	/**
	 * @param str1
	 * @param str2
	 * @return 1: if str1>str2 0: str1=str2 -1: str1<str2
	 */
	private int compareStrings(String str1,String str2){
		if(str1.length()>str2.length()){
			return 1;
		}else if(str1.length()<str2.length()){
			return -1;
		}else{
			int i=0;
			for(i=0;i<str1.length();i++){
				if(str1.charAt(i)!=str2.charAt(i)){
					break;
				}
			}
			if(i==str1.length()){
				return 0;
			}if(str1.charAt(i)>str2.charAt(i)){
				return 1;
			}else if (str1.charAt(i)<str2.charAt(i)){
				return -1;
			}else{
				return 0;
			}
		}
		
	}
	public String getReviewerId() {
		return reviewerId;
	}

	public void setReviewerId(String reviewerId) {
		this.reviewerId = reviewerId;
	}

	public ArrayList<String> getAppIdsOrded() {
		return appIdsOrded;
	}

	public void setReviewIdsOrded(ArrayList<String> reviewIdsOrded) {
		this.appIdsOrded = reviewIdsOrded;
	}

	public ArrayList<String> getReviewDateStrings() {
		return reviewDateStrings;
	}

	public void setReviewDateStrings(ArrayList<String> reviewDateStrings) {
		this.reviewDateStrings = reviewDateStrings;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public ArrayList<String> getReviewRatings() {
		return reviewRatings;
	}

	public void setReviewRatings(ArrayList<String> reviewRatings) {
		this.reviewRatings = reviewRatings;
	}

	public int getReviewSize() {
		return reviewSize;
	}

	public void setReviewSize(int reviewSize) {
		this.reviewSize = reviewSize;
	}

	public ArrayList<String> getDeveloperIdList() {
		return developerIdList;
	}

	public void setDeveloperIdList(ArrayList<String> developerIdList) {
		this.developerIdList = developerIdList;
	}
	/**
	 * Import data from database
	 * @param appIds
	 * @param reviewRatings
	 * @param reviewDates
	 * @param developerIds
	 * @param reviewSize
	 */
	public void add(String appIds,String reviewRatings,String reviewDates,String developerIds, String appVersions,int reviewSize){
		String []strs1=appIds.split("_");
		String []strs2=reviewRatings.split("_");
		String []strs3=reviewDates.split("_");
		
		String []strs4=null;
		if(developerIds==null){
			strs4=new String[strs1.length];
		}else{
			strs4=developerIds.split("_");
		}
		String []strs5=appVersions.split("_");
		this.reviewSize=reviewSize;
		if(strs1.length!=reviewSize||strs2.length!=reviewSize
				||strs3.length!=reviewSize||strs4.length!=reviewSize||strs5.length!=reviewSize){
			return;
		}
		for(int i=0;i<reviewSize;i++){
			this.appIdsOrded.add(strs1[i]);
			this.reviewRatings.add(strs2[i]);
			this.reviewDateStrings.add(strs3[i]);
			this.developerIdList.add(strs4[i]);
			this.appVersionList.add(strs5[i]);
		}
	}
	/**
	 * Compare the current reviewer with reviewer2 and store the results into resultArray
	 * @param reviewer2
	 * @param resultArray
	 */
	public void compareReviewer(Reviewer reviewer2,ArrayList<Long> resultArray){
		//compare the app ids reviewed
		int num=0;
		int i=0;
		int j=0;
		while(i<this.reviewSize&& j<reviewer2.getReviewSize()){
			int result=this.compareStrings(this.appIdsOrded.get(i), reviewer2.getAppIdsOrded().get(j));
			if(result==0){
				num++;
				i++;
				j++;
			}else if(result>0){
				j++;
			}else{
				i++;
			}
		}
		long currentNum=resultArray.get(num-1);
		resultArray.set(num-1, currentNum+1);
	}
	public String getStoreLocation() {
		return storeLocation;
	}

	public void setStoreLocation(String storeLocation) {
		this.storeLocation = storeLocation;
	}

	public ArrayList<String> getAppVersionList() {
		return appVersionList;
	}

	public int getCurIndex() {
		return curIndex;
	}

	public void setCurIndex(int curIndex) {
		this.curIndex = curIndex;
	}

	public NodeStatus getNodeStatus() {
		return nodeStatus;
	}

	public void setNodeStatus(NodeStatus nodeStatus) {
		this.nodeStatus = nodeStatus;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	@Override
	public String toString() {
		return "Reviewer [reviewerId=" + reviewerId + ", reviewIdsOrded="
				+ appIdsOrded + ", reviewRatings=" + reviewRatings
				+ ", reviewDateStrings=" + reviewDateStrings + ", status="
				+ status + "]";
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Reviewer reviewer = (Reviewer) o;

        if (reviewerId != null ? !reviewerId.equals(reviewer.reviewerId) : reviewer.reviewerId != null) return false;
        if (storeLocation != null ? !storeLocation.equals(reviewer.storeLocation) : reviewer.storeLocation != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = reviewerId != null ? reviewerId.hashCode() : 0;
        result = 31 * result + (storeLocation != null ? storeLocation.hashCode() : 0);
        return result;
    }
}

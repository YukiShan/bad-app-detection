import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import utility.MultiDateFormat;
import utility.SystemLogger;
import data.AppData;
import data.Comment;
import data.Reviewer;

public class IndicatorsCalculator {
	private static SystemLogger log = SystemLogger.getLogger(IndicatorsCalculator.class);
	private static IndicatorsCalculator indicatorsCal=null;
	private float epsilon=0.00001f;//termination point of group status calculation
	private float alpha=LSConfig.getInst().getAlpha();//for calculating new app status
	private float thresholdBadApps=3;//how many bad app a group have rated will be confirmed to be malicious
	private float thresholdAbusedAppStatus=0.99f;
	private HashMap<String,Float> confirmedAppSet;//record the final status of an app
	public static IndicatorsCalculator getInstance(){
		if(indicatorsCal==null){
			indicatorsCal=new IndicatorsCalculator();
		}
		return indicatorsCal;
	}
	
	
	/**
	 * calculate all the indicators
	 * @param bicliques
	 */
	public void calculator(ArrayList<Biclique> bicliques, ConcurrentHashMap<String,AppData> appHM, HashMap<String,Float> confirmedAppSet, ConcurrentHashMap<String, Reviewer> reviewerMap){
		this.confirmedAppSet=confirmedAppSet;
		//sort all the bicliques, maximum first
		HeapSort hs=new HeapSort();
		for(Biclique biclique:bicliques){
			hs.insertObject(biclique.setX.size()*biclique.setY.size(), biclique);
		}
		bicliques.clear();
		Object obj=null;
		while( (obj=hs.removeRoot())!=null){
			Biclique biclique=(Biclique)obj;
			bicliques.add(biclique);
		}
		
		for(Biclique bq:bicliques){
			if(bq.appStatus==null){
				bq.appStatus=new ArrayList<Float>();
			}
			for(String appId:bq.setY){
				if(appHM.containsKey(appId)){
					AppData ad=appHM.get(appId);
					float ccVal=ad.getCcVal();
					if(ccVal==-2){
						ccVal=this.calculateVairanceCC(ad);
						ccVal=Math.abs(ccVal);//convert to its absolute value
						ad.setCcVal(ccVal);//calculate app data
					}else if(confirmedAppSet.containsKey(ad.getAppId())){
						ccVal=confirmedAppSet.get(ad.getAppId());
					}
					bq.appStatus.add(ccVal);//initialize app status to its cc value
					log.info("ccVal: "+ad.getAppId()+"-"+ccVal);
				}
			}
			boolean abnormalDistribution=this.abnormalDistribution(bq, appHM, reviewerMap);
			this.calculateStatus(bq, abnormalDistribution);

			log.info("Group status: "+bq.groupStatus);
			log.info("App status: "+bq.appStatus);
		}
	}
	private void calculateStatus(Biclique biclique,boolean abnormalDistribution){
		if(abnormalDistribution && LSConfig.getInst().getSwitchFLRHOR()){//HLR/FOR are strong indicators
			for(int i=0;i<biclique.setY.size();i++){
				this.confirmedAppSet.put(biclique.setY.get(i), 1.0f);
			}
			biclique.groupStatus=1.0f;
			return;
		}
		float appstatusAvg=0;
		for(int i=0;i<biclique.setY.size();i++){
			appstatusAvg+=biclique.appStatus.get(i);
		}
		if(biclique.setY.size()>0){
			appstatusAvg=appstatusAvg/biclique.setY.size();
		}
		float groupStatus1=appstatusAvg;
		if(biclique.setY.size()*biclique.setX.size()>=LSConfig.getInst().getDamageImpactLargest()){
			groupStatus1=1;
		}
		float groupStatus2=appstatusAvg+(1-LSConfig.getInst().getAlpha())*(groupStatus1-appstatusAvg);
		if(groupStatus2>biclique.groupStatus){//if the status increase, update it. OW, discard it.
			biclique.groupStatus=groupStatus2;
		}
		for(int i=0;i<biclique.setY.size();i++){
			String appId=biclique.setY.get(i);
			if(this.confirmedAppSet.containsKey(appId)){
				float oldVal=this.confirmedAppSet.get(appId);
				if(groupStatus2>oldVal&&oldVal!=0.5f){//do not update popular apps
					this.confirmedAppSet.put(appId, groupStatus2);//record the largest app status value
				}
			}else{
				this.confirmedAppSet.put(appId, groupStatus2);//record the largest app status value
			}
		}
	}
	/**
	 * Calculate app status of the biclique
	 * @param biclique
	 * @return
	 */
	private void calculateStatus2(Biclique biclique){
		float newGroupStatus=0;
		float sumTemp=0;
		float temp=0;
		float abusedAppCounter=0;
		for(int i=0;i<biclique.setY.size();i++){
			if(biclique.appStatus.get(i)>this.thresholdAbusedAppStatus){
				abusedAppCounter++;
			}
			sumTemp+=biclique.appStatus.get(i);
		}
		//if many abused apps has been found in a group, the group is probably a malicious group
		if(abusedAppCounter>=this.thresholdBadApps){
			sumTemp=biclique.setY.size();
		}
		newGroupStatus=sumTemp/(float)biclique.setY.size();
		if(newGroupStatus>biclique.groupStatus){//if the status increase, update it. OW, discard it.
			biclique.groupStatus=newGroupStatus;
		}
		//update app status for one group
		boolean changeFlag=false;//monitor whether there are any changes 
		for(int i=0;i<biclique.setY.size();i++){
			String appId=biclique.setY.get(i);
			temp=this.alpha*biclique.appStatus.get(i)+(1-this.alpha)*biclique.groupStatus;
			if(Math.abs(temp-biclique.appStatus.get(i))>this.epsilon){
				changeFlag=true;
			}
			biclique.appStatus.set(i, temp);
			//update the app status
			if(this.confirmedAppSet.containsKey(appId)){
				float oldVal=this.confirmedAppSet.get(appId);
				if(temp>oldVal&&oldVal!=0.5f){//do not update popular apps
					this.confirmedAppSet.put(appId, temp);//record the largest app status value
				}
			}else{
				this.confirmedAppSet.put(appId, temp);//record the largest app status value
			}
			
		}
		if(changeFlag){
			//continue to next iteration
			calculateStatus(biclique,false);
		}
		
	}
	
	/**
	 * Calculate the variance of CC value
	 * @param appData
	 */
	public float calculateVairanceCC(AppData appData){
		class Node{
			public float average_rating=0;
			public int numOfReviewers=0;
			public String version;
			public float variance;
			public float varReviewer;
		}
		HashMap<String, Float> averagePerVer=new HashMap<String,Float>();
		HashMap<String,Float> averageReviewerPerVersion=new HashMap<String,Float>();
		float cc=-1;
		//<Version,<Week>>
		TreeMap<String,TreeMap<String,Node>> nodeHashMap=new TreeMap<String,TreeMap<String,Node>>();
		for(Comment comment:appData.getCommentList()){
			TreeMap<String,Node> versionHM=null;
			if(comment.getDeviceVersion()!=null&&comment.getDeviceVersion().matches("[0-9.]*")){
				if(nodeHashMap.containsKey(comment.getDeviceVersion())){
					versionHM=nodeHashMap.get(comment.getDeviceVersion());
				}else{
					versionHM=new TreeMap<String,Node>();
				}
				Calendar cal = Calendar.getInstance();
				cal.setTime(comment.getDate());
				String weeks=cal.get(cal.YEAR)+"-"+cal.get(cal.WEEK_OF_YEAR);
				Node node=null;
				if(!versionHM.containsKey(weeks)){
					node=new Node();
					node.version=comment.getDeviceVersion();
					versionHM.put(weeks, node);
				}else{
					node=versionHM.get(weeks);
				}
				node.average_rating+=comment.getRating();
				node.numOfReviewers++;
				nodeHashMap.put(comment.getDeviceVersion(), versionHM);
			}
		}
		ArrayList<Node> input=new ArrayList<Node>();//order in version & weeks
		for(String version:nodeHashMap.keySet()){
			TreeMap<String,Node> versionHM=nodeHashMap.get(version);
			for(String weeks:versionHM.keySet()){
				Node node=versionHM.get(weeks);
				if(node.numOfReviewers>7){//remove all the weeks with too less reviewers.
					node.average_rating=node.average_rating/node.numOfReviewers;
					input.add(node);
				}
			}
		}
		//cal average rating per version
		String version="";
		if(input.size()>0){
			version=input.get(0).version;
		}
		int count=0;
		float sum=0;
		float sumReviewer=0;
		for(Node node:input){
			if(version.equalsIgnoreCase(node.version)){
				count++;
				sum+=node.average_rating;
				sumReviewer+=node.numOfReviewers;
			}else{
				averagePerVer.put(version, sum/count);
				averageReviewerPerVersion.put(version, sumReviewer/count);
				version=node.version;
				count=1;
				sum=node.average_rating;
				sumReviewer=node.numOfReviewers;
			}
		}
		averagePerVer.put(version, sum/count);
		averageReviewerPerVersion.put(version, sumReviewer/count);

		float avg;
//		float avgNum=0;
		for(Node node:input){
			if(!averagePerVer.containsKey(node.version)){
				System.out.println(" NOT FOUND "+node.version);
				continue;
			}
			avg=averagePerVer.get(node.version);
			node.variance=node.average_rating-avg;
			avg=averageReviewerPerVersion.get(node.version);
			node.varReviewer=node.numOfReviewers-avg;
//			avgNum+=node.numOfReviewers;
		}
//		avgNum=avgNum/input.size();
//		System.out.println("AppID : "+appData.getAppId());
		for(Node node:input){
			avg=averagePerVer.get(node.version);
			float avg2=averageReviewerPerVersion.get(node.version);
//		System.out.println(node.variance+"\t"+node.varReviewer);
		}
		float ssXX=0;
		float ssYY=0;
		float ssXY=0;
		for(Node node:input){
			ssXX+=node.variance*node.variance;
			ssYY+=node.varReviewer*node.varReviewer;
			ssXY+=node.variance*node.varReviewer;
		}
		if(ssXX==0||ssYY==0){
			cc=0.5f;
		}else{
			cc= (float) (ssXY/Math.sqrt(ssXX*ssYY));
		}
//		System.out.println(cc);

		//estimate the quality
		for(String ver:averagePerVer.keySet()){
			float avgRating=averagePerVer.get(ver);
			if(cc>0){
				avgRating=avgRating-cc*cc*(avgRating-1);
			}else{
				avgRating=avgRating-cc*cc*(avgRating-5);
			}
			averagePerVer.put(ver, avgRating);// get the estimated quality
		}
		return cc;
	}
	public boolean abnormalDistribution(Biclique bq, ConcurrentHashMap<String,AppData> appHM,ConcurrentHashMap<String, Reviewer> reviewerMap){
		MultiDateFormat sdf=new MultiDateFormat("dd-MMM-yyyy");
		Date dateStarter=null;
		try {
			dateStarter=sdf.parse("01-Jan-2000");
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			log.error(e1.getMessage());
		}
		for(int i=0;i<bq.setY.size();i++){
			String appId=bq.setY.get(i);
			HashSet<String> attackPattern=new HashSet<String>();
			//print each edge of the biclique
			for(String reviewerId:bq.setX){
				Reviewer rev=reviewerMap.get(reviewerId);
				int k=0;
				for(k=0;k<rev.getAppIdsOrded().size();k++){
					if(rev.getAppIdsOrded().get(k).equals(appId)){
						break;
					}
				}
				if(k==rev.getAppIdsOrded().size()){
					continue;
				}
				String dateString=rev.getReviewDateStrings().get(k);
				Date date=null;
				//find pattern
				long weeks=-1;//convert dateString to weeks
				//<weeks,<Reviewer>>
				try {
					date=sdf.parse(dateString);
					weeks=(date.getTime()-dateStarter.getTime())/(1000*60*60*24*7);//convert dateString to weeks
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					log.error(e.getMessage());
				}
				if(weeks>-1&&date!=null){
					attackPattern.add(sdf.selfFormat(date));
				}
			}
			//filter all the reviews has the same pattern
			
			AppData appDataTemp=appHM.get(appId);
			HashSet<Comment> allComments=new HashSet<Comment>();
			allComments.addAll(appDataTemp.getCommentList());
			float avg2=0;//average rating of reviewers who have left a comment
			float avg3=0;//average rating of the reviewers who have no attack pattern
			int []rating2Num=new int[6];//average rating of all the reviews with text content
			int []rating3Num=new int[6];//average rating of reviews after discrading suspects
			//average ratings of each version
			HashMap<String,float[]> avgVersionMap=new HashMap<String,float[]>();
			for(Comment comment:allComments){
				int rating=(int)comment.getRating();
				avg2=avg2+comment.getRating();
				rating2Num[0]++;
				if(rating>0&&rating<6){
					rating2Num[rating]++;
				}
				String pattern=sdf.selfFormat(comment.getDate());
				if(!attackPattern.contains(pattern)){//w pattern
					rating3Num[0]++;
					avg3=avg3+comment.getRating();
					if(rating>0&&rating<6){
						rating3Num[rating]++;
					}
				}
				//collect data of each version
				float[] avgArray=null;
				if(avgVersionMap.containsKey(comment.getDeviceVersion())){
					avgArray=avgVersionMap.get(comment.getDeviceVersion());
				}else{
					avgArray=new float[12];
					avgVersionMap.put(comment.getDeviceVersion(), avgArray);
				}
				avgArray[0]++;
				avgArray[rating]++;
				if(!attackPattern.contains(pattern)){
					avgArray[rating+6]++;
					avgArray[6]++;
				}
			}
			if(allComments.size()!=0){
				avg2=avg2/rating2Num[0];
			}
			if(rating3Num[0]!=0){
				avg3=avg3/rating3Num[0];
			}
			//caculate proof
			float avgProof[]=new float[6];
			for(int index=1;index<6;index++){
				if(rating2Num[index]!=0){
					avgProof[index]=(float)(rating2Num[index]-rating3Num[index])/rating2Num[index];//discarded rating
				}else{
					avgProof[index]=0;
				}
			}
			float HLR=-1;
			float FOR=-1;
			/*if(avgProof[1]+avgProof[2]>0){
				HLR=(avgProof[4]+avgProof[5])/(avgProof[1]+avgProof[2]);
			}else if(avgProof[4]+avgProof[5]==0){
				HLR=0;
			}else{
				HLR=1000;
			}*/
			HLR=(avgProof[4]+avgProof[5]+1)/(avgProof[1]+avgProof[2]+1);
			/*if(avgProof[1]>0){
				FOR=(avgProof[5])/(avgProof[1]);
			}else if(avgProof[5]==0){
				FOR=0.0f;
			}else{
				FOR=1000;
			}*/
			FOR=(avgProof[5]+1)/(avgProof[1]+1);
			if(HLR>=LSConfig.getInst().getProofThreshold()||HLR<=1.0f/LSConfig.getInst().getProofThreshold()
				|| FOR>=LSConfig.getInst().getProofThreshold()||FOR<=1.0f/LSConfig.getInst().getProofThreshold()){
				return true; 
			}
		}
		return false;
	}
	public static void main(String []args){
		/*DataPersistence.getInstance().setBaseConfig(LSConfig.getInst());
		AppData appData=DataPersistence.getInstance().loadAppDataFromDB("483583569");
		IndicatorsCalculator.getInstance().calculateVairanceCC(appData);*/
	}
}

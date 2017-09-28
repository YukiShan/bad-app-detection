import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import data.NodeStatus;
import utility.MultiDateFormat;
import utility.SystemLogger;
import data.Reviewer;

public class BicliqueRefiner {
	private static BicliqueRefiner bicliqueRefiner;
	private static SystemLogger log = SystemLogger.getLogger(BicliqueRefiner.class);

	
	private BicliqueRefiner(){}
	public static BicliqueRefiner getInstance(){
		if(bicliqueRefiner==null){
			bicliqueRefiner=new BicliqueRefiner();
		}
		return bicliqueRefiner;
	}
	/**
	 * Refining each bicliqueList
	 * @param bicliqueList - biclique list
	 * @param reviewerList - reviewers list
	 * @return ArrayList<Biclique> 
	 */
	public ArrayList<Biclique> miningSuspiciousGroupsOnBipartite(ArrayList<Biclique> bicliqueList,ArrayList<Reviewer> reviewerList){
		//build map <reviewerId, reviewer>
		log.info("Start to miningSuspiciousGroupsOnBipartite");
		HashMap<String,Reviewer> reviewerHashMap=new HashMap<String,Reviewer>();
		for(Reviewer rev:reviewerList){
			reviewerHashMap.put(rev.getReviewerId(), rev);
		}
		
		ArrayList<String> tempArrayList=new ArrayList<String>();
		ArrayList<Biclique> tempBicliqueList=new ArrayList<Biclique>();
		for(int j=0;j<bicliqueList.size();j++){
			Biclique biclique=bicliqueList.get(j);
			//remove smaller bicliques
			if(biclique.setX.size()<LSConfig.getInst().getThresholdReviewSize()||biclique.setY.size()<LSConfig.getInst().getThresholdAppSize()
					||biclique.setX.size()*biclique.setY.size()<LSConfig.getInst().getDamageImpact()){
				tempBicliqueList.add(biclique);
				continue;
			}
			
			//Collect all the apps of one biclique
			ArrayList<Reviewer> reviewers=new ArrayList<Reviewer>();
			for(String reviewerId:biclique.setX){
				reviewers.add(reviewerHashMap.get(reviewerId));
			}
			//check each app
			for(int i=0;i<biclique.setY.size();i++){
				String appId=biclique.setY.get(i);
				//mining suspicious groups
				int maliciousReviewer=this.oneDClustering(reviewers, appId, LSConfig.getInst().getThresholdInterval());
				
				if(maliciousReviewer>LSConfig.getInst().getThresholdReviewSize()){//it is a malicious group
					ArrayList<Reviewer> temp=new ArrayList<Reviewer>();
					for(Reviewer reviewer:reviewers){
						if(reviewer.getNodeStatus()!= NodeStatus.Bad){//remove good and unknown reviewers
							temp.add(reviewer);
							biclique.setX.remove(reviewer.getReviewerId());//remove this reviewer id
						}
					}
					reviewers.removeAll(temp);
				}else{//it is not a malicious group, remove this 
					for(Reviewer reviewer:reviewers){
						reviewer.setNodeStatus(NodeStatus.Unknown);//recover the node status
					}
					tempArrayList.add(appId);//delete such app
				}
			}
			biclique.setY.removeAll(tempArrayList);
			tempArrayList.clear();
			
			int di=biclique.setX.size()*biclique.setY.size();
			//check the suspicious clique again
			if(di<LSConfig.getInst().getDamageImpact()){
				tempBicliqueList.add(biclique);//remove from the list
			}else{
				if(di>LSConfig.getInst().getDamageImpactLargest()){
					biclique.groupStatus=1;
				}
			}
		}
		bicliqueList.removeAll(tempBicliqueList);
		return tempBicliqueList;
	}
	/**
	 * Detecting collusion groups from biclqiues by seeking rating concentration windows
	 * @param reviewers
	 * @param appId
	 * @param thresholdInterval
	 * @return
	 */
	private int oneDClustering(ArrayList<Reviewer> reviewers, String appId, float thresholdInterval){
		log.debug("Start to mining suspicious groups");
		MultiDateFormat sdf=new MultiDateFormat("dd-MMM-yyyy");
		Date dateStarter=null;
		try {
			dateStarter=sdf.parse("01-Jan-2000");
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//group reviews by app version, ratings
		//<appVersion,<rating,<Reviewer>>>
		HashMap<String,HashMap<String,HashSet<Reviewer>>> reviewerHashMap=new  HashMap<String,HashMap<String,HashSet<Reviewer>>>();
		for(Reviewer reviewer:reviewers){
			for(int i=0;i<reviewer.getAppIdsOrded().size();i++){
				String appIdStr=reviewer.getAppIdsOrded().get(i);
				if(appIdStr.equals(appId)){
					String rating=reviewer.getReviewRatings().get(i);
					int ratingVal=Integer.parseInt(rating.trim());
					if(ratingVal==3){//discard non-biased ratings
						continue;
					}else if(ratingVal>3){
						rating="High";
					}else{
						rating="Low";
					}
					String appVersion=reviewer.getAppVersionList().get(i);
					//<ratings,reviewers>
					HashMap<String,HashSet<Reviewer>> tmpHM=null;
					HashSet<Reviewer> tmpHS=null;
					if(reviewerHashMap.containsKey(appVersion)){
						tmpHM=reviewerHashMap.get(appVersion);
					}else{
						tmpHM=new HashMap<String,HashSet<Reviewer>>();
						reviewerHashMap.put(appVersion, tmpHM);
					}
					if(tmpHM.containsKey(rating)){
						tmpHS=tmpHM.get(rating);
					}else{
						tmpHS=new HashSet<Reviewer>();
						tmpHM.put(rating, tmpHS);
					}
					reviewer.setCurIndex(i);//IMPORTANT to set index of current app Id
					tmpHS.add(reviewer);
				}
			}
		}
		//mining collusion groups
		int maliciousReviewers=0;
		HeapSort hs=new HeapSort();
		HeapSort hsInterval=new HeapSort();
		for(String appVersion:reviewerHashMap.keySet()){
			HashMap<String,HashSet<Reviewer>> tmpHM=reviewerHashMap.get(appVersion);
			for(String rating:tmpHM.keySet()){
				//order all the reviewers by rating dates and calculate gaps between
				HashSet<Reviewer> tmpHS=tmpHM.get(rating);
				//<weeks,HashSet<Reviewer>>
				HashMap<String,HashSet<Reviewer>> reviewWeeksMap=new HashMap<String,HashSet<Reviewer>>();
				for(Reviewer rev:tmpHS){
					int index=rev.getCurIndex();//the index of current app Id
					String dateString=rev.getReviewDateStrings().get(index);
					long weeks=-1;//convert dateString to weeks
					//<weeks,<Reviewer>>
					try {
						Date date=sdf.parse(dateString);
						weeks=(date.getTime()-dateStarter.getTime())/(1000*60*60*24*7);//convert dateString to weeks
					} catch (ParseException e) {
						log.error(e.getMessage());
					}
					if(weeks>-1){
						HashSet<Reviewer> reviewersHS=null;
						if(reviewWeeksMap.containsKey(weeks+"")){
							reviewersHS=reviewWeeksMap.get(weeks+"");
						}else{
							reviewersHS=new HashSet<Reviewer>();
							reviewWeeksMap.put(""+weeks, reviewersHS);
						}
						reviewersHS.add(rev);
					}
				}
				//remove the weeks in which the reviewer's number is below the average
				float avgReviewerNumber=(float)tmpHS.size()/(float)reviewWeeksMap.size();
				for(String weeks:reviewWeeksMap.keySet()){
					int weeksInt=Integer.valueOf(weeks);
					HashSet<Reviewer> reviewersHS=reviewWeeksMap.get(weeks);
					if(reviewersHS.size()>=avgReviewerNumber){
						for(Reviewer revTemp:reviewersHS){
							hs.insertObject((int)weeksInt, revTemp);
						}
					}
				}
				
				//order all the reviewer date gap between any two consecutive ones
				//and choose the largest ones
				Reviewer reviewer=null;
				ArrayList<Reviewer> revOrderList=new ArrayList<Reviewer>();
				ArrayList<Integer> intervalList=new ArrayList<Integer>();
				int preWeeks=-1;
				int curWeeks=0;
				try{
					while((reviewer=(Reviewer)hs.removeRoot())!=null){
						revOrderList.add(reviewer);
						String dateString=reviewer.getReviewDateStrings().get(reviewer.getCurIndex());
						Date date=sdf.parse(dateString);
						curWeeks=(int) ((date.getTime()-dateStarter.getTime())/(1000*60*60*24*7));
						//cal cur weeks
						if(preWeeks>-1){//skip the first element
							int interval=preWeeks-curWeeks;
							intervalList.add(interval);//preweeks > curweeks
							hsInterval.insertObject(interval, new Integer(revOrderList.size()-1));
						}
						preWeeks=curWeeks;
					}
				}catch(Exception ex){
					log.error(ex.getMessage());
				}
				//reorder all the gaps by their review dates
				ArrayList<Integer> gapIndexes=new ArrayList<Integer>();//store top largest gaps
				try{
					Integer index=null;
					while((index=(Integer)hsInterval.removeRoot())!=null){
						if(index>=intervalList.size()){
							continue;
						}
						if(intervalList.get(index-1)<thresholdInterval){//get the largest gaps
							break;
						}
						gapIndexes.add(index);
					}
					//order all the index of each gap
					HeapSort gapHS=new HeapSort();
					for(Integer tempIndex:gapIndexes){
						Reviewer reviewerTemp=revOrderList.get(tempIndex);
						String dateString=reviewerTemp.getReviewDateStrings().get(reviewerTemp.getCurIndex());
						Date date=sdf.parse(dateString);
						curWeeks=(int) ((date.getTime()-dateStarter.getTime())/(1000*60*60*24*7));
						gapHS.insertObject(curWeeks, tempIndex);
					}
					gapIndexes.clear();
					while((index=(Integer)gapHS.removeRoot())!=null){
						gapIndexes.add(index);
					}
					gapHS.clear();
				}catch(Exception ex){
					log.error(ex.getMessage());

				}
				//check each group separated by large gaps 
				int starter=0;
				gapIndexes.add(revOrderList.size()-1);
				for(int i=0;i<gapIndexes.size();i++){
					int gapIndex=gapIndexes.get(i);
					if(gapIndex-starter+1>LSConfig.getInst().getThresholdReviewSize()){
						for(int j=starter;j<=gapIndex;j++){
							revOrderList.get(j).setNodeStatus(NodeStatus.Bad);
							maliciousReviewers++;
						}
					}
					starter=gapIndex+1;
				}
				hs.clear();
				hsInterval.clear();
			}
		}
		return maliciousReviewers;
	}
}

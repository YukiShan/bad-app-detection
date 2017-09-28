import java.io.*;
import java.net.HttpURLConnection;
import java.sql.ResultSet;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import crawlerEngine.CommentCollector;
import utility.DataPersistence;
import utility.MultiDateFormat;
import utility.SystemLogger;
import crawlerAgent.AppleItuneCrawler;
import crawlerAgent.Crawler;
import crawlerEngine.OverviewEngine;
import crawlerEngine.UserProfileEngine;
import data.AppData;
import data.Comment;
import data.Reviewer;

/**
 * @author Shanshan Li
 *
 */
public class LargeScaleDetector{
	private static SystemLogger log = SystemLogger.getLogger(LargeScaleDetector.class);
    //handler of crawler
	private Crawler crawler=null;
    //for counting the number of raters
	private volatile int count=0;

	//<appid, AppData> is used to store the apps already being crawled.
	private ConcurrentHashMap<String, AppData> appDataMap=null;

	//<reviewer_id, Reviewer> is used to store the reviewers already being crawled.
	private ConcurrentHashMap<String, Reviewer> reviewerMap=new ConcurrentHashMap<String, Reviewer>();
	//<appid, app status> - the final status of each app
	private HashMap<String,Float> confirmedAppSet=new HashMap<String,Float>();
	//popular apps that has been skipped
	private HashSet<String> popularAppList=new HashSet<String>();
	//already checked apps
	private HashSet<String> inspectedAppList =new HashSet<String>();
	//apps with a group
	private HashSet<String> suspiciousApps=new HashSet<String>();
	private HashSet<String> totalPreloadedApps =new HashSet<String>();
    //preset seed list
	private ConcurrentLinkedQueue<String> presetSeedList = new ConcurrentLinkedQueue<String>();
    //store appid of current group
	private ConcurrentLinkedQueue<String> appIdNeighborsQueue=new ConcurrentLinkedQueue<String>();
    //store app id of next round
	private ArrayList<String> appId2InspectQueue =new ArrayList<String>();
	private ConcurrentLinkedQueue<String> appIdMediumQueue=new ConcurrentLinkedQueue<String>();
    //for recording group counts
	private int groupCounter[]=new int[2];
	//Only for calculating computation time
	private ArrayList<Long> computationTimeList=new ArrayList<Long>();
	//Storing for the results
	private ArrayList<String> results=new ArrayList<String>();
	//For recording reported apps.
	private HashSet<String> reportedApps=new HashSet<String>();


	public LargeScaleDetector(Crawler crawler){
        DataPersistence.getInstance().setBaseConfig(LSConfig.getInst());
        this.crawler=crawler;
		this.init();
	}
	private void init() {
		this.appDataMap=this.crawler.getAppMap();
		this.appIdNeighborsQueue.add(LSConfig.getInst().getSeed());
		//remove the last record of apps
//		DataPersistence.getInstance().clearDuplicatedRecords();
		//load pre-founded popular app from DB
		ArrayList<String> popularAppsTemp=DataPersistence.getInstance().loadPopularAppsFromDB(LSConfig.getInst().getSeed());
		this.popularAppList.addAll(popularAppsTemp);
        //preload apps from database
        if(LSConfig.getInst().isPreload()){
            preLoad();
        }
        this.setPresetSeedList(null);
	}
    private void preLoad(){
        try{
            String str="select distinct(app_id) from AppData";
            ResultSet rs=LSConfig.getInst().getMessageManager().doSelect(str);
            while(rs.next()){
                this.totalPreloadedApps.add(rs.getString("app_id"));
                //all the preloaded apps would be taken as seeds
                this.presetSeedList.add(rs.getString("app_id"));
            }
            rs.close();
            for(String appId:this.totalPreloadedApps){
                this.dataCollection(appId);
            }
        }catch(Exception ex){
            ex.printStackTrace();
            log.error(ex.getMessage());
        }
    }
	
	public void run(){
		log.info("LargeScaleEngine starts to load ...");
		while(true){
			try{
				long starter0=System.currentTimeMillis();
                // get the next app to be checked
				String appIdTemp=this.nextApp();
				if(appIdTemp==null){//no app found
					break;
				}else if(this.inspectedAppList.contains(appIdTemp)){//already inspected
					continue;
				}else if(this.popularAppList.contains(appIdTemp)){//already identified as a popular app
					log.info("Skip popular app "+appIdTemp);
					continue;
				}else if(!this.dataCollection(appIdTemp)){//collect data of appIdTemp, if not found, continue
					continue;
				}
				long starter1=System.currentTimeMillis(); // for computing download time
				AppData startApp=this.appDataMap.get(appIdTemp);
				
				ArrayList<Reviewer> reviewerList=new ArrayList<Reviewer>();
				reviewerList.addAll(startApp.getReviewList());
				//Get the top co-rated apps
				HashSet<String> topApps=this.topRatedApp(reviewerList,(int)LSConfig.getInst().getThresholdTopRatedApps());
				//Detect bicliques based on these co-rated apps
				ArrayList<Biclique> bicliqueList=this.bicliqueDetection(topApps, reviewerList);

                //record suspicious apps in preload mode
                if(LSConfig.getInst().isPreload()) {
                    for (Biclique biclique : bicliqueList) {
                        //only consider those TMB with certain size
                        if (biclique.setX.size() >= LSConfig.getInst().getThresholdReviewSize()
                                || biclique.setY.size() >= LSConfig.getInst().getThresholdAppSize()) {
                            boolean flag = true;
                            for (String appid : biclique.setY) {
                                //skip those TMBs with unknown apps in pre-load mode
                                if (!this.totalPreloadedApps.contains(appid)) {
                                    flag = false;
                                    break;
                                }
                            }
                            if (flag) {
                                this.suspiciousApps.addAll(biclique.setY);
                            }
                        }
                    }
                }
				//refine each bicliques by clustering reviewers on time
                //smaller TMBs
				ArrayList<Biclique> unknownBicliqueList=BicliqueRefiner.getInstance()
						.miningSuspiciousGroupsOnBipartite(bicliqueList, reviewerList);
				long starter2=System.currentTimeMillis();
				
				//refine bicliques after discarding popular apps
				ArrayList<Biclique> smallBicliques=new ArrayList<Biclique>();
				for(Biclique biclique:bicliqueList){
					ArrayList<String> popularApps=new ArrayList<String>();
					for(String appId:biclique.setY){
						if(!this.appDataMap.containsKey(appId)){
							boolean returnVal=this.dataCollection(appId);
							if(!returnVal){//false means the app is a popular app
								popularApps.add(appId);
								continue;
							}
                            //add all the non-popular, un-inspected apps to the TMB queue for further search
							if(!this.appIdNeighborsQueue.contains(appId))	{
								this.appIdNeighborsQueue.add(appId);
							}
						}
					}
					//remove popular apps and check the remaining biclique size
					biclique.setY.removeAll(popularApps);
					int edgeSize=biclique.setX.size()*biclique.setY.size();
                    //remove small TMB with less apps, less raters or less edges
					if(biclique.setX.size()<LSConfig.getInst().getThresholdReviewSize()
                            ||biclique.setY.size()<LSConfig.getInst().getThresholdAppSize()
							||edgeSize<LSConfig.getInst().getDamageImpact()){
						smallBicliques.add(biclique);
					}else{
						log.info("Y-"+edgeSize+": "+biclique.setY);
						log.info("X: "+biclique.setX);
					}
				}
				bicliqueList.removeAll(smallBicliques);
                //add small TMBs to the list
				unknownBicliqueList.addAll(smallBicliques);
				
				long end2=System.currentTimeMillis();
                //add all the apps to the 2be inspected queue
				for(Biclique biclique:unknownBicliqueList){
					for(String appId:biclique.setY){
						if(!this.appDataMap.containsKey(appId)){
							if(!this.appId2InspectQueue.contains(appId)){
								this.appId2InspectQueue.add(appId);
							}
						}
					}
				}
				//calculate indicators
				IndicatorsCalculator.getInstance().calculator(bicliqueList, appDataMap,confirmedAppSet,this.reviewerMap);
                //calculate time spent
				long end1=System.currentTimeMillis();
				long crawlerTime=end2-starter2;//crawling other apps
				long computerTime=end1-starter1;
				computerTime=computerTime-crawlerTime;
				crawlerTime=crawlerTime+starter1-starter0;//add the first part
				if(bicliqueList.size()>0){
					this.printResults(bicliqueList,appIdTemp,crawlerTime,computerTime,this.suspiciousApps);
				}
				this.inspectedAppList.add(appIdTemp);//set it as already searched apps
			}catch(Exception ex){
				log.error(ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	/**
	 * Get next app to be checked 
	 * @return appId 
	 */
	private String nextApp(){
		if(this.appIdNeighborsQueue.isEmpty()){
			if(this.appId2InspectQueue.isEmpty()){
				if(this.appIdMediumQueue.isEmpty()){
					if(this.presetSeedList !=null && this.presetSeedList.size()>0){//read from seed
						String appIdTemp=this.presetSeedList.poll();
						if(appIdTemp!=null){
							this.appIdNeighborsQueue.add(appIdTemp);
							log.info("Seed: "+appIdTemp);
						}
					}else{
						log.info("END of DETECTION!!!");
						System.exit(0);
					}
					
				}else{
					String str=this.appIdMediumQueue.poll();
					if(str==null){
						return null;
					}else if(!LSConfig.getInst().isPreload() || this.totalPreloadedApps.contains(str)){
						this.appIdNeighborsQueue.add(str);
					}
					log.info("Jump to another community ...");
				}
				this.groupCounter[1]=0;
				this.groupCounter[0]++;
			}else{
				this.groupCounter[1]++;
				log.info("Start to search another TMB "+this.groupCounter);
				for(String appIdTemp:this.appId2InspectQueue){
					if(this.popularAppList.contains(appIdTemp)
							||this.inspectedAppList.contains(appIdTemp)){
						continue;//skip the popular apps and already checked apps
					}
					if(!LSConfig.getInst().isPreload() || this.totalPreloadedApps.contains(appIdTemp)){
						this.appIdNeighborsQueue.add(appIdTemp);
					}
				}
				this.appId2InspectQueue.clear();
				this.clearCache();
			}
		}
		
		String appIdTemp=this.appIdNeighborsQueue.poll();
		return appIdTemp;
	}
    @Deprecated
	private String nextApp2(){
		if(this.presetSeedList.size()>0){
			String appIdTemp=this.presetSeedList.poll();
			log.info("check app "+appIdTemp+" - left with"+this.presetSeedList.size());
			return appIdTemp;
		}else{
			log.info("No more apps");
			return null;
		}
	}
	/**
	 * Retrieve data of the app with from DB or Websites
	 * @param appIdTemp - the app id to be dealt with
	 * @return true -  this app is gonna checked.
	 * 		   false - no need to check this app. [popular app or checked app] 
	 */
	private boolean dataCollection(String appIdTemp){
		AppData appData=this.appDataMap.get(appIdTemp);
		if(appData!=null){
			return true;
		}else{
            //try to get lock of DB
			try{
				FlushToDBLS.getInstance().getLock();
				appData=DataPersistence.getInstance().loadAppDataFromDB(appIdTemp);
			}finally{
				FlushToDBLS.getInstance().releaseLock();
			}
			if(appData==null){//no record in the database
				if(LSConfig.getInst().isAllowWebpage()){
					boolean appCommentStatus=this.collectAppInfo(appIdTemp);
					if(!appCommentStatus
							||this.popularAppList.contains(appIdTemp)
							||this.inspectedAppList.contains(appIdTemp)){//if false
						return false;
					}		
					this.collectReviewerInfo(appIdTemp);
					appData=this.appDataMap.get(appIdTemp);
					FlushToDBLS.getInstance().saveObject(appData);
				}else{
					log.error("No Data of app "+appIdTemp);
					this.popularAppList.add(appIdTemp);
					return false;
				}
				
			}else{
				//build a map for reviewer
				for(Reviewer reviewer:appData.getReviewList()){
					this.reviewerMap.put(reviewer.getReviewerId(), reviewer);
				}
                //add this app to map
				this.appDataMap.put(appIdTemp, appData);
			}
		}
		
		return true;
	}
	
	/**
	 * Collect overview,comment,reviewer profile from webpages
	 * @param appId - the app to be collected
	 */
	private boolean collectAppInfo(String appId){
		try{
			if(LSConfig.getInst().getSeedStoreLocation()==null){
				log.error("Please specify store location if crawling on the fly!");
				System.exit(0);
			}
			AppData appData=new AppData(appId);
			appData.setStoreLocation(LSConfig.getInst().getSeedStoreLocation());
			this.appDataMap.put(appId, appData);
			OverviewEngine overviewEngine=new OverviewEngine(this.crawler);
			CommentCollector commentCrawler=new CommentCollector(this.crawler);
			log.info("Start to retrieve overview of " + appId);
			HttpURLConnection httpURLConnection=this.crawler.getOverviewHttpURLConnection(appId,appData.getStoreLocation());
			if(httpURLConnection==null){
				log.error("Failed to connect the overview page of "+appId);
				return false;
			}
			boolean resultStatus=overviewEngine.retrieveOverview(httpURLConnection,appData);
			if(!resultStatus){//failed to parse overview
				log.error("Failed to parse overview of "+appId);
				return false;
			}
			log.info("Finish retrieving overview of " + appId);
			appData.setUrl(httpURLConnection.getURL().toString());

            //find the total number of pages
            AppData appDataTest=new AppData(appData.getAppId());
            appDataTest.setStoreLocation(appData.getStoreLocation());
            boolean isMoreReview=commentCrawler.retrieveOnePage(appDataTest,(int)LSConfig.getInst().getPoplarAppReviewerSize());
            if(!isMoreReview){//less than certain page of reviews
                commentCrawler.run(appData);
            }
			if(appData.getCommentList().size()==0){//popular apps
				this.popularAppList.add(appId);
				this.appDataMap.remove(appId);
				log.info("Skip popular app "+appId);
				FlushToDBLS.getInstance().saveObject(LSConfig.getInst().getSeed()+","+appId);
				return false;
			}
			return true;
		}catch(Exception ex){
			log.error(ex.getMessage());
			return false;
		}
	}
	/**
	 * Collect reviewer's rating history
	 * @param appId - app's Id to be retrieved
	 */
	private void collectReviewerInfo(String appId){
		final AppData appData=this.appDataMap.get(appId);
		final UserProfileEngine upe=new UserProfileEngine(this.crawler);
		count=0;
		final ConcurrentLinkedQueue<Comment> tempCommentsList=new ConcurrentLinkedQueue<Comment>();
		final ConcurrentHashMap<String,Thread> threadSet=new ConcurrentHashMap<String,Thread>();
		tempCommentsList.addAll(appData.getCommentList());
		for(int i=0;i<LSConfig.getInst().getThreadCount();i++){
			final String threadName="UPEng_"+i;
			Thread thread=new Thread(new Runnable(){
				public void run(){
					String threadName2=threadName;
					try{
						while(count<LSConfig.getInst().getLargestReviewers()){
							Comment comment=tempCommentsList.poll();
							if(comment==null){
								break;
							}
							Reviewer reviewer=null;
							if(reviewerMap.containsKey(comment.getReviewerId())){
								reviewer=reviewerMap.get(comment.getReviewerId());
							}else{
                                reviewer=new Reviewer(comment.getReviewerId(), appData.getStoreLocation());
								upe.retrieveUserProfile(reviewer);
								reviewerMap.put(comment.getReviewerId(), reviewer);
							}
							if(reviewer.getReviewSize()>=2){//only collect data for reviewing at least twice
								appData.getReviewList().add(reviewer);
							}
							count++;
							if(count%1000==0){
								log.info("The "+count+"th reviewer has already been crawled.");
							}
						}
					}catch(Exception ex){
                        ex.printStackTrace();
						log.error(ex.getMessage());
					}finally{
						threadSet.remove(threadName2);
					}
					
				}
			});
			thread.setName(threadName);
			threadSet.put(threadName, thread);
			thread.start();
		}
		
		try{
			log.info("Start to crawle user review history of app "+appId);
			while(threadSet.size()>0){
				Thread.sleep(2000);
			}
			log.info("End of crawling user reivew history of app "+appId);
		}catch(Exception ex){
			log.error(ex.getMessage());
		}
		
	}
	/**
	 * Get the top most co-rated apps
	 * @param reviewerList - all the reviewers of an app
	 * @return HashSet<String>
	 */
	private HashSet<String> topRatedApp(ArrayList<Reviewer> reviewerList, int threshold){
		//max heap
		log.info("Start to calculate top rated apps");
		//Count apps that co-rated by these reviewers
		HashMap<String,Integer> hm=new HashMap<String,Integer>();
		for(Reviewer reviewer:reviewerList){
			for(String str:reviewer.getAppIdsOrded()){
				int temp=0;
				if(hm.containsKey(str)){
					temp=hm.get(str);
				}
				temp++;
				hm.put(str, temp);
			}
		}
		//sort popular apps
		HeapSort heapSort=new HeapSort();
		for(String str:hm.keySet()){
			if(hm.get(str)>=LSConfig.getInst().getThresholdReviewSize()){
				heapSort.insertObject(hm.get(str), str);
			}else if(hm.get(str)<=LSConfig.getInst().getThresholdReviewSize()/20){
				//add the unpopular app to intermediate list for further examination
				if(!this.popularAppList.contains(str)
						&&!this.inspectedAppList.contains(str)
						&&!this.appIdMediumQueue.contains(str)){
					this.appIdMediumQueue.add(str);
				}
			}
		}
		HashSet<String> appList=new HashSet<String>();
		//get the top N co-rated apps
		while(appList.size()<threshold){
			Object obj=heapSort.removeRoot();
			if(obj==null){
				break;
			}
			String appIdTemp=(String)obj;
			if(!this.popularAppList.contains(appIdTemp)){//skip popular apps
				appList.add((String)obj);
			}
		}
		log.info("TopRatedApps: "+appList.toString());
		return appList;
	}
	/**
	 * Detect maximum bicliques from bipartite graph
	 * @param appList
	 * @param reviewers
	 */
	private ArrayList<Biclique> bicliqueDetection(HashSet<String> appList,ArrayList<Reviewer> reviewers){
		//collect bipartite graph edges
		log.info("Start to detect biclique");
		ArrayList<String> edges=new ArrayList<String>();
		for(Reviewer reviewer:reviewers){
			for(String str:reviewer.getAppIdsOrded()){
				if(appList.contains(str)){
					edges.add(reviewer.getReviewerId()+" "+str);
				}
			}
		}
		ConcensusBiclique cb=new ConcensusBiclique(edges);
		return cb.findAllMaximalBiclique();
	}

    /**
     * Add preset app Ids as seeds
     * @param presetAppIdList
     */
	public void setPresetSeedList(ArrayList<String> presetAppIdList){
        if(presetAppIdList==null || presetAppIdList.size()==0){
            //if not set, read from local file
            String seedFilePath=LSConfig.getInst().getPresetSeedFile();
            if (seedFilePath==null || seedFilePath.trim().equals("")){
                return;
            }
            try{
                File file=new File(seedFilePath);
                if (!file.exists()){
                    log.error(" File not exists. "+seedFilePath);
                    return;
                }
                BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String line=null;
                while((line=br.readLine())!=null){
                    this.presetSeedList.add(line.trim());
                }
                br.close();

            }catch (Exception ex){
                log.error(ex.getMessage());
            }
        }else{
            this.presetSeedList.addAll(presetAppIdList);
        }
    }
	/**
	 * Clear cached app data and reviewers information
	 */
	private void clearCache(){
		if(FlushToDBLS.getInstance().isDoneAndWait()){//will wait for all the object flushed to db
			this.appDataMap.clear();
			this.reviewerMap.clear();
		}
		
	}
	//result interface
	
	/**
	 * Save the results to file located in directory logs, one store location only allow one procedure
	 * Three files are 
	 * app_status.txt - storing the status of all the detected apps
	 * biclique.txt - storing all the bicliques
	 * app_graph.txt - storing all the app graph
	 * @param bqList
	 */
	private void printResults(ArrayList<Biclique> bqList,String starterApp,long crawlerTime, long computeTime,HashSet<String>suspiciousApps){
		PrintWriter printWriterAS=null;
		PrintWriter printWriterBC=null;
		PrintWriter printWriterAG=null;
		PrintWriter printWriterT=null;
		PrintWriter printWriterS=null;
		PrintWriter printWriterBE=null;
		PrintWriter printWriterV=null;
		PrintWriter printWriterRA=null;
		PrintWriter printWriterCG=null;
		PrintWriter printWriterSA=null;
		try{
			String storeLocation=LSConfig.getInst().getSeedStoreLocation();
			if(storeLocation==null){
				storeLocation="NA";
			}
			String fileAS="results/"+storeLocation+"_"+"appstatus.txt";
			String fileBC="results/"+storeLocation+"_"+"bicliques.txt";
			String fileAG="results/"+storeLocation+"_"+"appgraph.txt";
			String fileT="results/"+storeLocation+"_"+"timing.txt";
			String fileBE="results/"+storeLocation+"_"+"biclique_edges_detail.txt";
			String fileS="results/"+storeLocation+"_"+"appscore.txt";//out put average_score/average_rating_score/adjusted score
			String fileV="results/"+storeLocation+"_"+"appVersionRating.txt";//out put average_score/average_rating_score/adjusted score
			String fileRA="results/"+storeLocation+"_"+"abused_apps.txt";
			String fileCG="results/"+storeLocation+"_"+"collusion_groups.txt";
			String fileSA="results/"+storeLocation+"_"+"suspicious_apps.txt";

			printWriterAS=new PrintWriter(new BufferedWriter(new FileWriter(fileAS, true)));
			printWriterBC=new PrintWriter(new BufferedWriter(new FileWriter(fileBC, true)));
			printWriterAG=new PrintWriter(new BufferedWriter(new FileWriter(fileAG, true)));
			printWriterT=new PrintWriter(new BufferedWriter(new FileWriter(fileT, true)));
			printWriterS=new PrintWriter(new BufferedWriter(new FileWriter(fileS,true)));
			printWriterBE=new PrintWriter(new BufferedWriter(new FileWriter(fileBE,true)));
			printWriterV=new PrintWriter(new BufferedWriter(new FileWriter(fileV,true)));
			printWriterRA=new PrintWriter(new BufferedWriter(new FileWriter(fileRA,true)));
			printWriterCG=new PrintWriter(new BufferedWriter(new FileWriter(fileCG,true)));
			printWriterSA=new PrintWriter(new BufferedWriter(new FileWriter(fileSA,true)));
			
			MultiDateFormat sdf=new MultiDateFormat("dd-MMM-yyyy");
			Date dateStarter=null;
			try {
				dateStarter=sdf.parse("01-Jan-2000");
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				log.error(e1.getMessage());
			}
			
			//different start point has different prefix
			String prefix=storeLocation+"C"+this.groupCounter[0]+"G"+this.groupCounter[1]+"_"+starterApp;
			
			//print bicliques
			HashMap<String,String> abuseProof=new HashMap<String,String>(); 
			for(Biclique bq:bqList){
				printWriterBC.println(prefix+"\t"+bq.setY+"-"+bq.setX);
				//print edges
				int groupSize=bq.setX.size();
				for(int i=0;i<bq.setY.size();i++){
					String appId=bq.setY.get(i);
					for(int j=i+1;j<bq.setY.size();j++){
						String appId2=bq.setY.get(j);
						if(!appId.equals(appId2)){
							printWriterAG.println(prefix+"\t"+appId+"\t"+appId2+"\t"+groupSize);
						}
					}
					HashSet<String> attackPattern=new HashSet<String>();
					//print each edge of the biclique
					for(String reviewerId:bq.setX){
						Reviewer rev=this.reviewerMap.get(reviewerId);
						int k=0;
						for(k=0;k<rev.getAppIdsOrded().size();k++){
							if(rev.getAppIdsOrded().get(k).equals(appId)){
								break;
							}
						}
						if(k==rev.getAppIdsOrded().size()){
							log.error("No reviewerId found in reviewerId List");
							continue;
						}
						String appVersion=rev.getAppVersionList().get(k);
						String appRating=rev.getReviewRatings().get(k);
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
							printWriterBE.println(prefix+"\t"+appId+"\t"+reviewerId+"\t"+appVersion+"\t"+appRating+"\t"+dateString+"\t"+weeks);
							attackPattern.add(sdf.selfFormat(date));
						}
					}
					//filter all the reviews has the same pattern
					AppData appDataTemp=this.appDataMap.get(appId);
					HashSet<Comment> allComments=new HashSet<Comment>();
					allComments.addAll(appDataTemp.getCommentList());
					float avg1=appDataTemp.getAverageRating();//average rating for all the rating 
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
//						long weeks=(comment.getDate().getTime()-dateStarter.getTime())/(1000*60*60*24*7);//convert dateString to weeks
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
					String avgProofStr="";
					if(avgProof[1]+avgProof[2]>0){
						avgProofStr+=(avgProof[4]+avgProof[5])/(avgProof[1]+avgProof[2]);
					}else if(avgProof[4]+avgProof[5]==0){
						avgProofStr+="0";
					}else{
						avgProofStr+="1000";
					}
					avgProofStr+="\t";
					if(avgProof[1]>0){
						avgProofStr+=(avgProof[5])/(avgProof[1]);
					}else if(avgProof[5]==0){
						avgProofStr+="0";
					}else{
						avgProofStr+="1000";
					}
					abuseProof.put(appId, avgProofStr);
					//********** END *****************
					
					float appStatus=this.confirmedAppSet.get(appId);
					printWriterS.print(prefix+"\t"+appId+"\t"+appStatus+"\t"+avg1+"\t"+avg2+"\t"+avg3+"\t");
					printWriterS.print(appDataTemp.getTotalRaters()+"\t"+appDataTemp.getRatingNum(1)+"\t"+appDataTemp.getRatingNum(2)+"\t"
							+appDataTemp.getRatingNum(3)+"\t"+appDataTemp.getRatingNum(4)+"\t"+appDataTemp.getRatingNum(5)+"\t");
					printWriterS.print(rating2Num[0]+"\t"+rating2Num[1]+"\t"+rating2Num[2]+"\t"+rating2Num[3]+"\t"+rating2Num[4]+"\t"+rating2Num[5]+"\t");
					printWriterS.print(rating3Num[0]+"\t"+rating3Num[1]+"\t"+rating3Num[2]+"\t"+rating3Num[3]+"\t"+rating3Num[4]+"\t"+rating3Num[5]+"\t");
					for(String pattern:attackPattern){
						printWriterS.print(pattern+"\t");
					}
					printWriterS.print("\n");
					//print 
					printWriterV.print(prefix+"\t"+appId+"\t"+appStatus+"\t"+avg1+"\t"+avg2+"\t"+avg3+"\t");
					printWriterV.print(appDataTemp.getTotalRaters()+"\t"+appDataTemp.getRatingNum(1)+"\t"+appDataTemp.getRatingNum(2)+"\t"
							+appDataTemp.getRatingNum(3)+"\t"+appDataTemp.getRatingNum(4)+"\t"+appDataTemp.getRatingNum(5)+"\t");
					printWriterV.print(rating2Num[0]+"\t"+rating2Num[1]+"\t"+rating2Num[2]+"\t"+rating2Num[3]+"\t"+rating2Num[4]+"\t"+rating2Num[5]+"\t");
					printWriterV.print(rating3Num[0]+"\t"+rating3Num[1]+"\t"+rating3Num[2]+"\t"+rating3Num[3]+"\t"+rating3Num[4]+"\t"+rating3Num[5]+"\t");
					for(String ver:avgVersionMap.keySet()){
						printWriterV.print(ver+"\t");
						float []tempArray=avgVersionMap.get(ver);
						if(tempArray[0]==0){//skip the never changed app
							continue;
						}
						int sum=0;
						for(int tai=1;tai<6;tai++){
							sum+=tempArray[tai]*tai;
						}
						tempArray[0]=sum/tempArray[0];
						
						sum=0;
						for(int tai=7;tai<12;tai++){
							sum+=tempArray[tai]*(tai-6);
						}
						if(tempArray[6]!=0){
							tempArray[6]=sum/tempArray[6];
						}
						int endIndex=tempArray[6]==0?6:12;//only print changed version
						for(int tai=0;tai<endIndex;tai++){
							printWriterV.print(tempArray[tai]+"\t");
						}
					}
					printWriterV.print("\n");
				}
				//print collusion groups
				if(bq.groupStatus>=LSConfig.getInst().getAppStatusThreshold()){
					printWriterCG.println(prefix+"\t"+bq.groupStatus+"\t"+bq.setX);
				}
			}
			if(this.appDataMap.containsKey(starterApp)){
				if(this.appDataMap.get(starterApp).getCommentList()!=null){
					printWriterT.print(prefix+"\t"+crawlerTime+"\t"+computeTime+"\t"+this.appDataMap.get(starterApp).getCommentList().size()+"\t"
							+bqList.size()+"\t");
					this.results.add(starterApp+"\t"+"timing"+"\t"+crawlerTime+"\t"+computeTime);
					this.computationTimeList.add(computeTime);
					for(Biclique bq:bqList){
						int di=bq.setX.size()*bq.setY.size();
						printWriterT.print(di+"\t");
					}
					printWriterT.print("\n");
				}
			}
			
			//print app status
			HashSet<String> allAppIds=new HashSet<String>();
			for(Biclique bq:bqList){
				allAppIds.addAll(bq.setY);
			}
			for(String appId:allAppIds){
				float appStatus=this.confirmedAppSet.get(appId);
				String ap="";
				if(abuseProof.containsKey(appId)){
					ap=abuseProof.get(appId);
				}
				printWriterAS.println(prefix+"\t"+appId+"\t"+appStatus+"\t"+ap);
				this.results.add(starterApp+"\t"+"status"+"\t"+appId+"\t"+appStatus+"\t"+ap);
				if(appStatus>=LSConfig.getInst().getAppStatusThreshold()
						&& !this.reportedApps.contains(appId)){
					this.reportedApps.add(appId);
					printWriterRA.println(this.reportedApps.size()+". " +appId);
				}
			}
			for(String appId:suspiciousApps){
				printWriterSA.println(appId);
			}
			suspiciousApps.clear();
			
			printWriterAS.flush();
			printWriterBC.flush();
			printWriterAG.flush();
			printWriterT.flush();
			printWriterS.flush();
			printWriterBE.flush();
			printWriterV.flush();
			printWriterRA.flush();
			printWriterCG.flush();
			printWriterSA.flush();
		}catch(Exception ex){
			log.error(ex.getMessage());
			ex.printStackTrace();
		}finally{//close files
			if(printWriterAS!=null){
				printWriterAS.close();
			}
			if(printWriterBC!=null){
				printWriterBC.close();
			}
			if(printWriterAG!=null){
				printWriterAG.close();
			}
			if(printWriterT!=null){
				printWriterT.close();
			}
			if(printWriterS!=null){
				printWriterS.close();
			}
			if(printWriterBE!=null){
				printWriterBE.close();
			}
			if(printWriterV!=null){
				printWriterV.close();
			}
			if(printWriterRA!=null){
				printWriterRA.close();
			}
			if(printWriterCG!=null){
				printWriterCG.close();
			}
			if(printWriterSA!=null){
				printWriterSA.close();
			}
		}
	}
	
	public void exit(){
		FlushToDBLS.getInstance().exit();
		/*this.appDataMap.clear();
		this.reviewerMap.clear();*/
		//<appid, app status> - the final status of each app
		this.confirmedAppSet.clear();
		this.popularAppList.clear();
		this.inspectedAppList.clear();
		this.appIdNeighborsQueue.clear();
		this.appId2InspectQueue.clear();
		this.appIdMediumQueue.clear();
		this.results.clear();
		this.reportedApps.clear();
	}

	/**
	 *  Starter point
	 * @param args - not used
	 */
	public static void main(String []args){
		Crawler crawler=new AppleItuneCrawler(); //could be changed to GooglePlayCrawler
		LargeScaleDetector lse=new LargeScaleDetector(crawler);
		lse.run();
		lse.exit();
	}
}

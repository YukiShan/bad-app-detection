package crawlerAgent;

import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import configuration.CrawlerConfig;
import crawlerEngine.CrawlerEngine;
import data.*;

import crawlerParser.ContentParser;

import utility.SystemLogger;

//shared data structure
public abstract class Crawler {
    protected SystemLogger log = SystemLogger.getLogger(Crawler.class);
    //<appid,AppData>
	private ConcurrentHashMap<String, AppData> appMap=null;
	//store appid for IdEngine
	private ConcurrentLinkedQueue<AppId> appList=null;
    //A hashmap to prevent duplicated apps, <appId hashcode, latest comment time>
    private ConcurrentHashMap<Integer,Long> appSet=null;
    private final Integer appListCapacity=new Integer(10000);
	//store reviewerIdList for userProfileEngine
	private ConcurrentLinkedQueue<Reviewer> reviewerIdList=null;
    private ConcurrentHashMap<Integer,Long> reviewerSet=null;
    private final Integer reviewerListCapacity =new Integer(10000);
	//store developerId for developer id list
	private ConcurrentLinkedQueue<Developer> developerIdList=null;
    //A hashmap to prevent duplicated apps, <Developer hashcode, last modified time>
    private ConcurrentHashMap<Integer,Long> developerSet=null;
    private final Integer developerIdListCapacity=new Integer(10000);
    private long intervalsOfUpdate=Long.MAX_VALUE;//604800000;//one week
	protected static ContentParser contentParser=null;
	protected static Crawler crawler=null;


	public Crawler(){
		this.init();
	}
	
	private void init(){
		this.appMap=new ConcurrentHashMap<String, AppData> ();
		this.appList=new ConcurrentLinkedQueue<AppId>();
        this.appSet=new ConcurrentHashMap<Integer, Long>();
		this.reviewerIdList=new ConcurrentLinkedQueue<Reviewer> ();
        this.reviewerSet=new ConcurrentHashMap<Integer, Long>();
		this.developerIdList=new ConcurrentLinkedQueue<Developer>();
        this.developerSet=new ConcurrentHashMap<Integer, Long>();
	}


	/**
	 * Get the http connection with different url
	 * @param para: para[0]-category; para[1]-page number; para[3]-store location where the id belongs to; para[4]-crawler point like top_free
	 * @return HttpURLConnection
	 */
	public abstract HttpURLConnection getIdHttpURLConnection(String ...para);
	/**
	 *  Get the http connection with different url
	 * @param para: para[0]-app id,
	 * @return HttpURLConnection
	 */
	public abstract HttpURLConnection getOverviewHttpURLConnection(String ...para);
	/**
	 *  Get the http connection with different url
	 * @param para: para[0]-app id
	 * @return HttpURLConnection
	 */
	public abstract HttpURLConnection getCommentHttpURLConnection(String ...para);
	/**
	 *  Get the http connection with different url
	 * @param para:  para[0]-category; para[1]-page number; para[3]-store location where the id belongs to
	 * @return HttpURLConnection
	 */
	public abstract HttpURLConnection getRankHttpURLConnection(String ...para);
	/**
	 * Get the http connection of user profile
	 * @param para: para[0]-reviewer id, para[0]-store location
	 * @return HttpURLConnection
	 */
	public abstract HttpURLConnection getUserProfileHttpURLConnection(String ...para);

    /**
     * Get the http connection of developer profile
     * @param para
     * @return
     */
    public abstract HttpURLConnection getDeveloperProfileHttpURLConnection(String ... para);

    /*
        For accessing appmap
     */
	public boolean isAppIdExist(String key){
		if(this.appMap.containsKey(key)){
			return true;
		}
		return false;
	}
	public AppData getAppData(String key){
		if(key==null){
			return null;
		}
		if(this.appMap.containsKey(key)){
			AppData appData=this.appMap.get(key);
			return appData;
		}
		return null;
	}
    public void setAppData(String appId, AppData appData){
        this.appMap.put(appId, appData);
    }
    public int getAppMapSize(){
        return this.appList.size();
    }
    public ConcurrentHashMap<String, AppData> getAppMap() {
        return appMap;
    }
    /*
        For accessing appIdList
     */
	public AppId getAppId() {
        AppId appId=appList.poll();
        try {
            if (appId==null) {
                log.info("No appId is found, suspend all the consumer threads!");
                while ((appId=appList.poll())==null) {
                    synchronized (appList){
                        appList.wait(); //suspend consumer threads because of empty
                    }
                }
            }
            if(appList.size()<this.appListCapacity/2){
                synchronized (this.appListCapacity){
                    this.appListCapacity.notifyAll(); // notify producer threads suspended because of full
                }
            }
        }catch (Exception ex){
            log.error(ex);
            ex.printStackTrace();
        }
        return appId;
	}

    public void addAppId(AppId appId){
        if(appId==null){
            return;
        }
        try {
            if(this.appSet.containsKey(appId.hashCode()) &&
                    this.appSet.get(appId.hashCode())-System.currentTimeMillis() <this.intervalsOfUpdate){
                return;
            }
            this.appList.add(appId);
            this.appSet.put(appId.hashCode(),System.currentTimeMillis());
            synchronized (this.appList) {
                this.appList.notify();//notify the first threads suspended because of empty queue
            }
            if (appList.size() > this.appListCapacity) {
                synchronized (this.appListCapacity) {
                    this.appListCapacity.wait();
                }
            }
        }catch (Exception ex){
            log.error(ex);
            ex.printStackTrace();
        }
    }
    public void addAppIds(Collection<AppId> appListTemp){
        if(appListTemp==null || appListTemp.size()==0){
            return;
        }
        for(AppId appId:appListTemp){
            this.addAppId(appId);
        }
    }
    //process Reviewer list
    public Reviewer getReviewer(){
       Reviewer reviewer=null;
        try{
            reviewer=this.reviewerIdList.poll();
            if(reviewer==null){
                log.info("Reviewer List is empty!");
                while((reviewer=this.reviewerIdList.poll())==null){
                    synchronized (this.reviewerIdList){
                        this.reviewerIdList.wait();
                    }
                }
            }
            if(this.reviewerIdList.size()<this.reviewerListCapacity /2){
                synchronized (this.reviewerListCapacity){
                    this.reviewerListCapacity.notifyAll();
                }
            }
        }catch (Exception ex){
            log.error(ex);
            ex.printStackTrace();
        }
        return reviewer;
    }

    public void addReviewer(Reviewer reviewer){
        if(reviewer == null){
            log.error("Reviewer is null!");
            return ;
        }
        try{
            if(this.reviewerSet.containsKey(reviewer.hashCode()) &&
                    this.reviewerSet.get(reviewer.hashCode())-System.currentTimeMillis()<this.intervalsOfUpdate){
                return;
            }
            this.reviewerIdList.add(reviewer);
            this.reviewerSet.put(reviewer.hashCode(),System.currentTimeMillis());
            synchronized (this.reviewerIdList) {
                this.reviewerIdList.notify();
            }
            if (this.reviewerIdList.size() > this.reviewerListCapacity) {
                synchronized (this.reviewerListCapacity) {
                    this.reviewerListCapacity.wait();
                }
            }
        }catch (Exception ex){
            log.error(ex);
            ex.printStackTrace();
        }
    }
    public void addReviewers(Collection<Reviewer> reviewers){
        if(reviewers==null || reviewers.size() == 0){
            log.error("Reviewers is null!");
            return;
        }
        for(Reviewer reviewer:reviewers){
            this.addReviewer(reviewer);
        }
    }
    //process developer list
    public Developer getDeveloper(){
        Developer developer=null;
        try {
            developer = this.developerIdList.poll();
            if (developer == null) {
                log.info("Developer list is empty!");
                while ((developer = this.developerIdList.poll()) == null) {
                    synchronized (this.developerIdList) {
                        this.developerIdList.wait();
                    }
                }
            }
            if(this.developerIdList.size()<this.developerIdListCapacity/2){
                synchronized (this.developerIdListCapacity){
                    this.developerIdListCapacity.notifyAll();
                }
            }
        }catch (Exception ex){
            log.error(ex);
            ex.printStackTrace();
        }
        return developer;
    }
    public void addDeveloper(Developer developer){
        if(developer==null){
            log.error("Developer is null!");
            return;
        }
        try {
            //check if it needs to update
            if(this.developerSet.containsKey(developer.hashCode()) &&
                    this.developerSet.get(developer.hashCode())- System.currentTimeMillis()<this.intervalsOfUpdate){
                return ;
            }
            this.developerIdList.add(developer);
            this.developerSet.put(developer.hashCode(),System.currentTimeMillis());//update access time
            synchronized (this.developerIdList){
                this.developerIdList.notify();//notify one consumer
            }
            if (this.developerIdList.size() > this.developerIdListCapacity) {
                synchronized (this.developerIdListCapacity) {
                    this.developerIdListCapacity.wait();
                }
            }
        }catch (Exception ex){
            log.error(ex);
            ex.printStackTrace();
        }
    }
    public void addDevelopers(Collection<Developer> developers){
        if(developers==null || developers.size() == 0){
            log.error("Developer is null!");
            return;
        }
        for(Developer developer:developers){
            this.addDeveloper(developer);
        }
    }

	public ContentParser getContentParser() {
		return contentParser;
	}


    abstract public void  start();
}

package utility;

import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import DAO_DB.MessageManager;
import configuration.BaseConfig;
import configuration.CrawlerConfig;
import crawlerAgent.Crawler;
import data.*;

public class DataPersistence {
	private boolean sysMode=false;//true-running mode false-debug mode
	private static DataPersistence dataPersistence=null;
	private static SystemLogger log = SystemLogger.getLogger(DataPersistence.class);
	public static MessageManager messageManager = null;
	private String crawlerName;
	private Crawler crawler=null;
	private BaseConfig baseConfig=null;
	
	private DataPersistence(){
    }
	public static DataPersistence getInstance(){
		if(dataPersistence==null){
			dataPersistence=new DataPersistence();
		}
		return dataPersistence;
	}
	/***************  manipulate database **********************/
    //save to database
	public void saveAppDataToDB(AppData appData){
		this.messageManager.insertToAppData(this.crawlerName,appData);
	}
	public void saveCommentToDB(Comment comment){
		this.messageManager.insertToAppComment(this.crawlerName, comment);
	}
	public void saveRatingsToDB(AppRatingInfo ari){
		this.messageManager.insertToAppRatings(this.crawlerName,ari);
	}
	public void saveReviewerToDB(Reviewer reviewer){
		if(reviewer!=null){
			this.messageManager.insertToReviewers(this.crawlerName, reviewer);
			//if a new app was found, add it to the list
			//ArrayList<String> ratedAppId=reviewer.getAppIdsOrded();
		}else{
			log.error("reviewer is null ");
		}
	}
	public void savePopularAppsToDB(String starterSeed,String popularApp){
		this.messageManager.savePopularApps(starterSeed, popularApp);
	}
    public void saveAppIdToDB(AppId appId){
        this.messageManager.insertAppIdToDB(this.crawlerName,appId);
    }
    public void saveDeveloperToDB(Developer developer){
        this.messageManager.insertDeveloperToDB(this.crawlerName,developer);
    }
	//load AppData
	public AppData loadAppDataFromDB(String appId){
		return this.messageManager.loadAppDataFromDB(appId);
	}
	public ArrayList<String> loadPopularAppsFromDB(String starterSeed){
		return this.messageManager.loadPopularApps(starterSeed);
	}
	public void clearDuplicatedRecords(){
		this.messageManager.clearDuplicatedRecords();
	}
	//start from broken point
	public String loadLatestReviewId(String appId){
		return this.messageManager.loadLatestReveiwIdForApp(this.crawlerName, appId);
	}
	public ConcurrentLinkedQueue<AppData> loadAppIdFromAppRatings(String storeLocation,long startPoint){
		return this.messageManager.loadAppIdFromAppRatings(this.crawlerName,storeLocation, startPoint);
	}
	public Object getCrawler() {
		return crawler;
	}
	public void setCrawler(Crawler crawler) {
		this.crawler = crawler;
	}
	public Proxy getProxy(URL url){
		if(!this.baseConfig.isProxyMode()){
			return null;
		}
		List<Proxy> lp=null;
		try {
			lp =this.baseConfig.getMyProxySelector().select(url.toURI());
			if(lp!=null&&lp.size()>0){
				return lp.get(0);
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		return null;
	}
	public void setProxyError(URI uri,Proxy proxy){
		if(proxy==null){
			return;
		}
		this.baseConfig.getMyProxySelector().connectFailed(uri, proxy.address(),null);
	}
	public void decreaseLoad(Proxy proxy){
		if(proxy==null){
			return;
		}
		this.baseConfig.getMyProxySelector().decreaseLoad(proxy.address());
	}
	public boolean isSysMode() {
		return sysMode;
	}
	public String getDbMode() {
		return this.baseConfig.getDbMode();
	}
	public void setBaseConfig(BaseConfig baseConfig) {
		this.baseConfig = baseConfig;
		this.messageManager=this.baseConfig.getMessageManager();
		this.crawlerName=this.baseConfig.getCrawlerName();
	}
	public BaseConfig getBaseConfig(){
		return this.baseConfig;
	}
}

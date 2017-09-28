package crawlerEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import DAO_DB.FlushToDB;
import data.AppId;
import data.Developer;
import utility.DataPersistence;
import utility.SystemLogger;
import configuration.CrawlerConfig;
import crawlerAgent.Crawler;
import data.Reviewer;

public class UserProfileEngine extends Thread implements CrawlerEngine{
	private SystemLogger log = SystemLogger.getLogger(UserProfileEngine.class);
	private Crawler crawler=null;
	private int engineToken;
	public UserProfileEngine(){
	}
	public UserProfileEngine(Crawler crawler){
		this.crawler=crawler;
	}
	@Override
	public void init(int engineNum,Crawler crawler, String... para) {
		// TODO Auto-generated method stub
		this.crawler=crawler;
		this.engineToken=engineNum;
        this.setName("UserProfileEG_"+engineNum);

    }
	public void run(){
		while(CrawlerConfig.getInstance().getToken("UserProfileEngine", engineToken)){
			try{
				Reviewer reviewer=this.crawler.getReviewer();
				if(reviewer==null){
					log.info(" Sleep 20 sec ...");
					Thread.sleep(20000);
				}
                log.info("Start to crawl Reviewer "+reviewer.getReviewerId());

                retrieveUserProfile(reviewer);
                log.info("End of crawling Reviewer "+reviewer.getReviewerId());
                if(reviewer.getReviewSize()==0){
                    log.error("Failed to parse the userfile of " + reviewer.getReviewerId());
                    this.crawler.addReviewer(reviewer);
                    continue;
                }
                //save data to db
                FlushToDB.getInstance().saveObject(reviewer);
                //transfer data to OverviewEngine
                if(CrawlerConfig.getInstance().isEngineCooperated(UserProfileEngine.class,OverviewEngine.class)){
                    for(String appId:reviewer.getAppIdsOrded()){
                        this.crawler.addAppId(new AppId(appId,null,null,reviewer.getStoreLocation(),null));
                    }
                }
                //transfer data to DeveloperProfileEngine
                if(CrawlerConfig.getInstance().isEngineCooperated(UserProfileEngine.class,DeveloperProfileEngine.class)){
                    for(String developerId:reviewer.getDeveloperIdList()){
                        this.crawler.addDeveloper(new Developer(developerId,reviewer.getStoreLocation()));
                    }
                }
			}catch(Exception ex){
				log.error(ex);
			}
		}
	}
	
	public void retrieveUserProfile(Reviewer reviewer){
		int maxPageNum=Integer.MAX_VALUE;
		boolean flag=true;
		for(int page=1;flag&&page<maxPageNum;page++){
			flag=retrieveUserProfileOnePage(reviewer,reviewer.getStoreLocation(),page+"");
		}
	}
	private boolean retrieveUserProfileOnePage(Reviewer reviewer,String storeLocation,String pageNum){
		HttpURLConnection httpConn=null;
		BufferedReader reader=null;
		try{
			log.debug("Start to connect reviewer link of "+reviewer.getReviewerId());
			httpConn =this.crawler.getUserProfileHttpURLConnection(reviewer.getReviewerId(),storeLocation, pageNum);
			if(httpConn==null){
				httpConn =this.crawler.getUserProfileHttpURLConnection(reviewer.getReviewerId(),storeLocation, pageNum);
				if(httpConn==null){
					return false;
				}
			}
			log.debug("Finish connecting reviewer link of "+reviewer.getReviewerId());
			reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "utf-8"));
			String response=null;
			StringBuilder userProfileStr=new StringBuilder();
			while ((response = reader.readLine()) != null){
				if(response.contains("Page number is out of acceptable range")){
					return false;
				}
				userProfileStr.append(response);
			}
//			userProfileStr=userProfileStr.replaceAll("\\\\u003C", "<");
//			userProfileStr=userProfileStr.replaceAll("\\\\", "");
			log.debug("Start to parse reviewer "+reviewer.getReviewerId());
			boolean flag= this.crawler.getContentParser().userProfileParser(reviewer, userProfileStr);
			userProfileStr=null;
			return flag;
		}catch(Exception ex){
			log.error(ex);
		}finally{
			if(httpConn!=null){
				httpConn.disconnect();
			}
			if(reader!=null){
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					log.error(e);
				}
			}
		}
		return false;
	}
}

package crawlerEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import DAO_DB.FlushToDB;
import data.*;
import utility.DataPersistence;
import utility.SystemLogger;
import configuration.CrawlerConfig;
import crawlerAgent.Crawler;

public class OverviewEngine extends Thread implements CrawlerEngine{
	private static SystemLogger log = SystemLogger
			.getLogger(OverviewEngine.class);
	private Crawler crawler = null;
	private CommentCollector commentCrawler=null;
	private int engineToken=0;
	public OverviewEngine() {
		
	}
	public OverviewEngine(Crawler crawler) {
		this.crawler=crawler;
	}
	@Override
	public void init(int engineNum,Crawler crawler, String... para) {
		// TODO Auto-generated method stub
		this.crawler = crawler;
		this.commentCrawler=new CommentCollector(crawler);
		this.engineToken=engineNum;
		this.setName("OverviewEG_"+engineNum);
	}
	public void run() {
		while (CrawlerConfig.getInstance().getToken("OverviewEngine", engineToken)) {
			try {
				AppId appIdObj = this.crawler.getAppId();

                String appId=appIdObj.getAppId();
				if (appIdObj == null || appId==null) {
					Thread.sleep(2000);
					continue;
				}

                AppData appData=new AppData(appId);
                //read the most recent review id
                appData.setLatestReviewId(DataPersistence.getInstance().loadLatestReviewId(appId));

                appData.setRank(appIdObj.getRank());
                appData.setStoreLocation(appIdObj.getStoreLocation());
                appData.setCategory(appIdObj.getCategory());
                appData.setSubCategory(appIdObj.getSubCategory());
                appData.setCrawlerPoint(appIdObj.getCrawlerPoint());

				log.info("Start to retrieve overview of " + appId);
				HttpURLConnection httpURLConnection=this.crawler.getOverviewHttpURLConnection(appId,appData.getStoreLocation());
				if(httpURLConnection==null){
					log.error("Failed to connect the page of "+appId);
					this.crawler.addAppId(appIdObj);//save the app again
					continue;
				}else if(httpURLConnection.getResponseCode()==404||httpURLConnection.getResponseCode()==403){
					log.error("Failed to find the app of "+appId);
                    log.error(httpURLConnection.getURL());
					httpURLConnection.disconnect();
                    this.crawler.addAppId(appIdObj);//save the app again
					continue;
				}
				boolean resultStatus=this.retrieveOverview(httpURLConnection,appData);
				if(!resultStatus){//failed to parse overview
					this.crawler.addAppId(appIdObj);//save the app again
					log.error("Failed to parse overview of "+appId);
					continue;
				}
				log.info("Finish retrieving overview of " + appId);
                //retrieve comments of the app
				this.commentCrawler.run(appData);
				Comment firstComment=appData.getCommentList().peek();
				if(firstComment!=null){
					appData.setLatestReviewId(firstComment.getReviewId());//update the latest reviewId
				}
				log.info("Finish comments of "+appId+" with size "+appData.getCommentList().size());

                //save to database
                FlushToDB.getInstance().saveObject(appData);

                //save developer info for DeveloperProfileEngine
                if(CrawlerConfig.getInstance().isEngineCooperated(OverviewEngine.class,DeveloperProfileEngine.class)){
                    this.crawler.addDeveloper(new Developer(appData.getDeveloperId(), appData.getStoreLocation()));
                }
                //save reviewer info for UserProfileEngine
                if(CrawlerConfig.getInstance().isEngineCooperated(OverviewEngine.class,UserProfileEngine.class)){
                    for(Comment comment:appData.getCommentList()){
                        this.crawler.addReviewer(new Reviewer(comment.getReviewerId(), appData.getStoreLocation()));
                    }
                }
			} catch (Exception ex) {
				log.error(ex);
				ex.printStackTrace();
			}
		}
		log.info("Overview Thread exit.");
	}

	public boolean retrieveOverview(HttpURLConnection httpConn, AppData appData) {
		if(httpConn==null){
			return false;
		}
		BufferedReader reader=null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					httpConn.getInputStream(), "utf-8"));
			String response = null;
			StringBuilder sum = new StringBuilder();
			while ((response = reader.readLine()) != null) {
				sum.append( response);
			}
			boolean ret=this.crawler.getContentParser().overviewParser(sum,appData);
			sum=null;
			return ret;
		} catch (Exception ex) {
			log.error(ex);
			return false;
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
	}

}

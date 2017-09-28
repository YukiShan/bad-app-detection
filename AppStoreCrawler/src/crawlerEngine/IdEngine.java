package crawlerEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.concurrent.ConcurrentLinkedQueue;

import DAO_DB.FlushToDB;
import data.AppId;
import utility.SystemLogger;
import configuration.CrawlerConfig;
import crawlerAgent.Crawler;
import crawlerParser.ContentParser;

public class IdEngine extends Thread implements CrawlerEngine{
	private static SystemLogger log = SystemLogger.getLogger(IdEngine.class);
	private int engineToken=0;
	private Crawler crawler=null;
	public IdEngine(){
    }
	@Override
	public void init(int engineNum,Crawler crawler, String... para) {
		// TODO Auto-generated method stub
		this.crawler=crawler;
		this.engineToken=engineNum;
		this.setName("IdEG_"+engineNum);
	}
	public void run(){
		while(CrawlerConfig.getInstance().getToken("IdEngine", engineToken)){
			try{
				String sc=CrawlerConfig.getInstance().getStoreCategory();
				if(sc==null){
					Thread.sleep(2000);
					continue;
				}
				String []temp=sc.split(",");
				String storeLocation=temp[0];
				String category=temp[1];
				String crawlerPoint=temp[2];
				log.info("IdCrawler starts to search category="+category+" store location="+storeLocation+", crawler point="+crawlerPoint+" ...");
                this.retrieveAppPages(category,storeLocation,crawlerPoint);
			}catch(Exception ex){
				log.error(ex.getCause());
				ex.printStackTrace();
			}
		}
	}
    //ugly code
    private void retrieveAppPages(String category,String storeLocation,String crawlerPoint){
        int flag=2;
        int index=0;
        while(flag>0){
            flag=2;
            if(crawlerPoint.equals("1")){//crawler from websites
                int index2=0;
                while(flag>1){
                    HttpURLConnection httpConn = crawler.getIdHttpURLConnection(crawlerPoint, category, storeLocation,""+(index),index2+"");
                    if(httpConn==null){//try it again
                        httpConn = crawler.getIdHttpURLConnection(crawlerPoint, category, storeLocation,index+"",index2+"");
                        if (httpConn == null) {
                            log.error("Failed to crawl " + category + "-" + storeLocation + "-" + crawlerPoint);
                            break;
                        }
                    }
                    flag=this.retrieveAppId(httpConn,category,storeLocation,crawlerPoint);
                    index2++;
                }
            }else {
                HttpURLConnection httpConn = crawler.getIdHttpURLConnection(crawlerPoint, category, index + "", storeLocation);
                if (httpConn == null) {
                    httpConn = crawler.getIdHttpURLConnection(crawlerPoint, category, index + "", storeLocation);
                    if (httpConn == null) {
                        log.error("Failed to crawl " + category + "-" + storeLocation + "-" + crawlerPoint);
                        break;
                    }
                }
                flag = this.retrieveAppId(httpConn, category, storeLocation, crawlerPoint);
            }
            index++;
        }
        return;
    }
	private int retrieveAppId(HttpURLConnection httpConn,String category,String storeLocation,String crawlerPoint){
		BufferedReader reader=null;
		if(httpConn==null){
			return 0;
		}
		try{
			reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "utf-8"));
			String response=null;
			StringBuilder sum=new StringBuilder();
			while ((response = reader.readLine()) != null){
				sum.append(response);
			}
//			sum=sum.replaceAll("\\\\u003C", "<");
//			sum=sum.replaceAll("\\\\", "");
			ContentParser cp=this.crawler.getContentParser();
            ConcurrentLinkedQueue<AppId> appIdQueue=new ConcurrentLinkedQueue<AppId>();
            int flag= cp.idParser(appIdQueue,sum,category,storeLocation,crawlerPoint);
            //save to db thread and waiting for flush
            FlushToDB.getInstance().saveObjects(appIdQueue);
            //save to appIdList for crawling complete information
            if(CrawlerConfig.getInstance().isEngineCooperated(IdEngine.class,OverviewEngine.class)){
                this.crawler.addAppIds(appIdQueue);
            }
            appIdQueue.clear();
			return flag;
		}catch(Exception ex){
			log.error(ex);
			return 0;
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

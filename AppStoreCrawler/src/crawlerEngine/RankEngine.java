package crawlerEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.concurrent.ConcurrentLinkedQueue;

import utility.DataPersistence;
import utility.SystemLogger;
import configuration.CrawlerConfig;
import crawlerAgent.Crawler;
import data.AppRatingInfo;

public class RankEngine extends Thread implements CrawlerEngine{

	private static SystemLogger log = SystemLogger.getLogger(RankEngine.class);
	private ConcurrentLinkedQueue<AppRatingInfo> ariList=null;
	private Crawler crawler=null;
	private int engineToken;
	public RankEngine(){
		
	}
	@Override
	public void init(int engineNum,Crawler crawler, String... para) {
		// TODO Auto-generated method stub
		this.crawler=crawler;
		this.ariList=new ConcurrentLinkedQueue<AppRatingInfo>();
		this.engineToken=engineNum;
		this.setName("RankEG_"+engineNum);
	}
	public void run(){
		while(CrawlerConfig.getInstance().getToken("RankEngine", engineToken)){
			try{
				String sc=CrawlerConfig.getInstance().getStoreCategory();
				if(sc==null){
					Thread.sleep(60000);
					continue;
				}
				String []temp=sc.split(",");
				if(temp.length<3){
					continue;
				}
				String storeLocation=temp[0];
				String category=temp[1];
				String crawlerPoint=temp[2];
				log.info("Rank crawler starts to searching category="+category+", store location="+storeLocation+", crawler point="+crawlerPoint+" ...");
				boolean flag=true;
				int currentPageNum=0;
				while(flag){
					flag=this.retrieveAppRating(this.crawler.getRankHttpURLConnection(category,currentPageNum+"",storeLocation,crawlerPoint),category,storeLocation,crawlerPoint);
					currentPageNum++;
					AppRatingInfo ari=null;
					while(true){
						ari=this.ariList.poll();
						if(ari==null){
							break;
						}else{
							DataPersistence.getInstance().saveRatingsToDB(ari);
						}
					}
//					Thread.sleep(200);
				}
			}catch(Exception ex){
				log.error(ex);
			}
		}
	}
	private boolean retrieveAppRating(HttpURLConnection httpConn,String category,String storeLocation, String crawlerPoint){
		if(httpConn==null){
			return false;
		}
		BufferedReader reader=null;
		try{
			
			reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "utf-8"));
			String response=null;
			StringBuilder sum=new StringBuilder();
			while ((response = reader.readLine()) != null){
				sum.append(response);
			}
//			sum=sum.replaceAll("\\\\u003C", "<");
//			sum=sum.replaceAll("\\\\", "");
			boolean flag=this.crawler.getContentParser().RankParser(this.ariList,sum,category,storeLocation,crawlerPoint);
			sum=null;
			return flag;
		}catch(Exception ex){
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

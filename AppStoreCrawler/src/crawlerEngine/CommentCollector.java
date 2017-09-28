package crawlerEngine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.concurrent.ConcurrentLinkedQueue;

import crawlerAgent.Crawler;

import utility.SystemLogger;
import data.AppData;
import data.Comment;

public class CommentCollector {
	private SystemLogger log = SystemLogger.getLogger(CommentCollector.class);
	private Crawler crawler=null;
	public CommentCollector(Crawler crawler){
		this.crawler=crawler;
	}
	public void run(AppData appData){
		try{
			if(appData==null || appData.getAppId() ==null){
				return;
			}else{
				log.debug("Start to retrieve comments of "+appData.getAppId());
				this.retrieveComments(appData);
				log.debug("Finish retrieving comments of "+appData.getAppId());

			}
		}catch(Exception ex){
			log.error(ex);
		}
	}
	
	public void retrieveComments(AppData appData){
        boolean flag=false;
        int pageNum=0;
        do{
            flag=retrieveOnePage(appData,pageNum++);
        }while(flag);
	}
	public boolean retrieveOnePage(AppData appData, int pageNum){
		
		try {
			HttpURLConnection httpConn = this.crawler.getCommentHttpURLConnection(appData.getAppId(),appData.getStoreLocation(),pageNum+"");
			if(httpConn==null){
				return true;//skip this page
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "utf-8"));
			String response=null;
			StringBuilder sum=new StringBuilder();
			while ((response = reader.readLine()) != null){
				if(response.contains("Page number is out of acceptable range")){
					return false;
				}
				sum.append(response);
			}
			boolean flag=this.crawler.getContentParser().CommentsParser(sum,pageNum,appData);
			return flag;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
			e.printStackTrace();
			return false;
		}
	}
}

package crawlerEngine;

import DAO_DB.FlushToDB;
import configuration.CrawlerConfig;
import crawlerAgent.Crawler;
import crawlerParser.ContentParser;
import data.AppId;
import data.Developer;
import utility.SystemLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashSet;

/**
 * Created by xiezhen on 5/31/2014.
 */
public class DeveloperProfileEngine extends Thread implements CrawlerEngine {
    private SystemLogger log = SystemLogger.getLogger(DeveloperProfileEngine.class);
    private int engineToken=0;
    private Crawler crawler =null;
    public DeveloperProfileEngine(){}

    @Override
    public void init(int engineNum, Crawler crawler, String... para) {
        this.engineToken=engineNum;
        this.crawler =crawler;
        this.setName("DeveloperProfileEG_"+engineNum);
    }

    public void run(){
        while(CrawlerConfig.getInstance().getToken("DeveloperProfileEngine", engineToken)) {
            try {
                Developer developer=this.crawler.getDeveloper();
                if(developer==null){//no developer to be crawled
                    log.info("No developer for crawling, sleep 2 seconds");
                    Thread.sleep(2000);
                    continue;
                }
                log.info("Start to crawl developer "+developer.getDeveloperId());
                if(!developer.isReady()){
                    log.error(developer.getDeveloperId() +" misses important information like store location.");
                    continue;
                }
                this.retrieveDeveloperProfile(developer);
                log.info("End of crawling developer "+developer.getDeveloperId());

                if(!developer.isEmpty()) {
                    //save to db
                    FlushToDB.getInstance().saveObject(developer);
                    //transfer appId to OverviewEngine
                    if(CrawlerConfig.getInstance().isEngineCooperated(Developer.class,OverviewEngine.class)){
                        for(HashSet<String> apps:developer.getAppList().values()){
                            for(String appId:apps){
                                this.crawler.addAppId(new AppId(appId,null,null,developer.getStoreLocation(),developer.getCrawlerPoint()));
                            }
                        }
                    }
                }else{
                    log.error("Developer "+developer.getDeveloperId()+" is empty!");
                }

            } catch (Exception ex) {
                log.error(ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void retrieveDeveloperProfile(Developer developer){
        int iphonePageNum=1;
        int ipadPageNum=1;
        boolean ret=false;
        do{
            ret=retrieveOnePage(developer,developer.getDeveloperId(),developer.getStoreLocation(),iphonePageNum,ipadPageNum);
            iphonePageNum++;
            ipadPageNum++;
            //mac?
        }while(ret);
    }
    private boolean retrieveOnePage(Developer developer,String developerId,String storeLocation,int iphonePageNum,int ipadPageNum){
        if(developer!=null && developerId==null||storeLocation==null || iphonePageNum<=0 || ipadPageNum<=0){
            log.error("Parameters error!");
            return false;
        }
        boolean ret=false;
        String para[]=new String[4];
        para[0]=developerId;
        para[1]=storeLocation;
        para[2]=iphonePageNum+"";
        para[3]=ipadPageNum+"";
        HttpURLConnection httpConn =null;
        BufferedReader bufferedReader=null;
        try {
            httpConn = this.crawler.getDeveloperProfileHttpURLConnection(para);
            if (httpConn == null) {
                httpConn = this.crawler.getDeveloperProfileHttpURLConnection(para);//try again
                if (httpConn == null) {
                    return false;
                }
            }
            bufferedReader=new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            String response=null;
            StringBuilder content=new StringBuilder();
            while((response=bufferedReader.readLine())!=null){
                content.append(response);
            }
            ContentParser cp=this.crawler.getContentParser();
            if(cp!=null){
                ret=cp.developerProfileParser(developer,content);
            }
        }catch (Exception ex){
            log.error(ex);
        }finally{
            if(bufferedReader!=null){
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
            if(httpConn!=null){
                httpConn.disconnect();
            }
        }
        return ret;
    }
}

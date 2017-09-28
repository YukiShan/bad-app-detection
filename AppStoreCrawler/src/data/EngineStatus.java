package data;

import java.util.concurrent.ConcurrentHashMap;

import utility.DataPersistence;
import utility.SystemLogger;
import crawlerAgent.Crawler;
import crawlerEngine.CrawlerEngine;

/**
 * This class is used to save status of each engine.
 * @author Shanshan
 *
 */
public class EngineStatus {
	private String engineName=null;
	private volatile int totalTokens=0;//the maximum tokens this engine has, it decides the number of concurrent threads
	private int currentTokens=0;//the index of current used tokens
	private int status=0;//0-inactive 1-running 2-on checking
	private String []para=null;
	private static SystemLogger log = SystemLogger.getLogger(EngineStatus.class);
	private ConcurrentHashMap<Integer,CrawlerEngine> threadMap=null;
	private Crawler crawler=null;
	public EngineStatus(String name){
		this.engineName=name;
		threadMap=new ConcurrentHashMap<Integer,CrawlerEngine>();
//		this.init(tokens, para);
		this.crawler=(Crawler)DataPersistence.getInstance().getCrawler();
	}
	//initialization
	public void init(int tokens,String ...para){
		try{
			this.totalTokens=tokens;
            if(this.totalTokens<=0){
                this.status=0;//set it inactive
            }else{
                this.status=1;
            }
			this.para=para;
			for(currentTokens=0;currentTokens<this.totalTokens;currentTokens++){
				CrawlerEngine engine=(CrawlerEngine) Class.forName("crawlerEngine."+this.engineName).newInstance();
				engine.init(currentTokens,this.crawler, para);
				engine.start();
				threadMap.put(currentTokens, engine);
			}
		}catch(Exception ex){
			log.error(ex);
		}
	}
	//parameters update
	public void update(int tokens,String ...para){
		try{
			this.totalTokens=tokens;
            if(this.totalTokens<=0){
                this.status=0;//set it inactive
            }else{
                this.status=1;
            }
			this.para=para;
			//create new thread
			if(totalTokens>currentTokens){
				log.info("Thread increased:"+this.toString());
			}
			for(;currentTokens<this.totalTokens;currentTokens++){
				CrawlerEngine engine=(CrawlerEngine) Class.forName("crawlerEngine."+this.engineName).newInstance();
				engine.init(currentTokens,this.crawler, para);
				engine.start();
				threadMap.put(currentTokens, engine);
			}
		}catch(Exception ex){
			log.error(ex);
		}
	}
	public synchronized boolean getToken(int token){
		if(token>=this.totalTokens){//the higher token has be removed, and stop the thread
			this.currentTokens--;
			return false;
		}
		return true;
	}
	public String getEngineName() {
		return engineName;
	}

	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	public boolean isRunning(){
        return this.status==1;
    }


	@Override
	public String toString() {
		return "EngineStatus [engineName=" + engineName + ", totalTokens="
				+ totalTokens + ", currentTokens=" + currentTokens
				+  ", status=" + status + "]";
	}
	public static void main(String ...args){
		CrawlerEngine engine;
		try {
			engine = (CrawlerEngine) Class.forName("crawlerEngine.IdEngine").newInstance();
			engine.start();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}

package configuration;

import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Properties;


import org.apache.log4j.PropertyConfigurator;
import utility.MyProxySelector;
import utility.SystemLogger;
import DAO_DB.DataControl;
import DAO_DB.MessageManager;

public abstract class BaseConfig {
	protected static SystemLogger log = SystemLogger.getLogger(BaseConfig.class);

	protected Properties properties=null;
	private Thread loadingThread=null;
	/* 0-not initialized, >1 update version*/
	private int configVersion=0;
	
	private MessageManager messageManager=null;
	private MyProxySelector myProxySelector=null;

    private String crawlerName;

	protected boolean isDynamicLoading=false;
    private volatile int dbFlusherThreadNum=0;

    protected BaseConfig (){
		this.init();
	}
	
	private void init(){
		try{
            String log4jPath="conf/log4j.properties";

            PropertyConfigurator.configure(log4jPath);
			loadConfigrationFile();
			if(!this.getDbMode().equalsIgnoreCase("none")){
				if (!DataControl.isInitialized()) {
					DataControl.init(properties);
				}
				messageManager = (MessageManager) DataControl
						.getDataManager(MessageManager.class.getName());
			}
			if(this.isProxyMode()){
				if(myProxySelector==null){
					myProxySelector=new MyProxySelector(null);
				}
				myProxySelector.init(properties);//init proxy
			}

		}catch(Exception ex){
			log.error(ex.getMessage());
			System.exit(0);
		}
		
		loadingThread=new Thread(new Runnable(){
			public void run(){
				while(true){
                    try {
                        if(isDynamicLoading) {
                            long dynamicLoadingWaitingTime = Integer.parseInt(properties
                                    .getProperty("dynamic_loading_interval", "60000").trim());
                            Thread.sleep(dynamicLoadingWaitingTime);
                            loadConfigrationFile();
                            dynamicLoading();
                        }else{
                            Thread.sleep(60000);//sleep 1 min
                        }
                    } catch (Exception ex) {
                        log.error(ex.getMessage());
                    }
				}

				
			}
		});
		loadingThread.setName("CF_Loading");
		loadingThread.start();
	}
	protected void loadConfigrationFile(){
		try {
				FileInputStream fis = null;
				String propFile = "conf/database.properties";
				Properties properties = new Properties();
				fis = new FileInputStream(propFile);
				properties.load(fis);
				fis.close();
				if(this.properties==null||!this.properties.equals(properties)){
					this.properties=properties;
					this.configVersion++;
					log.debug("Configuration updated to " + this.properties.toString());
                    this.isDynamicLoading=Boolean.parseBoolean(properties.getProperty("is_dynamic_loading",
                            "false").trim());
                    this.dbFlusherThreadNum=Integer.parseInt(properties.getProperty("db_flusher_thread_num","0").trim());
                    this.crawlerName = properties.getProperty("Crawler_name",
                            "crawler").trim();
				}
		} catch (Exception e) {
			log.error(e);
		}
	}
	/*
	 * Interfaces
	 */
	abstract protected void dynamicLoading();
	abstract public HashSet<String> getDateFormates();
	public int getConfigVersion() {
		return configVersion;
	}
	public String getDbMode(){
		return this.properties.getProperty("db_type", "none").trim();
	}
	public boolean isProxyMode(){
		try{
			return Boolean.valueOf(properties.getProperty("https_proxy","false"));
		}catch(Exception ex){
			log.error(ex.getMessage());
		}
		return false;

	}
	public MessageManager getMessageManager() {
		return messageManager;
	}
	public MyProxySelector getMyProxySelector() {
		return myProxySelector;
	}

    public int getDbFlusherThreadNum() {
        return this.dbFlusherThreadNum;
    }
    public String getCrawlerName() {
        return crawlerName;
    }

}

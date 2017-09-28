package configuration;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import crawlerEngine.CrawlerEngine;
import data.EngineStatus;

public class CrawlerConfig extends BaseConfig {
	private static CrawlerConfig config;
	// stories and categories
	private ArrayList<String> stores = null;
	private ArrayList<String> categories = null;
	private ArrayList<String> crawlerPoints = null;
	private ArrayList<String> idEngineStoreList = null;
	// store the location and category <store_category_crawlerPoint>
	private ConcurrentLinkedQueue<String> storeCategory = null;
	// store the store whose app overview and comments needed to be crawled
	private ConcurrentLinkedQueue<String> idEngineStoreQueue = null;
	// the hashmap of engine status
	private ConcurrentHashMap<String, EngineStatus> engineStatusMapping = null;
    private ConcurrentHashMap<String,HashSet<String>> engineWorkModes=null;
    private boolean isEngineStarted=false;
    private  String dateFormats=null;
    private String tbNameAppData="AppData";
    private String tbNameComment="Comment";
    private String tbNameReviewers="Reviewers";
    private  String tbNameAppIds="AppIds";
    private String tbNameDevelopers="Developers";
    private String tbNamePopularApps="PopularApps";

    private CrawlerConfig() {
		this.init();
	}

	public static CrawlerConfig getInstance() {
		// TODO Auto-generated method stub
		if (config == null) {
			config = new CrawlerConfig();
		}
		return config;
	}

	// load the configuration of database
	private void init() {
		try {
			this.engineStatusMapping = new ConcurrentHashMap<String, EngineStatus>();
			this.storeCategory = new ConcurrentLinkedQueue<String>();
			this.idEngineStoreQueue = new ConcurrentLinkedQueue<String>();
            this.engineWorkModes=new ConcurrentHashMap<String, HashSet<String>>();
			// init category
            this.dateFormats=super.properties.getProperty("date_format","").trim();

            this.stores = this.getStoreLocations(properties);
			this.categories = this.getCategories(properties);
			this.crawlerPoints = this.getCrawlerPoint(properties);
			this.idEngineStoreList = this.getIdEngineStoreList(properties);
			this.idEngineStoreQueue.addAll(this.idEngineStoreList);
            this.loadDbTableNames();
			for (String category : categories) {
                    for (String cp : crawlerPoints) {
                        for (String store : stores) {
                            String key = store + "," + category + "," + cp;
                            if (!storeCategory.contains(key)) {
                                storeCategory.add(key);
                            }
                        }
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
			log.error("Fail to initialize crawler engine.");
			System.exit(0);
		}
	}

    //start to load engines
    public void loadEngine(){
        try {
            //load engine work mode
            ConcurrentHashMap<String,HashSet<String>> engineWorkModesTemp=new ConcurrentHashMap<String,HashSet<String>>();
            String engineWorkModeStr = properties.getProperty("engine_work_mode", "");
            if (engineWorkModeStr.length() > 0) {
                String[] workModes = engineWorkModeStr.split(",");
                for (String workMode : workModes) {
                    String[] engines = workMode.split("->");
                    if (engines.length == 2) {
                        HashSet<String> endEngines = null;
                        if (engineWorkModesTemp.containsKey(engines[0])) {
                            endEngines = engineWorkModesTemp.get(engines[0]);
                        } else {
                            endEngines = new HashSet<String>();
                            engineWorkModesTemp.put(engines[0], endEngines);
                        }
                        endEngines.add(engines[1]);
                    }
                }
                synchronized (this.engineWorkModes){//update the engine mode
                    this.engineWorkModes=engineWorkModesTemp;
                }
            }
            //load engine status
            for (Enumeration e = properties.propertyNames(); e
                    .hasMoreElements(); ) {
                String key = (String) e.nextElement();
                String para = null;
                if (key.matches("[A-Z][a-zA-Z]*Engine")) {
                    para = properties.getProperty(key);
                    String[] temp = para.split(",");
                    int tokens = Integer.parseInt(temp[0]);
                    if (engineStatusMapping.containsKey(key)) {
                        engineStatusMapping.get(key).update(tokens,
                                para);
                    } else {// newly added engine
                        EngineStatus engineStatus = new EngineStatus(
                                key);
                        engineStatusMapping.put(key, engineStatus);
                        engineStatus.init(tokens, para);
                    }
                }
            }

            this.isEngineStarted = true;
        }catch (Exception ex){
            log.error(ex);
            ex.printStackTrace();
        }
    }

	// load the configuration periodically
	// TODO what if the engined has been removed
	protected void dynamicLoading() {
		try {
            if(this.isEngineStarted){
                loadEngine();
            }
			// update location
			updateStoreLocations();
			updateIdEngineStore();
			this.loadDbTableNames();
		} catch (Exception ex) {
			log.error(ex);
		}
	}

	// /////////////// load configuration from local file
	/**
	 * Load the categories (also called crawler entry point) from local file
	 * 
	 * @return the array list of the categories
	 */
	private ArrayList<String> getCategories(Properties propertiesTemp) {
		String category = propertiesTemp.getProperty("Category", "36");// 36-all
																		// category
		String[] cates = category.trim().split(",");
		ArrayList<String> cateList = new ArrayList<String>();
		for (int i = 0; i < cates.length; i++) {
			if (!"".equalsIgnoreCase(cates[i])) {
				cateList.add(cates[i]);
			}
		}
		return cateList;
	}

	/**
	 * Load the store location from local configuration file
	 * 
	 * @return the array list of the store locations
	 */
	private ArrayList<String> getStoreLocations(Properties propertiesTemp) {
		String storeLocations = propertiesTemp.getProperty("Store_location",
				"NA").trim();// 143465-Chinese market
		String[] slArray = storeLocations.trim().split(",");
		ArrayList<String> slList = new ArrayList<String>();
		for (int i = 0; i < slArray.length; i++) {
			if (!"".equalsIgnoreCase(slArray[i])) {
				slList.add(slArray[i]);
			}
		}
		return slList;
	}

	private ArrayList<String> getCrawlerPoint(Properties propertiesTemp) {
		String storeLocations = propertiesTemp.getProperty("Crawler_point",
				"27").trim();// 27-free
		String[] slArray = storeLocations.trim().split(",");
		ArrayList<String> slList = new ArrayList<String>();
		for (int i = 0; i < slArray.length; i++) {
			if (!"".equalsIgnoreCase(slArray[i])) {
				slList.add(slArray[i]);
			}
		}
		return slList;
	}

	private ArrayList<String> getIdEngineStoreList(Properties propertiesTemp) {
		String storeLocations = propertiesTemp.getProperty(
				"IdEngine_store_location", "143441").trim();// 143441-US
		String[] slArray = storeLocations.trim().split(",");
		ArrayList<String> slList = new ArrayList<String>();
		for (int i = 0; i < slArray.length; i++) {
			if (!"".equalsIgnoreCase(slArray[i])) {
				slList.add(slArray[i]);
			}
		}
		return slList;
	}

	// update stores and categories
	private void updateStoreLocations() {
		ArrayList<String> storeTemp = this.getStoreLocations(properties);
		ArrayList<String> categoryTemp = this.getCategories(properties);
		ArrayList<String> crawlerPointsTemp = this.getCrawlerPoint(properties);

		ArrayList<String> storeNew = new ArrayList<String>();
		ArrayList<String> categoryNew = new ArrayList<String>();
		ArrayList<String> crawlerPointsNew = new ArrayList<String>();
		for (String store : storeTemp) {
			if (!stores.contains(store)) {
				storeNew.add(store);
			}
		}
		for (String cate : categoryTemp) {
			if (!categories.contains(cate)) {
				categoryNew.add(cate);
			}
		}
		for (String cp : crawlerPointsTemp) {
			if (!crawlerPoints.contains(cp)) {
				crawlerPointsNew.add(cp);
			}
		}
		if (categoryNew.size() != 0) {
			log.info("New categories added: " + categoryNew.toString());
			for (String category : categoryNew) {
				for (String cp : crawlerPointsTemp) {
					for (String store : storeTemp) {
						String key = store + "," + category + "," + cp;
						if (!storeCategory.contains(key)) {
							storeCategory.add(key);
						}
					}
				}
			}
		}
		if (storeNew.size() != 0) {
			log.info("New stores added: " + storeNew.toString());
			// add the new stores and categories to the tail of queue
			for (String category : categoryTemp) {
				for (String cp : crawlerPointsTemp) {
					for (String store : storeNew) {
						String key = store + "," + category + "," + cp;
						if (!storeCategory.contains(key)) {
							storeCategory.add(key);
						}
					}

				}
			}
		}

		if (crawlerPointsNew.size() != 0) {
			log.info("New crawler points added: " + crawlerPointsNew.toString());
			for (String cp : crawlerPointsNew) {
				for (String category : categoryTemp) {
					for (String store : storeTemp) {
						String key = store + "," + category + "," + cp;
						if (!storeCategory.contains(key)) {
							storeCategory.add(key);
						}
					}
				}
			}
		}
		stores = storeTemp;
		categories = categoryTemp;
		crawlerPoints = crawlerPointsTemp;
	}

	private void updateIdEngineStore() {
		ArrayList<String> idTaskListTemp = this
				.getIdEngineStoreList(properties);
		for (String store : idTaskListTemp) {
			if (!this.idEngineStoreList.contains(store)) {
				this.idEngineStoreQueue.add(store);
			}
		}
		this.idEngineStoreList = idTaskListTemp;
	}

	public String getStoreCategory() {
		String sc = this.storeCategory.poll();
		if (sc == null) {// start over again
			try {
				log.info("start over again, sleep for 24 hours");
				Thread.sleep(24 * 3600000);// sleep 24 hours
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for (String category : categories) {
				for (String cp : crawlerPoints) {
					for (String store : stores) {
						String key = store + "," + category + "," + cp;
						if (!storeCategory.contains(key)) {
							storeCategory.add(key);
						}
					}
				}
			}
			sc = this.storeCategory.poll();
		}
		return sc;
	}
	public boolean getToken(String className,int token){
		if(this.engineStatusMapping.containsKey(className)){
			return engineStatusMapping.get(className).getToken(token);
		}
		return false;
	}
    /**
     * assert whether CrawlerEngine A should feed CrawlerEngine B
     * @param A - engine name
     * @param B - engine name
     * @return true- if A-> B, otherwise, false
     */
    public boolean isEngineCooperated(Class A, Class B){
        String engineA=A.getSimpleName();
        String engineB=B.getSimpleName();
        if(this.engineWorkModes.containsKey(engineA)){
            if(this.engineWorkModes.get(engineA).contains(engineB)){
                EngineStatus esA=engineStatusMapping.get(engineA);
                EngineStatus esB=engineStatusMapping.get(engineB);
                if(esA!=null && esB!=null && esA.isRunning() && esB.isRunning()){
                    return true;
                }
            }
        }
        return false;
    }
    private void loadDbTableNames(){

        this.tbNameAppData = properties.getProperty("DB_app_tb",
                "AppData").trim();
        this.tbNameComment = properties.getProperty("DB_comment_tb",
                "Comment").trim();
        this.tbNameReviewers = properties.getProperty("DB_reviewers_tb",
                "Reviewers").trim();
        this.tbNameAppIds = properties.getProperty("DB_appids_tb",
                "AppIds").trim();
        this.tbNameDevelopers = properties.getProperty("DB_developers_tb",
                "Developers").trim();
        this.tbNamePopularApps=super.properties.getProperty("LS_DB_popular_apps_tb", "PopularApps");

    }
    public HashSet<String> getDateFormates(){
        HashSet<String> hs=new HashSet<String>();
        Pattern p=Pattern.compile("\\[.+?\\]");
        Matcher matcher = p.matcher(this.dateFormats);
        while(matcher.find())
        {
            String str=matcher.group(0).trim();
            hs.add(str.substring(1, str.length()-1));
        }
        return hs;
    }

    public String getTbNameAppData() {
        return tbNameAppData;
    }

    public String getTbNameComment() {
        return tbNameComment;
    }

    public String getTbNameReviewers() {
        return tbNameReviewers;
    }
    public String getTbNamePopularApps() {
        return tbNamePopularApps;
    }


    public void setTbNameAppData(String tbNameAppData) {
        this.tbNameAppData = tbNameAppData;
    }

    public void setTbNameComment(String tbNameComment) {
        this.tbNameComment = tbNameComment;
    }

    public void setTbNameReviewers(String tbNameReviewers) {
        this.tbNameReviewers = tbNameReviewers;
    }
}

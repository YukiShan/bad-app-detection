import configuration.BaseConfig;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LSConfig  extends BaseConfig {

	private volatile int threadCount=20;
    private int ls_type =1; //1- preload from DB, 2-on the fly
	/*Model parameters See large-scale paper Table I*/

	private float thresholdReviewSize=100;//the minimum reviews of one customer
	private float thresholdAppSize=2;//
	private float thresholdTopRatedApps=20;//the maximal number of apps to be considered
	private float thresholdInterval=4;//the minimum weeks that denote separate groups
	private float damageImpact=300;
	private float damageImpactLargest=600;
	private float largestReviewers=3000;
	private float poplarAppReviewerSize=15000;
	private String seed;
	private String seedStoreLocation;
//	private String seedCrawlerPoint;
	private float appStatusThreshold;
	private float proofThreshold;
	private float alpha;
	private int dataSource=1;
	private String dateFormats=null;
	private int switchFLRHOR=0;
    private String presetSeedFile =null;
	private static LSConfig config;
	public static LSConfig getInst() {
		// TODO Auto-generated method stub
		if (config == null) {
			config = new LSConfig();
			config.init();
		}
		return config;
	}

	@Override
	protected void dynamicLoading() {
		// TODO Auto-generated method stub
	}
	
	protected void init(){
		try{
			this.threadCount=Integer.parseInt(super.properties.getProperty("LS_R_th_count","20").trim());  
			String seedStr=super.properties.getProperty("LS_seed","525378313,143465").trim();
			String []seedStrs=seedStr.split(",");
			if(seedStrs.length>=2){
				this.seed=seedStrs[0].trim();
				this.seedStoreLocation=seedStrs[1].trim();
			}else if(seedStrs.length>=1){
				this.seed=seedStrs[0].trim();
			}
            this.presetSeedFile =super.properties.getProperty("LS_seed_file","").trim();
            this.ls_type =Integer.parseInt(super.properties.getProperty("LS_type","2").trim());

            this.dataSource=Integer.parseInt(super.properties.getProperty("data_source","1").trim());
			this.dateFormats=super.properties.getProperty("LS_date_formate","").trim();
			this.alpha=Float.parseFloat(super.properties.getProperty("LS_alpha","0.4").trim());  
			this.thresholdReviewSize=Integer.parseInt(super.properties.getProperty("LS_N_b","100").trim());  
			this.thresholdAppSize=Integer.parseInt(super.properties.getProperty("LS_N_a","2").trim());  
			this.thresholdTopRatedApps=Integer.parseInt(super.properties.getProperty("LS_N_ta","20").trim());  
			this.thresholdInterval=Integer.parseInt(super.properties.getProperty("LS_Th_i","4").trim());  
			this.damageImpact=Integer.parseInt(super.properties.getProperty("LS_DI_s","300").trim());  
			this.damageImpactLargest=Integer.parseInt(super.properties.getProperty("LS_DI_m","600").trim());  
			this.largestReviewers=Integer.parseInt(super.properties.getProperty("LS_N_r","3000").trim());  
			this.poplarAppReviewerSize=Integer.parseInt(super.properties.getProperty("LS_N_p","15000").trim());  
			this.appStatusThreshold=Float.parseFloat(super.properties.getProperty("LS_Th_s","0.25").trim());  
			this.proofThreshold=Float.parseFloat(super.properties.getProperty("LS_Th_p","2").trim());  
			this.switchFLRHOR=Integer.parseInt(super.properties.getProperty("LS_HLR_FOR_switch","0").trim());  

		}catch(Exception ex){
			log.error(ex.getMessage());
			System.exit(0);
		}
	}
	public void refreshConfig(){
		this.alpha=Float.parseFloat(super.properties.getProperty("LS_alpha","0.4").trim());  
		this.thresholdReviewSize=Float.parseFloat(super.properties.getProperty("LS_N_b","100").trim());  
		this.thresholdAppSize=Float.parseFloat(super.properties.getProperty("LS_N_a","2").trim());  
		this.thresholdTopRatedApps=Float.parseFloat(super.properties.getProperty("LS_N_ta","20").trim());  
		this.thresholdInterval=Float.parseFloat(super.properties.getProperty("LS_Th_i","4").trim());  
		this.damageImpact=Float.parseFloat(super.properties.getProperty("LS_DI_s","300").trim());  
		this.damageImpactLargest=Float.parseFloat(super.properties.getProperty("LS_DI_m","600").trim());  
		this.largestReviewers=Float.parseFloat(super.properties.getProperty("LS_N_r","3000").trim());  
		this.poplarAppReviewerSize=Float.parseFloat(super.properties.getProperty("LS_N_p","15000").trim());  
		this.appStatusThreshold=Float.parseFloat(super.properties.getProperty("LS_Th_s","0.25").trim());  
		this.proofThreshold=Float.parseFloat(super.properties.getProperty("LS_Th_p","2").trim()); 
	}
	public void refreshProperties(){
		this.alpha=Float.parseFloat(super.properties.getProperty("LS_alpha","0.4").trim());
		this.thresholdReviewSize=Integer.parseInt(super.properties.getProperty("LS_N_b","100").trim());  
		this.thresholdAppSize=Integer.parseInt(super.properties.getProperty("LS_N_a","2").trim());  
		this.thresholdTopRatedApps=Integer.parseInt(super.properties.getProperty("LS_N_ta","20").trim());  
		this.thresholdInterval=Integer.parseInt(super.properties.getProperty("LS_Th_i","4").trim());  
		this.damageImpact=Integer.parseInt(super.properties.getProperty("LS_DI_s","300").trim());  
		this.damageImpactLargest=Integer.parseInt(super.properties.getProperty("LS_DI_m","600").trim());  
		this.largestReviewers=Integer.parseInt(super.properties.getProperty("LS_N_r","3000").trim());  
		this.poplarAppReviewerSize=Integer.parseInt(super.properties.getProperty("LS_N_p","15000").trim());  
		this.appStatusThreshold=Float.parseFloat(super.properties.getProperty("LS_Th_s","0.25").trim());  
		this.proofThreshold=Float.parseFloat(super.properties.getProperty("LS_Th_p","2").trim()); 

	}
	//getters
	public int getThreadCount() {
		return threadCount;
	}

	public String getSeed() {
		return seed;
	}

	public String getSeedStoreLocation() {
		return seedStoreLocation;
	}

	/*public String getSeedCrawlerPoint() {
		return seedCrawlerPoint;
	}*/

	public float getAppStatusThreshold() {
		return appStatusThreshold;
	}

	public float getProofThreshold() {
		return proofThreshold;
	}

	public float getAlpha() {
		return alpha;
	}

	public float getThresholdReviewSize() {
		return thresholdReviewSize;
	}

	public float getThresholdAppSize() {
		return thresholdAppSize;
	}

	public float getThresholdTopRatedApps() {
		return thresholdTopRatedApps;
	}

	public float getThresholdInterval() {
		return thresholdInterval;
	}

	public float getDamageImpact() {
		return damageImpact;
	}

	public float getDamageImpactLargest() {
		return damageImpactLargest;
	}

	public float getLargestReviewers() {
		return largestReviewers;
	}

	public float getPoplarAppReviewerSize() {
		return poplarAppReviewerSize;
	}

	public static LSConfig getConfig() {
		return config;
	}

	public boolean isAllowWebpage(){
		return this.dataSource==2;
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


	public boolean getSwitchFLRHOR() {
		return switchFLRHOR==1;
	}

	public void setSwitchFLRHOR(int switchFLRHOR) {
		this.switchFLRHOR = switchFLRHOR;
	}
    public boolean isPreload(){return this.ls_type ==1;}
    public String getPresetSeedFile(){
        return this.presetSeedFile;
    }
}

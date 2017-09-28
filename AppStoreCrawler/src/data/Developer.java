package data;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Shanshan on 10/31/2016.
 */
public class Developer {
    private String developerName=null; // the name of the developer
    private String developerId=null;     // the account id assigned by app store vendors
    private String storeLocation=null;   // store location

    private String crawlerPoint=null;   //from where
    //<type, app_list> type= iphone, ipad, mac, android
    private ConcurrentHashMap<String,HashSet<String>> appList=null;
    public Developer(){}
    public Developer(String developerId,String storeLocation){
        this.developerId=developerId;
        this.storeLocation=storeLocation;
    }
    // check if the information needed have been set.
    public boolean isReady(){
        return developerId!=null && storeLocation!=null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Developer developer = (Developer) o;

        if (!developerId.equals(developer.developerId)) return false;
        if (!storeLocation.equals(developer.storeLocation)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = developerId.hashCode();
        result = 31 * result + storeLocation.hashCode();
        return result;
    }

    public String getDeveloperName() {
        return developerName;
    }

    public void setDeveloperName(String developerName) {
        this.developerName = developerName;
    }

    public String getDeveloperId() {
        return developerId;
    }

    public void setDeveloperId(String developerId) {
        this.developerId = developerId;
    }

    public String getStoreLocation() {
        return storeLocation;
    }

    public void setStoreLocation(String storeLocation) {
        this.storeLocation = storeLocation;
    }
    public void setAppList(String type,HashSet<String> appIds){
        if(type==null || appIds ==null || appIds.size()==0){
            return;
        }
        if(this.appList==null){
            appList=new ConcurrentHashMap<String, HashSet<String>>();
        }
        if(!appList.containsKey(type)){
            appList.put(type,appIds);
        }else{
            appList.get(type).addAll(appIds);
        }
    }

    public ConcurrentHashMap<String, HashSet<String>> getAppList() {
        return appList;
    }

    public String getCrawlerPoint() {
        return crawlerPoint;
    }

    public void setCrawlerPoint(String crawlerPoint) {
        this.crawlerPoint = crawlerPoint;
    }
    //check if the object is empty
    public boolean isEmpty(){
        return appList==null|| appList.size()==0||developerId==null;
    }

    @Override
    public String toString() {
        return "Developer{" +
                "developerName='" + developerName + '\'' +
                ", developerId='" + developerId + '\'' +
                ", storeLocation='" + storeLocation + '\'' +
                ", crawlerPoint='" + crawlerPoint + '\'' +
                ", appList=" + appList +
                '}';
    }
}

package data;

/**
 * Created by xiezhen on 6/1/2014.
 */
public class AppId {
    private String appId=null;            // app id assigned by app store vendors
    private String category=null;        // category of this app like games/...
    private String subCategory=null;    //subcategory of this app
    private String storeLocation=null;  // store location of this app, like us
    private String crawlerPoint=null;   // from where get this app id
    private int rank=-1;
//    public AppId(){}
    public AppId(String appId){
        this.appId=appId;
    }
    public AppId(String appId,String category,String subCategory,String storeLocation,String crawlerPoint){
        this.appId=appId;
        this.category=category;
        this.subCategory=subCategory;
        this.storeLocation=storeLocation;
        this.crawlerPoint=crawlerPoint;
    }
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getStoreLocation() {
        return storeLocation;
    }

    public void setStoreLocation(String storeLocation) {
        this.storeLocation = storeLocation;
    }

    public String getCrawlerPoint() {
        return crawlerPoint;
    }

    public void setCrawlerPoint(String crawlerPoint) {
        this.crawlerPoint = crawlerPoint;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppId appId1 = (AppId) o;

        if (!appId.equals(appId1.appId)) return false;
        if (!storeLocation.equals(appId1.storeLocation)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = appId.hashCode();
        result = 31 * result + storeLocation.hashCode();
        return result;
    }
}

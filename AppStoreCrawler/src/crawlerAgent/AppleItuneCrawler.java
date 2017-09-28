package crawlerAgent;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import configuration.CrawlerConfig;

import crawlerEngine.CrawlerEngine;
import data.AppId;
import utility.DataPersistence;
import crawlerParser.AppleItuneParser;
import data.AppData;

public class AppleItuneCrawler extends Crawler{

    static HashMap<String,String> storeFronts=new HashMap<String, String>();
    private int tryCount=5;

    public  AppleItuneCrawler(){
        crawler=this;
        contentParser = AppleItuneParser.getInstance();
        DataPersistence.getInstance().setCrawler(crawler);
    }
    @Override
    public void start(){
        DataPersistence.getInstance().setBaseConfig(CrawlerConfig.getInstance());
        loadStoreFronts();
        CrawlerConfig.getInstance().loadEngine();
    }
    private void loadStoreFronts(){
        ArrayList<String> sfs=CrawlerConfig.getInstance().getMessageManager().loadStoreParameters("itunes","storefronts");
        for(String sf:sfs){
            String []eles=sf.split("_");
            if(eles.length>1){
                storeFronts.put(eles[0],eles[1]);
            }
        }
    }
    @Override
    /**
     * para[0]- crawler point, para[1] - category, para[2]-store location, para[3]- index, para[4]- index2
     */
    public HttpURLConnection getIdHttpURLConnection(String... para) {
        if(para==null||para.length<1){
            log.error("Parameters list of getIdHttpURLConnection is empty or less than FOUR elements!");
            return null;
        }
        if(para[0].equals("1")){
            return getIdHttpURLConnection_websites(para);
        }else{
            return getIdHttpURLConnection_itunes(para);
        }
    }
    //connect to website directly to download app list
	public HttpURLConnection getIdHttpURLConnection_websites(String... para) {//store_location,
		if(para==null||para.length<5){
			log.error("Parameters list of getIdHttpURLConnection is empty or less than FOUR elements!");
			return null;
		}
        //https://itunes.apple.com/us/genre/ios-games-action/id7001?mt=8&letter=A&page=1#page
        //need convert storefront from code to abbreviation name
        String storeFront=storeFronts.get(para[2]);
        if(storeFront==null){
            return null;
        }
        storeFront=storeFront.toLowerCase();
        char letter=(char)('A'+Integer.parseInt(para[3]));
        if(letter>'Z'+1){
            return null;
        }else if(letter == 'Z'+1){
            letter='*';
        }

        int pageNum=Integer.parseInt(para[4]);
        pageNum++;//start from 1
		String requestURL="https://itunes.apple.com/"+storeFront+"/genre/id"+para[1]+"?mt=8&letter="+letter+"&page="+pageNum+"#page";
        log.info(requestURL);
        URL url=null;
		HttpURLConnection httpConn=null;
		Proxy proxy=null;
		int count=this.tryCount;
		try {
			do{
				try{
					url = new URL(requestURL);
					proxy=DataPersistence.getInstance().getProxy(url);
					if(proxy!=null){
						httpConn = (HttpURLConnection) url.openConnection(proxy);
                        httpConn.connect();
                        if(httpConn.getResponseCode()!=200){
                            log.info("response code:"+httpConn.getResponseCode());
                            log.info(requestURL);
                            DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
                            httpConn.disconnect();
                            httpConn=null;
                        }else{
                            DataPersistence.getInstance().decreaseLoad(proxy);
                        }
					}else{
						log.debug("Unable to connect proxy finally, use real IP instead!");
						httpConn = (HttpURLConnection) url.openConnection();
						httpConn.connect();
                        if(httpConn.getResponseCode()!=200){
                            log.info("response code:"+httpConn.getResponseCode());
                            log.info(requestURL);
                        }
						return httpConn;
					}

				} catch (Exception ex) {
					log.error(ex);
					log.info(requestURL);
					httpConn.disconnect();
                    if(proxy!=null){
                        DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
                    }
					httpConn=null;
				}
				if(count--<0){
					break;
				}
			}while(httpConn==null||httpConn.getResponseCode()!=200);
		} catch (Exception e) {
			log.error(e);
			log.info(requestURL);
			return null;
		}
		return httpConn;
	}

    //connect to itunes
    public HttpURLConnection getIdHttpURLConnection_itunes(String... para) {
        if(para==null||para.length<4){
            log.error("Parameters list of getIdHttpURLConnection is empty or less than FOUR elements!");
            return null;
        }
        String requestURL="https://itunes.apple.com/WebObjects/MZStore.woa/wa/viewTop?genreId="+para[1]+"&id=29100&popId="+para[0];
        URL url=null;
        HttpURLConnection httpConn=null;
        Proxy proxy=null;
        int count=this.tryCount;
        try {
            do{
                try{
                    url = new URL(requestURL);
                    proxy=DataPersistence.getInstance().getProxy(url);
                    if(proxy!=null){
                        httpConn = (HttpURLConnection) url.openConnection(proxy);
                        httpConn.setRequestProperty("User-Agent", "iTunes/11.0.1.12 (Windows; Microsoft Windows 7 x64 Ultimate Edition Service Pack 1 (Build 7601)) AppleWebKit/534.52.7");
                        httpConn.setRequestProperty("X-Apple-Store-Front", para[3]+"-1,12");
                        httpConn.connect();
                        if(httpConn.getResponseCode()!=200){
                            log.info("response code:"+httpConn.getResponseCode());
                            log.info(requestURL);
                            DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
                            httpConn.disconnect();
                            httpConn=null;
                        }else{
                            DataPersistence.getInstance().decreaseLoad(proxy);
                        }
                    }else{
                        log.debug("Unable to connect proxy finally, use real IP instead!");
                        httpConn = (HttpURLConnection) url.openConnection();
                        httpConn.setRequestProperty("User-Agent", "iTunes/11.0.1.12 (Windows; Microsoft Windows 7 x64 Ultimate Edition Service Pack 1 (Build 7601)) AppleWebKit/534.52.7");
                        httpConn.setRequestProperty("X-Apple-Store-Front", para[3]+"-1,12");
                        httpConn.connect();
                        return httpConn;
                    }

                } catch (Exception ex) {
                    // TODO Auto-generated catch block
                    log.error(ex);
                    log.info(requestURL);
                    httpConn.disconnect();
                    DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
                    httpConn=null;
                }
                if(count--<0){
                    break;
                }
            }while(httpConn==null||httpConn.getResponseCode()!=200);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            log.error(e);
            log.info(requestURL);
            return null;
        }
        return httpConn;
    }
	@Override
    /***
     * para[0] - app_id, para[1] - store location
     */
	public HttpURLConnection getOverviewHttpURLConnection(String... para) {
		// TODO Auto-generated method stub
		if(para==null||para.length<2){
			log.error("Parameters list of getOverviewHttpURLConnection is empty or less than TWO element!");
			return null;
		}
		//http://ax.phobos.apple.com.edgesuite.net/WebObjects/MZStore.woa/wa/viewSoftware?id=525378313&mt=8
		String requestURL="https://itunes.apple.com/WebObjects/MZStore.woa/wa/viewSoftware?id="+para[0];
//        requestURL="http://ax.phobos.apple.com.edgesuite.net/WebObjects/MZStore.woa/wa/viewSoftware?id="+para[0];
		URL url=null;
		HttpURLConnection httpConn=null;
		Proxy proxy=null;
		int count=this.tryCount;
		try {
				do{
					try{
						url = new URL(requestURL);
						proxy=DataPersistence.getInstance().getProxy(url);
						if(proxy!=null){
							httpConn = (HttpURLConnection) url.openConnection(proxy);
                            httpConn.setRequestProperty("User-Agent", "iTunes/10.7 (Windows; Microsoft Windows 7 x64 Ultimate Edition Service Pack 1 (Build 7601)) AppleWebKit/534.52.7");
                            httpConn.setRequestProperty("X-Apple-Store-Front", para[1]+"-1,17 ab:BKDB");
                            httpConn.setConnectTimeout(15000);
                            httpConn.connect();
                            if(httpConn.getResponseCode()!=200){
                                log.info("response code:"+httpConn.getResponseCode());
                                log.info(requestURL);
                                DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
                                httpConn.disconnect();
                                httpConn=null;
                            }else{
                                DataPersistence.getInstance().decreaseLoad(proxy);
                            }
                        }else{
							log.debug("Unable to connect proxy finally, use real IP instead!");
							httpConn = (HttpURLConnection) url.openConnection();
							httpConn.setRequestProperty("User-Agent", "iTunes/10.7 (Windows; Microsoft Windows 7 x64 Ultimate Edition Service Pack 1 (Build 7601)) AppleWebKit/534.52.7");
							httpConn.setRequestProperty("X-Apple-Store-Front", para[1]+"-1,17 ab:BKDB");
                            httpConn.setConnectTimeout(15000);
                            httpConn.connect();
                            return httpConn;
						}

					}catch(Exception ex){
						log.error(ex);
						log.info(requestURL);
						DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
						httpConn.disconnect();
						httpConn=null;
					}
					if(count--<0){
						break;
					}
				}while(httpConn==null||httpConn.getResponseCode()!=200);
			} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
			log.info(requestURL);
			httpConn.disconnect();
			return null;
		}
		return httpConn;
	}
	@Override
    //appid, storeLocation, pageNum
	public HttpURLConnection getCommentHttpURLConnection(String... para) {
		// TODO Auto-generated method stub
		if(para==null||para.length<3){
			log.error("Parameters list of getCommentHttpURLConnection is empty or less than Three element!");
			return null;
		}
		String requestURL="https://itunes.apple.com/WebObjects/MZStore.woa/wa/viewContentsUserReviews?id="+para[0]+"&pageNumber="+para[2]+"&sortOrdering=4&type=Purple+Software";
        URL url=null;
		HttpURLConnection httpConn=null;
		Proxy proxy=null;
		int count=this.tryCount;
		try {
			do{
				try{
					
					url = new URL(requestURL);
					proxy=DataPersistence.getInstance().getProxy(url);

					if(proxy!=null){
						httpConn = (HttpURLConnection) url.openConnection(proxy);
                        httpConn.setRequestProperty("User-Agent", "iTunes/10.5.3 (Windows; Microsoft Windows 7 x64 Ultimate Edition Service Pack 1 (Build 7601)) AppleWebKit/534.52.7");
                        httpConn.setRequestProperty("X-Apple-Store-Front", para[1]+"-1,17");
                        httpConn.setConnectTimeout(15000);
                        httpConn.connect();
                        if(httpConn.getResponseCode()!=200){
                            log.info("response code:"+httpConn.getResponseCode());
                            log.info(requestURL);
                            DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
                            httpConn.disconnect();
                            httpConn=null;
                        }else{
                            DataPersistence.getInstance().decreaseLoad(proxy);
                        }
					}else{
						log.debug("Unable to connect proxy finally, use real IP instead!");
						httpConn = (HttpURLConnection) url.openConnection();
                        httpConn.setRequestProperty("User-Agent", "iTunes/10.5.3 (Windows; Microsoft Windows 7 x64 Ultimate Edition Service Pack 1 (Build 7601)) AppleWebKit/534.52.7");
                        httpConn.setRequestProperty("X-Apple-Store-Front", para[1]+"-1,17");
                        httpConn.setConnectTimeout(15000);
                        httpConn.connect();
					}
				}catch(Exception ex){
					log.error(ex);
					log.info(requestURL);
					DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
					httpConn.disconnect();
					httpConn=null;
				}
				if(count--<0){
					break;
				}
			}while(httpConn==null||httpConn.getResponseCode()!=200);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
			log.info(requestURL);						
			httpConn.disconnect();
			httpConn=null;
		}
		return httpConn;
	}

	@Override
	public HttpURLConnection getRankHttpURLConnection(String... para) {
		// TODO Auto-generated method stub
		return getIdHttpURLConnection(para);
	}


	@Override
	public HttpURLConnection getUserProfileHttpURLConnection(String... para) {
		// TODO Auto-generated method stub
		if(para==null||para.length<3){
			log.error("Parameters list of getUserProfileHttpURLConnection is empty or less than THREE elements!");
			return null;
		}
		log.debug("Start to connect the user profile address of "+para[0]);
		String requestURL="https://itunes.apple.com/WebObjects/MZStore.woa/wa/viewUsersUserReviews?userProfileId="+para[0]+"&page="+para[2]+"&sort=14";
//		requestURL="https://itunes.apple.com/us/reviews&userProfileId="+para[0]+"&amp;ign-mscache=1&page="+para[2]+"&sort=14";
//        https://itunes.apple.com/us/reviews?userProfileId=465009737
		HttpURLConnection httpConn =null;
		URL url =null;
		Proxy proxy=null;
		int count=this.tryCount;
		try{
			do{
				try{
					url = new URL(requestURL);
					proxy=DataPersistence.getInstance().getProxy(url);
					if(proxy!=null){
						httpConn = (HttpURLConnection) url.openConnection(proxy);
					}else{
						log.debug("Unable to connect proxy finally, use real IP instead!");
						httpConn = (HttpURLConnection) url.openConnection();
					}
					httpConn.setRequestProperty("User-Agent", "iTunes/10.5.3 (Windows; Microsoft Windows 7 x64 Ultimate Edition Service Pack 1 (Build 7601)) AppleWebKit/534.52.7");
					httpConn.setRequestProperty("X-Apple-Store-Front", para[1]+"-1");
                    httpConn.setConnectTimeout(15000);
					httpConn.connect();
					if(httpConn.getResponseCode()!=200){
						log.info("response code:"+httpConn.getResponseCode());
						log.info(requestURL);
						DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
						httpConn.disconnect();
						httpConn=null;
					}else{
						DataPersistence.getInstance().decreaseLoad(proxy);
					}
				}catch(Exception ex){
					log.error(ex);
					log.info(requestURL);
					DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
					httpConn.disconnect();
					httpConn=null;
				}
				if(count--<0){
					log.info(requestURL);
					break;
				}
			}while(httpConn==null||httpConn.getResponseCode()!=200);
		}catch(Exception ex){
			log.error(ex);
			log.info(requestURL);
			httpConn.disconnect();
			return null;
		}
		log.debug("Finish connecting the user profile address of "+para[0]+" HttpConn:"+httpConn);

		return httpConn;
	}

    @Override
    public HttpURLConnection getDeveloperProfileHttpURLConnection(String... para) {//developer_id,storelocation,pageNum1,pageNum2
        if(para==null || para.length<4){
            log.error("Missing parameters -  "+para.toString());
            return null;
        }

        log.debug("Start to connect the user profile address of "+para[0]);
        //https://itunes.apple.com/us/artist/google-inc./id281956209?iPhoneSoftwarePage=1&iPadSoftwarePage=2
        String storeFront=storeFronts.get(para[1]).toLowerCase();
        String requestURL="https://itunes.apple.com/"+storeFront+"/artist/id"+para[0]+"?iPhoneSoftwarePage="+para[2]+"&iPadSoftwarePage="+para[3];
        HttpURLConnection httpConn =null;
        URL url =null;
        Proxy proxy=null;
        int count=this.tryCount;
        try{
            do{
                try{
                    url = new URL(requestURL);
                    proxy=DataPersistence.getInstance().getProxy(url);
                    if(proxy!=null){
                        httpConn = (HttpURLConnection) url.openConnection(proxy);
                    }else{
                        log.debug("Unable to connect proxy finally, use real IP instead!");
                        httpConn = (HttpURLConnection) url.openConnection();
                    }
                    httpConn.setRequestProperty("User-Agent", "iTunes/10.5.3 (Windows; Microsoft Windows 7 x64 Ultimate Edition Service Pack 1 (Build 7601)) AppleWebKit/534.52.7");
                    httpConn.setConnectTimeout(15000);
                    httpConn.connect();
                    if(httpConn.getResponseCode()!=200){
                        log.info("response code:"+httpConn.getResponseCode());
                        log.info(requestURL);
                        DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
                        httpConn.disconnect();
                        httpConn=null;
                    }else{
                        DataPersistence.getInstance().decreaseLoad(proxy);
                    }
                }catch(Exception ex){
                    log.error(ex);
                    log.info(requestURL);
                    DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
                    httpConn.disconnect();
                    httpConn=null;
                }
                if(count--<0){
                    log.info(requestURL);
                    break;
                }
            }while(httpConn==null||httpConn.getResponseCode()!=200);
        }catch(Exception ex){
            log.error(ex);
            log.info(requestURL);
            httpConn.disconnect();
            return null;
        }
        log.debug("Finish connecting the user profile address of "+para[0]+" HttpConn:"+httpConn);

        return httpConn;
    }

    /**
     * Use dynamic loading method to load each engine configured in the configuration
     * @param arg
     */
    public static void main(String ...arg){
        Crawler crawler= new AppleItuneCrawler();
        crawler.start();
        crawler.addAppId(new AppId("880178264","","","143441","test"));
//        crawler.addAppId(new AppId("862433111","","","143465","test"));

    }
}

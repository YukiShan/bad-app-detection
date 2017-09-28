package crawlerAgent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import crawlerEngine.CrawlerEngine;
import crawlerParser.AppleItuneParser;
import data.AppId;
import utility.DataPersistence;

import com.gc.android.market.api.MarketSession;

import configuration.CrawlerConfig;
import crawlerParser.GooglePlayParser;

public class GooglePlayCrawler extends Crawler{
	private static MarketSession session;
	private int tryCount=5;
	public GooglePlayCrawler(){
		if(crawler==null){
			crawler=this;
			contentParser=GooglePlayParser.getInstance();
            DataPersistence.getInstance().setCrawler(crawler);
            DataPersistence.getInstance().setBaseConfig(CrawlerConfig.getInstance());
            init();
        }
	}
	private static void init(){
//		session = new MarketSession(false);
//		session.login("","","");
	}
    @Override
    public void start(){
        CrawlerConfig.getInstance().loadEngine();
    }
	@Override
	public HttpURLConnection getIdHttpURLConnection(String... para) {
		// TODO Auto-generated method stub
		if(para==null||para.length<4){
			log.error("Parameters list of getIdHttpURLConnection is empty or less than Four elements!");
			return null;
		}
		String category="";
		if(!"all".equalsIgnoreCase(para[0])){
			category="/category/"+para[0];
		}
		String startNum=Integer.parseInt(para[1])*24+"";
		String requestURL="https://play.google.com/store/apps"+category+"/collection/"+para[3]+"?start="+startNum+"&num=24&hl=en";
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
				}else{
					log.info("Unable to connect proxy finally, use real IP instead!");
					httpConn = (HttpURLConnection) url.openConnection();
					httpConn.connect();
					return httpConn;
				}
				httpConn.connect();
				if(httpConn.getResponseCode()!=200){
					log.info("response code:"+httpConn.getResponseCode());
					log.info(requestURL);
					if(httpConn.getResponseCode()==404||httpConn.getResponseCode()==403){
						return null;
					}
					DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
					httpConn=null;
				}
				}catch(Exception ex){
					log.error(ex);
					ex.printStackTrace();
					log.info(requestURL);
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
	public HttpURLConnection getOverviewHttpURLConnection(String... para) {
		// TODO Auto-generated method stub
		if(para==null||para.length<1){
			log.error("Parameters list of getOverviewHttpURLConnection is empty or less than ONE element!");
			return null;
		}
		String requestURL="https://play.google.com/store/apps/details?id="+para[0]+"&hl=en";
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
					}else{
						log.info("Unable to connect proxy finally, use real IP instead!");
						httpConn = (HttpURLConnection) url.openConnection();
					}
					httpConn.setRequestProperty("hl", "en");
					httpConn.setRequestProperty("accept-language", "en-US,en;q=0.8");
					httpConn.connect();
					if(httpConn.getResponseCode()!=200){
						log.info("response code:"+httpConn.getResponseCode());
						log.info(requestURL);

						if(httpConn.getResponseCode()==404||httpConn.getResponseCode()==403){
							return httpConn;
						}else if(httpConn.getResponseCode()==503){
                            DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
                        }
						httpConn=null;
					}
				}catch(Exception ex){
					log.error(ex);
					log.info(requestURL);
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
	public HttpURLConnection getCommentHttpURLConnection(String... para) {
		// TODO Auto-generated method stub
		if(para==null||para.length<2){
			log.error("Parameters list of getCommentHttpURLConnection is empty or less than TWO elements!");
			return null;
		}
		//newest review first
		String requestURL="https://play.google.com/store/getreviews";//?id="+para[0]+"&reviewSortOrder=0&reviewType=1&pageNum="+para[1]+"&hl=en";
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
					}else{
						log.debug("Unable to connect proxy finally, use real IP instead!");
						httpConn = (HttpURLConnection) url.openConnection();
					}
					String requestURLOverview="https://play.google.com/store/apps/details?id="+para[0]+"&hl=en";
					httpConn.setRequestProperty("referer", requestURLOverview);
                    httpConn.setRequestProperty("content-type","application/x-www-form-urlencoded;charset=UTF-8");
                    httpConn.setRequestMethod("POST");
					httpConn.setDoInput(true);
					httpConn.setDoOutput(true);
					OutputStream oStrm =null; 
					oStrm = httpConn.getOutputStream();
                    String formData="reviewType=1&pageNum="+para[2]+"&id="+para[0]+"&reviewSortOrder=0&xhr=1&hl=en";
					oStrm.write(formData.getBytes());
					oStrm.flush(); 
					oStrm.close(); 
					httpConn.connect();
					if(httpConn.getResponseCode()!=200){
						log.info("response code:"+httpConn.getResponseCode());
						log.info(requestURL);
						if(httpConn.getResponseCode()==404||httpConn.getResponseCode()==403){
							return null;
						}else if(httpConn.getResponseCode()==503){
                            DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
                        }
						httpConn=null;
					}
				}catch(Exception ex){
					log.error(ex);
					log.info(requestURL);
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
	public HttpURLConnection getRankHttpURLConnection(String... para) {
		// TODO Auto-generated method stub
		return getIdHttpURLConnection(para);
	}
	@Override
	public HttpURLConnection getUserProfileHttpURLConnection(String... para) {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public HttpURLConnection getDeveloperProfileHttpURLConnection(String... para) {

        if(para==null||para.length<1){
            log.error("Parameters list of getDeveloperProfileHttpURLConnection is empty or less than ONE element!");
            return null;
        }
        String requestURL="https://play.google.com/store/apps/developer?id="+para[0]+"&hl=en";
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
                    }else{
                        log.info("Unable to connect proxy finally, use real IP instead!");
                        httpConn = (HttpURLConnection) url.openConnection();
                    }
                    httpConn.setRequestProperty("accept-language", "en-US,en;q=0.8");
                    httpConn.connect();
                    if(httpConn.getResponseCode()!=200){
                        log.info("response code:"+httpConn.getResponseCode());
                        log.info(requestURL);

                        if(httpConn.getResponseCode()==404||httpConn.getResponseCode()==403){
                            return httpConn;
                        }else if(httpConn.getResponseCode()==503){
                            DataPersistence.getInstance().setProxyError(url.toURI(), proxy);
                        }
                        httpConn=null;
                    }
                }catch(Exception ex){
                    log.error(ex);
                    log.info(requestURL);
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


    /**
	 * Use dynamic loading method to load each engine configured in the configuration
	 * @param arg
	 */
	public static void main(String ...arg){
        Crawler crawler= new GooglePlayCrawler();
        crawler.start();
        crawler.addAppId(new AppId("com.gameloft.android.ANMP.GloftR2HM","","","US","Test"));
	}

	
}

package crawlerEngine;

import crawlerAgent.Crawler;

public interface CrawlerEngine{
	public void start();
	public void init(int engineNum,Crawler crawler,String ...para);
}

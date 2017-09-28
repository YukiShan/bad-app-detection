package crawlerParser;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.gc.android.market.api.model.Market;
import data.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import utility.DataPersistence;
import utility.MultiDateFormat;
import utility.SystemLogger;
import crawlerAgent.GooglePlayCrawler;

public class GooglePlayParser implements ContentParser{

    private SystemLogger log = SystemLogger.getLogger(GooglePlayParser.class);
	private static ContentParser gpp=null;
	private GooglePlayParser(){}
	public static ContentParser getInstance(){
		if(gpp==null){
			gpp=new GooglePlayParser();
		}
		return gpp;
	}

	@Override
	//idList, sum,category,storeLocation,crawlerPoint
	public int idParser(ConcurrentLinkedQueue<AppId> idList,StringBuilder idPage,String ...para) {
		// TODO Auto-generated method stub
		org.jsoup.nodes.Document doc = Jsoup.parse(idPage.toString());
		String appId=null;
		for(Element ele:doc.getElementsByClass("snippet-medium")){
			appId=ele.getElementsByClass("buy-offer").attr("data-docid").trim();
			String ordial=ele.getElementsByClass("ordinal-value").text().trim();
			int order=Integer.parseInt(ordial);
			if(appId!=null && !"".equalsIgnoreCase(appId)){
				
				idList.add(new AppId(appId,para[0],para[0],para[1],para[2]));
//				AppData appData=GooglePlayCrawler.getInstance().getAppData(appId);
//				if(appData==null){
//					appData=new AppData(appId);
//					appData.setLatestReviewId(DataPersistence.getInstance().loadLatestReviewId(appId));
//				}
//				appData.setCategory(para[0]);
//				appData.setStoreLocation(para[1]);
//				appData.setCrawlerPoint(para[2]);//no need
//				appData.setRank(order);
//				log.debug(appId);
//				GooglePlayCrawler.getInstance().setAppData(appId, appData);
			}
			if(order==504){//specific for google play
				return 0;//true
			}
		}
		return 1;//false
	}

	@Override
	public boolean overviewParser(StringBuilder overview,AppData appData) {
        if(overview==null ||overview.length()==0|| appData==null){
            return false;
        }
		try{
			org.jsoup.nodes.Document doc = Jsoup.parse(overview.toString());
            Element bodyContent=doc.getElementById("body-content");
            Elements eles=bodyContent.getElementsByAttributeValue("itemprop", "author");
            if (eles.size()>0){
                Element author=((eles.get(0)).getElementsByClass("document-subtitle")).get(0);
                appData.setDeveloperId(author.attr("href").split("\\?id=")[1]);
                appData.setDeveloperName(author.text());
            }

            Element ratingBox=bodyContent.getElementsByClass("rating-box").get(0);
            String averageValue=ratingBox.getElementsByClass("score").text().trim();
            averageValue.replaceAll(",", ".");
            try{
                if(!"".equals(averageValue)){
                    appData.setAverageRating(Float.parseFloat(averageValue));
                }
            }catch(NumberFormatException ne){
                log.error(ne.getMessage());
            }

            int index=5;
            for(Element barNum:ratingBox.getElementsByClass("bar-number")){
                String numStr=barNum.text();
                numStr=numStr.replaceAll(",", "").trim();
                if(!"".equals(numStr)){
                    int ratingNum=Integer.parseInt(numStr);
                    appData.setRatingNum(index--, ratingNum);
                }
            }
            String totalNumStr=bodyContent.getElementsByClass("reviews-num").text().trim();
            totalNumStr=totalNumStr.replace(",", "");
            if(!"".equals(totalNumStr)){
                int totalNum=Integer.parseInt(totalNumStr);
                appData.setTotalRaters(totalNum);
            }
            appData.setCurrentVersion(bodyContent.getElementsByAttributeValue("itemprop", "softwareVersion").text());
            String requiredAndroidVersion=(bodyContent.getElementsByAttributeValue("itemprop", "operatingSystems")).text();
            appData.setRequiredAndroidVersion(requiredAndroidVersion);
            appData.setSubCategory(bodyContent.getElementsByAttributeValue("itemprop", "genre").text());
            String priceStr=bodyContent.getElementsByAttributeValue("itemprop", "price").attr("content").trim();
            if ("0".equals(priceStr)){//free
                appData.setPrice(priceStr);
                appData.setCurrency("");
            }else{
                appData.setPrice(priceStr.substring(1));
                appData.setCurrency(priceStr.substring(0,1));
            }
            String thumbNail=bodyContent.getElementsByClass("cover-image").attr("src");
            appData.setThumbNail(thumbNail);
            String appName=bodyContent.getElementsByClass("document-title").text();
            appData.setAppName(appName);
            String description=bodyContent.getElementsByClass("id-app-orig-desc").text();
            appData.setDescription(description);
            appData.setLatestModified(bodyContent.getElementsByAttributeValue("itemprop", "datePublished").text());
//            String latestAction=(bodyContent.getElementsByClass("doc-metadata-list")).get(0).child(5).text();
//            if(!"".equals(latestAction)){
//                appData.setLatestAction(latestAction.substring(0, latestAction.length()-1));
//            }
            String installs=bodyContent.getElementsByAttributeValue("itemprop", "numDownloads").text();
            appData.setInstalls(installs);
            String contentRated=bodyContent.getElementsByAttributeValue("itemprop", "contentRating").text();
            appData.setContentRated(contentRated);
            String url=bodyContent.getElementsByAttributeValue("itemprop", "url").get(0).attr("content");
            appData.setUrl(url);
            String size=bodyContent.getElementsByAttributeValue("itemprop", "fileSize").text();
            appData.setSize(size);
            //permission
//            String permission="";
//            for(Element elePermission:bodyContent.getElementsByClass("permissions-container bucket-style")){
//                permission+=elePermission.text()+",";
//            }
//            if(!"".equalsIgnoreCase(permission)){
//                permission=permission.substring(0,permission.length()-1);
//                appData.setPermission(permission);
//            }
            //contact information
            appData.setContactPhone("");
            Elements elesEmail=bodyContent.getElementsMatchingOwnText("Email Developer");
            if(elesEmail.size()>0){
                String contactEmail=elesEmail.attr("href");
                appData.setContactEmail(contactEmail);
            }
            Elements elesWebsite=bodyContent.getElementsMatchingOwnText("Visit Developer's Website");
            if(elesWebsite.size()>0){
                String contactWebsite=elesWebsite.attr("href");
                int indexq=contactWebsite.indexOf("q=");
                appData.setContactWebsite(contactWebsite.substring(indexq+2));
            }

            String promotedText=bodyContent.getElementsByClass("recent-change").text();
            appData.setPromotedText(promotedText);
            String promotedScreenshot="";
            Elements elesScreenshot=bodyContent.getElementsByClass("screenshot-carousel-content-container");
            if(elesScreenshot.size()>0){
                for(Element eleScreenshot:elesScreenshot.get(0).getElementsByTag("img")){
                    promotedScreenshot+=eleScreenshot.attr("src")+",";
                }
            }
            if(!"".equalsIgnoreCase(promotedScreenshot)){
                promotedScreenshot=promotedScreenshot.substring(0,promotedScreenshot.length()-1);
            }
            appData.setPromotedScreenshot(promotedScreenshot);
            String promotedVideo="";
            Elements elesVideo=bodyContent.getElementsByClass("play-action-container");
            if(elesVideo.size()>0){
                promotedVideo=(elesVideo.get(0)).attr("data-video-url");
            }
            appData.setPromotedVideo(promotedVideo);
            return true;
		}catch(Exception ex){
			log.info(ex.getMessage());
            ex.printStackTrace();
		}
        return false;
	}

	@Override
	public boolean CommentsParser(StringBuilder commentsPage,
			int currentPageNum,AppData appData) {
		if(commentsPage ==null || commentsPage ==null){
			return false;
		}
        try {
            String input = StringEscapeUtils.unescapeJava(commentsPage.toString());
            int start = input.indexOf("<div");
            int end = input.lastIndexOf("div>");
            if(start == -1 || end==-1 ){
                return false;
            }
            org.jsoup.nodes.Document doc = Jsoup.parse(input.substring(start, end + 4));
            for (Element ele : doc.getElementsByClass("single-review")) {
                Comment comment = parseComment(ele);
                //For update only
                if (appData.getLatestReviewId() != null && appData.getLatestReviewId().equalsIgnoreCase(comment.getReviewId())) {
                    return false;
                } else if (comment != null) {
                    appData.addComment(comment);
                }
            }
        }catch (Exception ex){
            log.error(ex);
            ex.printStackTrace();
        }
//		return true;
        return false;
	}
	@Override
	public boolean RankParser(ConcurrentLinkedQueue<AppRatingInfo> ariList,StringBuilder rankPage,String ...param) {
		// TODO Auto-generated method stub
		org.jsoup.nodes.Document doc = Jsoup.parse(rankPage.toString());
		String appId=null;
		String ordial=null;
		String title=null;
		for(Element ele:doc.getElementsByClass("snippet-medium")){
			appId=ele.getElementsByClass("buy-offer").attr("data-docid").trim();
			ordial=ele.getElementsByClass("ordinal-value").text().trim();
			title=ele.getElementsByClass("buy-offer").attr("data-doctitle").trim();
			if(appId!=null && !"".equalsIgnoreCase(appId)
					&& ordial!=null && !"".equalsIgnoreCase(ordial)){
				log.debug(appId + "\t"+ordial+ "\t"+title);
				int order=Integer.parseInt(ordial);
				title=title.replace("'", "\\'");
				AppRatingInfo ari=new AppRatingInfo(param[0],param[1],param[2],appId,new Date(),order,title);
				ariList.add(ari);
				if(order==500){//specific for google play
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * parse a single comment and save it to a object
	 * @param ele
	 * @return
	 */
	private Comment parseComment(Element ele){
		String reviewAuthor="author-name";
		String reviewDateKeyword="review-date";
		String reviewRatingKeyword="current-rating";
		String reviewRatingValueKeyword="style";
		String reviewTitleKeyword="review-title";
		String reviewTextKeyword="review-body";
		Comment comment=new Comment();
		MultiDateFormat sdf2=new MultiDateFormat("MMMM dd, yyyy");

		comment.setReviewer((ele.getElementsByClass(reviewAuthor)).text());
		
		String dateStr=(ele.getElementsByClass(reviewDateKeyword)).text();
		dateStr=dateStr.replace("- ", "");
		try {
			comment.setDate(sdf2.parse(dateStr));
		} catch (ParseException e) {
			log.error(e);
		}
		
		String ratingStr=ele.getElementsByClass(reviewRatingKeyword).attr(reviewRatingValueKeyword);
		String []ratings=ratingStr.split(" ");
		String ratingTemp=ratings[1];
		ratingTemp=ratingTemp.replaceAll("%|;", "");
		float ratingVal=Float.parseFloat(ratingTemp);
		comment.setRating(ratingVal*5/100);
		
		comment.setComment((ele.getElementsByClass(reviewTextKeyword)).text());
		comment.setCommentTitle((ele.getElementsByClass(reviewTitleKeyword)).text());
		//reviewer
		Element authorEle=ele.getElementsByClass(reviewAuthor).first();
		if(authorEle!=null && authorEle.getElementsByTag("a").size()>0){
            String authorId=authorEle.getElementsByTag("a").attr("href");
            authorId=authorId.split("id=")[1];
			comment.setReviewerId(authorId);
		}else{
			comment.setReviewerId("0");//dummy id
		}
		
		Element reviewEle=ele.getElementsByClass("reviews-permalink").first();
		String reviewURL=reviewEle.attr("href");
		String reviewId=reviewURL.split("&reviewId=")[1];
		comment.setReviewId(reviewId);
		
		
		/* Cannot get device version any more.
		String deviceVer=ele.childNode(1).childNode(2).toString().trim();
		int versionIndex=deviceVer.indexOf("version");;
		if(versionIndex>-1){
			String device=deviceVer.substring(2, versionIndex).replace("with", "");
			comment.setDevice(device.trim());
			comment.setDeviceVersion(deviceVer.substring(versionIndex+7).trim());
		}*/
		return comment;
	}
	@Override
	public boolean userProfileParser(Reviewer reviewer,StringBuilder userProfileStr) {
		// TODO Auto-generated method stub
		return false;
	}

    @Override
    public boolean developerProfileParser(Developer developer, StringBuilder developerProfileStr) {
        org.jsoup.nodes.Document doc = Jsoup.parse(developerProfileStr.toString());
        developer.setDeveloperName(doc.getElementsByClass("cluster-heading").text());
        HashSet<String> appIdSet=new HashSet<String>();

        for(Element ele:doc.getElementsByClass("preview-overlay-container")){
            appIdSet.add(ele.attr("data-docid"));
        }
        developer.setAppList("Android",appIdSet);
        return false;
    }

}

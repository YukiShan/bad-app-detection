package crawlerParser;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import data.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import utility.MultiDateFormat;
import utility.SystemLogger;

public class AppleItuneParser implements ContentParser{
	private static SystemLogger log = SystemLogger.getLogger(AppleItuneParser.class);
	private static ContentParser aip=null;
	private AppleItuneParser(){}
	public static ContentParser getInstance(){
		if(aip==null){
			aip=new AppleItuneParser();
		}
		return aip;
	}
	@Override
	//idList, sum,category,storeLocation,crawlerPoint
	public int idParser(ConcurrentLinkedQueue<AppId> idList,StringBuilder idPage,String ...para) {
        if(para.length<3){//category, store_location, crawler_point
            log.error(" Parameters error!");
            return 0;
        }
        if(para[2].equals("1")){
            return idParser_websites(idList,idPage,para);
        }else{
            return idParser_itunes(idList,idPage,para);
        }
	}
    private int idParser_websites(ConcurrentLinkedQueue<AppId> idList,StringBuilder idPage,String ...para) {
        //flag must indicate three status:
        int flag=2;
        boolean isLastLetter=false;
        boolean isLastPage=false;
        try{
            org.jsoup.nodes.Document doc =Jsoup.parse(idPage.toString());
            Element body= doc.getElementById("selectedgenre");
            //get alpha
            Elements alphaEles=body.getElementsByClass("alpha");
            if(alphaEles.size()>0){
                Element alpha=alphaEles.first();
                Elements allIndexes=alpha.getElementsByClass("selected");//the last page has no such elements
                if(allIndexes.size() == 1){
                    String href=allIndexes.first().attr("href");
                    if(href.contains("letter=*")){//meet the last letter
                        isLastLetter=true;
                    }
                }
            }else{
                isLastLetter=true;
            }
            //get paginate
            Elements paginateEles=body.getElementsByClass("paginate");
            if(paginateEles.size()>0){
                Element paginate=paginateEles.first();//paginate
                Elements allIndexes=paginate.getElementsByClass("selected");//the last page has no such elements
                if(allIndexes.size() == 0){//the last page of 'A'
                    isLastPage=true;//the last page
                }else{
                    Element lastIndex=paginate.getElementsByTag("a").last();//get index
                    if(lastIndex.hasClass("selected")){//
                        isLastPage=true;
                    }
                }
            }else{
                isLastPage=true;
            }
            Element contentEle=body.getElementById("selectedcontent");
            for(Element column:contentEle.getElementsByTag("li")){
                Elements hrefs=column.getElementsByTag("a");
                if(hrefs.size()==1){
                    String href=hrefs.get(0).attr("href");
                    int startIndex=href.lastIndexOf("/id");
                    int endIndex=href.lastIndexOf("?mt=8");
                    if(startIndex>0 && endIndex>0){
                        String appId=href.substring(startIndex+3,endIndex);
                        idList.add(new AppId(appId,para[0],para[0],para[1],para[2]));
                    }
                }
            }
            if(isLastPage && isLastLetter){
                flag=0;//no last page
            }else if(isLastPage){
                flag=1; //change to next letter
            }
        }catch(Exception ex){
            log.error(ex);
            flag=0;
        }
        return flag;
    }
    private int idParser_itunes(ConcurrentLinkedQueue<AppId> idList,StringBuilder idPage,String ...para) {
        // TODO Auto-generated method stub
        try{
            org.jsoup.nodes.Document doc = Jsoup.parse(idPage.toString());
//            int i=1;
            for (Element ele: doc.getElementsByAttributeValue("class","lockup small detailed option application")){
                String appId=ele.attr("adam-id");
                String rankStr=ele.getElementsByClass("index").text();
                rankStr=rankStr.substring(0, rankStr.length()-1);
                log.debug("Rank=	"+rankStr+" appId="+appId);
                AppId appIdObj=new AppId(appId,para[0],para[0],para[1],para[2]);
                try {
                    appIdObj.setRank(Integer.parseInt(rankStr));
                }catch (Exception ex){
                    log.error(ex);
                }
                idList.add(appIdObj);
            }
        }catch(Exception ex){
            log.error(ex);
            return 0;
        }
        return 1;
    }
	@Override
    public boolean overviewParser(StringBuilder overview,AppData appData) {
        if(overview==null ||overview.length()==0|| appData==null){
            return false;
        }
        try{
            int startIndex=overview.indexOf(">its.serverData=");
            int endIndex=overview.indexOf("</script>",startIndex);
            if(startIndex ==-1 ||endIndex == -1){//not valid form
                return false;
            }
            String text=overview.substring(startIndex+16,endIndex);
            JSONObject jsonObject= new JSONObject(text);
            JSONObject productObj=jsonObject.getJSONObject("storePlatformData").getJSONObject("product-dv-product").getJSONObject("results");
			JSONObject appJASONObj=productObj.getJSONObject(appData.getAppId());
			appData.setAppName(appJASONObj.get("name").toString());
            appData.setDeveloperId(appJASONObj.get("artistId").toString());
            appData.setDeveloperName(appJASONObj.get("artistName").toString());
            appData.setDeveloperCompany(appJASONObj.get("copyright").toString());
            appData.setAverageRating((float) appJASONObj.getJSONObject("userRating").getDouble("value"));
            appData.setTotalRaters(appJASONObj.getJSONObject("userRating").getInt("ratingCount"));
            appData.setRatedSummary(appJASONObj.getJSONObject("contentRatingsBySystem")
                    .getJSONObject("appsApple").get("name").toString());
            appData.setSubCategory(appJASONObj.get("genreNames").toString());
            JSONArray offers=appJASONObj.getJSONArray("offers");
            if(offers.length()>0){
                appData.setCurrentVersion(offers.getJSONObject(0).getJSONObject("version").getString("display"));
                appData.setPrice(offers.getJSONObject(0).getDouble("price")+"");
                appData.setCurrency(offers.getJSONObject(0).getString("priceFormatted").charAt(0)+"");
            }
            //set thumbnail of the app
            JSONArray icons=appJASONObj.getJSONArray("artwork");
            if(icons.length()>0){
                appData.setThumbNail(icons.getJSONObject(0).toString());
            }
            appData.setDescription(appJASONObj.get("description").toString());
            //privacyPolicyUrl
            appData.setLatestAction(appJASONObj.getJSONObject("softwareInfo").get("privacyPolicyUrl").toString());
            appData.setContentRated(appJASONObj.getJSONObject("contentRatingsBySystem")
                    .getJSONObject("appsApple").get("name").toString());
            appData.setLanguage(appJASONObj.getJSONObject("softwareInfo").get("languagesDisplayString").toString());
//            appData.setRequiredAndroidVersion(appJASONObj.getJSONArray("requiredCapabilities").toString());
            appData.setRequirements(appJASONObj.getJSONObject("softwareInfo").get("requirementsString").toString());
            appData.setUrl(appJASONObj.get("url").toString());

            //for itunes, permission is set to itunesNotes
            appData.setPermission(appJASONObj.get("itunesNotes").toString());

            //moreByThisDeveloper : other apps belonging to this developer
            JSONObject softwareJObj=jsonObject.getJSONObject("pageData").getJSONObject("softwarePageData");
            if(softwareJObj.keySet().contains("moreByThisDeveloper")){
                appData.setContactPhone(softwareJObj.get("moreByThisDeveloper").toString());
            }
            appData.setContactEmail(appJASONObj.getJSONObject("softwareInfo").get("supportUrl").toString());
            appData.setContactWebsite(appJASONObj.getJSONObject("softwareInfo").get("websiteUrl").toString());
            //bundleId com.MikaMobile.BattleheartLegacy
            appData.setPromotedText(appJASONObj.get("bundleId").toString());
            //supported devices family
            appData.setPromotedScreenshot(appJASONObj.getJSONArray("deviceFamilies").toString());
            //current version total raters and average rating
            String currentRatings=appJASONObj.getJSONObject("userRating").getInt("ratingCountCurrentVersion")
                    +"_"+appJASONObj.getJSONObject("userRating").getDouble("valueCurrentVersion");
            appData.setPromotedVideo(currentRatings);
            //get recent changes
            JSONObject softwarePageObj=jsonObject.getJSONObject("pageData").getJSONObject("softwarePageData");
            appData.setRecentChanges(softwarePageObj.get("versionHistory").toString());
        }catch (Exception ex){
            log.error(ex);
            ex.printStackTrace();
            return false;
        }
        return true;
    }
    //deprecated after 6/4/2014
    @Deprecated
	public boolean overviewParser_old(StringBuilder overview,AppData appData) {
		// TODO Auto-generated method stub
        if(overview==null ||overview.length()==0|| appData==null){
            return false;
        }
		try{
			org.jsoup.nodes.Document doc = Jsoup.parse(overview.toString());
			boolean flag=true;
			boolean numberVectorFlag=true;
			boolean ratedFlag=true;
			boolean languageFlag=true;
			boolean ratedSummaryFlag=false;
			if(doc.getElementsByTag("Document").size()>0){
				appData.setDeveloperId(doc.getElementsByTag("Document").get(0).attr("artistId"));
			}
			for (Element ele: doc.getElementsByTag("VBoxView")){
				Elements overviewEles=ele.getElementsByTag("TextView");
				ArrayList<String> overviewInfo=new ArrayList<String>();
				for(Element overviewEle:overviewEles){
					String ovStr=overviewEle.text();
					if(!"".equalsIgnoreCase(ovStr)){
						overviewInfo.add(overviewEle.text());
					}
				}
				if(!(flag|numberVectorFlag|ratedFlag|languageFlag)){
					break;
				}
				if(flag&&overviewInfo.toString().contains("Category:")){
					appData.setAppName(overviewInfo.get(1));
					appData.setSubCategory(overviewInfo.get(2).split(":")[1].trim());
					String latestModification=overviewInfo.get(3).trim();
					int actionIndex=latestModification.indexOf(" ");
					appData.setLatestAction(latestModification.substring(0, actionIndex).trim());
					appData.setLatestModified(latestModification.substring(actionIndex).trim());
					
					appData.setCurrentVersion(overviewInfo.get(4).split(":")[1].trim());
					String []developer=overviewInfo.get(5).split(":");
					appData.setDeveloperName(developer[1].trim());
					appData.setDeveloperCompany(overviewInfo.get(6).trim());
					
					appData.setSize(overviewInfo.get(7));
					appData.setPrice(overviewInfo.get(8));
					Element thumbEle=ele.getElementsByAttribute("verticalAlignment").get(0);
					appData.setThumbNail(thumbEle.attr("url"));
					flag=false;
				}else if(ratedSummaryFlag||ratedFlag&&overviewInfo.toString().contains("Rated ")){
					if(ratedFlag){
						String tempStr="";
						for(String str:overviewInfo){
							if(str.contains("Rated ")){
								tempStr=str;
								break;
							}
						}
						String temp=(tempStr.split(" "))[1];
						appData.setContentRated(temp.substring(0, temp.length()-1));
					}
					if(ratedSummaryFlag){
						appData.setRatedSummary(overviewInfo.toString());
						ratedSummaryFlag=false;
					}
					if(overviewInfo.toString().contains("for the following")){
						ratedSummaryFlag=true;
					}
					ratedFlag=false;
				}else if(numberVectorFlag&&(overviewInfo.toString().contains("Average rating for all versions:")||overviewInfo.toString().contains("Average rating for the current version:"))){
					try{
//						log.info(overviewInfo);
						int totalRaters=0;
						float averageRating=0;
						if(overviewInfo.size()==8){
							for(int i=5;i>=1;i--){
								int temp=Integer.parseInt(overviewInfo.get(8-i));
								totalRaters+=temp;
								averageRating+=temp*i;
								appData.setRatingNum(i,temp);
							}
							if(totalRaters>0){
								appData.setAverageRating(averageRating/totalRaters);
							}else{
								appData.setAverageRating(0);
							}
							appData.setTotalRaters(totalRaters);
//							numberVectorFlag=false;
						}
						
					}catch(Exception ex){
						log.error(ex);
						log.info(overviewInfo);
						ex.printStackTrace();
					}
				}else if(languageFlag&&overviewInfo.toString().contains("LANGUAGES:")){
					try{
						if(overviewInfo.size()>=8){
							appData.setDescription(overviewInfo.get(1));
							appData.setRecentChanges(overviewInfo.get(3));
							appData.setLanguage(overviewInfo.get(5));
							appData.setRequirements(overviewInfo.get(7));
						}else{
							appData.setDescription(overviewInfo.get(1));
							appData.setLanguage(overviewInfo.get(3));
							appData.setRequirements(overviewInfo.get(5));
						}
						
						languageFlag=false;
					}catch(Exception ex){
						log.error(ex);
						log.info(overviewInfo);
						ex.printStackTrace();
					}
				}
			}
            return true;
		}catch(Exception ex){
			log.error(ex);
			ex.printStackTrace();
		}
		return false;
	}


	@Override
	public boolean CommentsParser(StringBuilder commentsPage,
        int currentPageNum,AppData appData) {
		boolean nextPageFlag=false;
		try{
			MultiDateFormat sdf2=new MultiDateFormat("MMM dd, yyyy");
			JSONObject jsonObject= XML.toJSONObject(commentsPage.toString());
			JSONObject vBoxView=(JSONObject) jsonObject.getJSONObject("Document").getJSONObject("View")
					.getJSONObject("ScrollView").getJSONObject("VBoxView").getJSONObject("View")
					.getJSONObject("MatrixView").getJSONArray("VBoxView").get(0);
            Object pageObj=vBoxView.getJSONObject("VBoxView").get("HBoxView");
            if(pageObj instanceof  JSONArray){//have other pages
                JSONObject pageNumberObj=(JSONObject)vBoxView.getJSONObject("VBoxView").getJSONArray("HBoxView").get(1);
                String pageNumStr=pageNumberObj.getJSONObject("TextView").getJSONObject("SetFontStyle").getString("b");
                String []pageNums=pageNumStr.split(" ");
                int currentPages=Integer.parseInt(pageNums[1]);
                int totalPages=Integer.parseInt(pageNums[3]);
                if(currentPages<totalPages){
                    nextPageFlag= true;
                }
            }
//            nextPageFlag=false;
            JSONObject commentJObjsTemp=vBoxView.getJSONObject("VBoxView");
            if(!commentJObjsTemp.keySet().contains("VBoxView") ||
                    vBoxView.getJSONObject("VBoxView").get("VBoxView") instanceof  JSONObject){//no review was found
                return nextPageFlag;
            }
            //If for test, only download one page of comments
            //nextPageFlag = false;
            //parse rating summary of 1 star 2 star 3 star ... raters
            if(!appData.isRatingNumSet()){
                Object ratingObjTemp=vBoxView.get("HBoxView");
                JSONArray currentRatingJObj=null;
                boolean allRJ=false;
                if(ratingObjTemp instanceof  JSONObject){//have both current & all rating summary
                    currentRatingJObj=vBoxView.getJSONObject("HBoxView").getJSONArray("VBoxView").getJSONObject(1)
                            .getJSONObject("VBoxView").getJSONObject("View").getJSONObject("View")
                            .getJSONObject("View").getJSONObject("VBoxView").getJSONArray("Test")
                            .getJSONObject(1).getJSONArray("VBoxView").getJSONObject(0).getJSONObject("MatrixView")
                            .getJSONArray("VBoxView").getJSONObject(0).getJSONArray("HBoxView");
                    allRJ=true;
                }else if(ratingObjTemp instanceof  JSONArray){//only have current rating summary
                    currentRatingJObj=vBoxView.getJSONArray("HBoxView").getJSONObject(0).getJSONArray("VBoxView").getJSONObject(1)
                            .getJSONObject("VBoxView").getJSONObject("View").getJSONObject("View")
                            .getJSONObject("View").getJSONObject("VBoxView").getJSONObject("Test")
                            .getJSONArray("VBoxView").getJSONObject(1).getJSONObject("MatrixView")
                            .getJSONArray("VBoxView").getJSONObject(0).getJSONArray("HBoxView");
                }
                if(allRJ) {
                    try {
                        JSONArray customerRatingsAll = vBoxView.getJSONObject("HBoxView").getJSONArray("VBoxView").getJSONObject(1)
                                .getJSONObject("VBoxView").getJSONObject("View").getJSONObject("View")
                                .getJSONObject("View").getJSONObject("VBoxView").getJSONArray("Test")
                                .getJSONObject(0).getJSONArray("VBoxView").getJSONObject(1).getJSONObject("MatrixView")
                                .getJSONArray("VBoxView").getJSONObject(0).getJSONArray("HBoxView");
                        for (int i = 0; i < 5; i++) {
                            JSONObject alLRating = customerRatingsAll.getJSONObject(i);
                            String arTemp = alLRating.getString("alt").split(",")[1].trim().split(" ")[0].trim();
                            appData.setRatingNum(5-i,Integer.parseInt(arTemp));
                        }
                    } catch (JSONException ex) {
                        log.error(ex);
                    }
                }
                if(currentRatingJObj!=null) {
                    try {
                        String curRatingScore="";
                        for (int i = 0; i < 5; i++) {
                            JSONObject curRating = currentRatingJObj.getJSONObject(i);
                            String arTemp = curRating.getString("alt").split(",")[1].trim().split(" ")[0].trim();
                            curRatingScore="_"+arTemp+curRatingScore;
                        }
                        appData.setPromotedVideo(appData.getPromotedVideo()+curRatingScore);
                    } catch (JSONException ex) {
                        log.error(ex);
                    }
                }
            }
            JSONArray commentJObjs=vBoxView.getJSONObject("VBoxView").getJSONArray("VBoxView");
			for(int i=0;commentJObjs!=null&&i<commentJObjs.length();i++){
				Comment comment=new Comment();
				JSONObject cjobj=commentJObjs.getJSONObject(i);
				JSONArray  hboxArray=cjobj.getJSONArray("HBoxView");
				String commentTitle=hboxArray.getJSONObject(0).getJSONObject("TextView").getJSONObject("SetFontStyle").get("b").toString();
				comment.setCommentTitle(commentTitle);
				
				String ratingStr=hboxArray.getJSONObject(0).getJSONObject("HBoxView").getJSONArray("HBoxView").getJSONObject(0).getString("alt");
				comment.setRating(Float.parseFloat(ratingStr.split(" ")[0]));
				
				JSONObject commentRevewerObj=hboxArray.getJSONObject(1).getJSONObject("TextView").getJSONObject("SetFontStyle");
				if(commentRevewerObj.has("GotoURL")){//normal case
					try {
						String commentMetaStr=commentRevewerObj.getJSONArray("content").getString(1);
						String []tempStr=commentMetaStr.split("-");
						String dataStr="";
						int specialIndex=0;
						if(commentMetaStr.contains("Version")){
							comment.setDeviceVersion(tempStr[1].trim().substring(8));
							specialIndex=2;
						}else{
							specialIndex=1;
						}
						if(tempStr.length>specialIndex+1){
							dataStr=tempStr[specialIndex+1]+" "+tempStr[specialIndex]+", "+tempStr[specialIndex+2];
						}else{
							dataStr=tempStr[specialIndex].trim();
						}
						String userNameStr=commentRevewerObj.getJSONObject("GotoURL").get("b").toString();
						comment.setReviewer(userNameStr);
						String []userProfileIdStr=commentRevewerObj.getJSONObject("GotoURL").getString("url").split("=");
						comment.setReviewerId(userProfileIdStr[userProfileIdStr.length-1]);
                        comment.setDate(sdf2.parse(dataStr));
                    } catch (Exception e) {
						log.error(e);
						log.error(commentRevewerObj);
						e.printStackTrace();
                        continue;//skip this comment if meeting any error
					}
				}else{//review by anonymous
					comment.setDate(sdf2.parse("Jan 01, 1990"));
					comment.setReviewer(commentRevewerObj.get("b").toString());
				}
				JSONObject commentHelpfulnessObj=hboxArray.getJSONObject(2).getJSONObject("HBoxView");
				if(commentHelpfulnessObj.getJSONArray("TextView").length()>2){
					String eleHelpfulnessStr=((JSONObject)(commentHelpfulnessObj.getJSONArray("TextView").get(0)))
							.getJSONObject("SetFontStyle").get("b").toString();					
					String []tempArray=eleHelpfulnessStr.split(" ");
					try{
						int agree=Integer.parseInt(tempArray[0]);
						int total=Integer.parseInt(tempArray[3]);
						float helpfulnessRatio=(float)agree/total;
						comment.setHelpfulnessAgree(agree);
						comment.setHelpfulnessTotal(total);
						comment.setHelpfulnessRatio(helpfulnessRatio);
					}catch(Exception ex){
						log.error(ex);
						ex.printStackTrace();
					}
				}
				String userReviewIdStr=commentHelpfulnessObj.getString("viewName");
				comment.setReviewId(userReviewIdStr.substring(8));
				String commentStr=cjobj.getJSONObject("TextView").getJSONObject("SetFontStyle").get("content").toString();
				comment.setComment(commentStr);
			
				//For update only
				if(appData.getLatestReviewId()!=null&&appData.getLatestReviewId().equalsIgnoreCase(comment.getReviewId())){
					return false;
				}else if(comment!=null){
					appData.addComment(comment);
				}
			
			}
		}catch(Exception ex){
			log.error(ex.getMessage());
			ex.printStackTrace();
//            appData.getCommentList().clear(); // remove all
//			return this.CommentsParser_backup(commentsPage.toString(), commentsList, currentPageNum, latestReviewId);
		}
		return nextPageFlag;
	}
    @Deprecated
	public boolean CommentsParser_backup(String commentsPage,
			ConcurrentLinkedQueue<Comment> commentsList, int currentPageNum,String latestReviewId) {
		// TODO Auto-generated method stub
		log.info("Start CommentsParser_backup");
		try{
			org.jsoup.nodes.Document doc = Jsoup.parse(commentsPage);
			boolean nextPageFlag=false;
			boolean helpfulnessFlag=true;
			MultiDateFormat sdf2=new MultiDateFormat("MMM dd, yyyy");
			for (Element ele: doc.getElementsByTag("VBoxView")){
				if(ele.attr("leftInset").equalsIgnoreCase("10")&&
				   ele.attr("rightInset").equalsIgnoreCase("0")&&
				   ele.attr("stretchiness").equalsIgnoreCase("1")){//first level
					Comment comment=new Comment();
					Elements textEles=ele.getElementsByAttributeValue("styleSet", "basic13");
					comment.setCommentTitle(textEles.get(0).text());
					String []reviewerInfos=null;
					String dateString=null;
					try {
						String reviewerStr=textEles.get(1).text();
						int indexVersion=reviewerStr.lastIndexOf("Version");
						if(indexVersion>3){
							comment.setReviewer(reviewerStr.substring(3, indexVersion-2).trim());
							int indexHyhen=reviewerStr.indexOf("-", indexVersion);
							if(indexHyhen>indexVersion){
								comment.setDeviceVersion(reviewerStr.substring(indexVersion+7,indexHyhen).trim());
							}
							dateString=reviewerStr.substring(indexHyhen+1).trim();
							if(dateString.contains("-")){
								reviewerInfos=dateString.split("-");
								int len=reviewerInfos.length;
								if(len==3){
									dateString=reviewerInfos[len-2].trim()+" "+reviewerInfos[len-3].trim()+","+" "+reviewerInfos[len-1].trim();
								}
							}
							comment.setDate(sdf2.parse(dateString));
						}else{
							comment.setReviewer(reviewerStr.substring(3).trim());
							comment.setDate(sdf2.parse("Jan 01, 1990"));
						}
						
						
						if(helpfulnessFlag){
							for(Element eleHelpfulness:ele.getElementsByTag("textview")){
								if(eleHelpfulness.text().matches("[0-9]* out of [0-9]* customers found this review helpful")){
									String []temp=eleHelpfulness.text().split(" ");
									try{
										int agree=Integer.parseInt(temp[0]);
										int total=Integer.parseInt(temp[3]);
										float helpfulnessRatio=(float)agree/total;
										comment.setHelpfulnessAgree(agree);
										comment.setHelpfulnessTotal(total);
										comment.setHelpfulnessRatio(helpfulnessRatio);
										helpfulnessFlag=false;
									}catch(Exception ex){
										log.error(ex);
										ex.printStackTrace(	);
									}
								}
								
							}
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						log.error(e);
						log.info(textEles.get(1).text());
						log.info(dateString);
						e.printStackTrace();
					}
					Element ele2=ele.getElementsByAttributeValue("bottomInset", "3").get(0);
					Pattern p=Pattern.compile("[0-9]* star[s]*");
					Elements ele3=ele2.getElementsByAttributeValueMatching("alt", p);
					if(ele3.size()>0){
						String ratingStr=ele3.get(0).attr("alt");
						comment.setRating(Float.parseFloat(ratingStr.split(" ")[0]));
					}//Rating star
											
					Pattern p2=Pattern.compile("userProfileId=[0-9]+$");
					Elements userIdEle=ele.getElementsByAttributeValueMatching("url",p2);
					if(userIdEle.size()>0){
						String []userProfileIdStr=userIdEle.get(0).attr("url").split("=");
						comment.setReviewerId(userProfileIdStr[userProfileIdStr.length-1]);
					}
					Pattern p3=Pattern.compile("userReviewId=[0-9]+$");
					Elements reviewIdEle=ele.getElementsByAttributeValueMatching("url",p3);
					String []userReviewIdStr=reviewIdEle.get(0).attr("url").split("=");
					comment.setReviewId(userReviewIdStr[userReviewIdStr.length-1]);
					
					Element reviewEle=ele.getElementsByAttributeValue("styleSet","normal11").get(0);
					comment.setComment(reviewEle.text());
					//For update only
					if(latestReviewId!=null&&latestReviewId.equalsIgnoreCase(comment.getReviewId())){
						return false;
					}else if(comment!=null){
						commentsList.add(comment);
					}
					
				}else{
					Elements pagesEles=ele.getElementsByTag("HBoxView");
					for(Element pagesEle:pagesEles){
						if(pagesEle.text().matches("Page [0-9]+ of [0-9]+")){
							String pageNumStr=pagesEle.text();
							String []pageNums=pageNumStr.split(" ");
							int currentPages=Integer.parseInt(pageNums[1]);
							int totalPages=Integer.parseInt(pageNums[3]);
							if(currentPages>=totalPages){
								nextPageFlag= false;
							}else{
								nextPageFlag= true;
							}
						}
					}
				}
			}
			log.debug("End CommentsParser_backup");

			return nextPageFlag;
		}catch(Exception ex){
			log.error(ex);
		}
		return false;
	}
	@Override
	public boolean RankParser(ConcurrentLinkedQueue<AppRatingInfo> ariList,StringBuilder rankPage,String ...param) {
		try{
			log.debug("Start to parse rankpage...");
			org.jsoup.nodes.Document doc = Jsoup.parse(rankPage.toString());
			for (Element ele: doc.getElementsByAttributeValueMatching("class","lockup small detailed option[ a-z]*")){
				String appId=ele.attr("adam-id");
				String appName=ele.attr("aria-label");
				String rankStr=ele.getElementsByClass("index").text();
				int rank=Integer.parseInt(rankStr.substring(0, rankStr.length()-1));
				AppRatingInfo ari=new AppRatingInfo(param[0],param[1],param[2],appId,new Date(),rank,appName);
				ariList.add(ari);
				log.info("Rank=	"+rank+" appId="+appId);
			}
		}catch(Exception ex){
			log.error(ex);
			return false;
		}
		return false;//only one page
	}
	@Override
	public boolean userProfileParser(Reviewer reviewer,StringBuilder userProfileStr) {
		// TODO Auto-generated method stub
		String appUrl=null;
		try{
			log.debug("Start to parse the user profile of "+reviewer.getReviewerId());
			org.jsoup.nodes.Document doc = Jsoup.parse(userProfileStr.toString());
			for (Element mbEle: doc.getElementsByClass("main-block")){
				Elements nameEles=mbEle.getElementsByClass("name");
				if(nameEles.size()==0){
					continue;
				}
				Element nameEle=nameEles.get(0);
				appUrl=nameEle.getElementsByTag("a").get(0).attr("href");
				int index0=appUrl.indexOf("/id");
				int index1=appUrl.indexOf("?", index0);
				if(index1==-1){//no ? exist, just skip it
					index1=appUrl.length();
				}
				String appId=appUrl.substring(index0+3, index1);
				String appRatingStr=mbEle.getElementsByClass("rating").get(0).attr("aria-label");
				String appRating=appRatingStr.split(" ")[0];
				String reveiwDate=mbEle.getElementsByClass("review-date").get(0).text().trim();
				Elements developerIdEles=mbEle.getElementsByClass("artist");
				String developerId="UNKNOWN";
				if(developerIdEles.size()>0){
					 developerId=developerIdEles.get(0).text();
				}
				Elements versionEles=mbEle.getElementsByTag("button");
				String appVersion="dummy";
				if(versionEles.size()>0){
					appVersion=versionEles.get(0).attr("bundle-short-version");
				}
				//TODO add app version
				reviewer.addReviewId(appId, appRating, reveiwDate,developerId,appVersion);
			}
			Elements pageInfoEles=doc.getElementsByClass("paginated-content");
			String currentPage="";
			String totalPages="";
			if(pageInfoEles.size()>0){
				Element pageInfoEle=pageInfoEles.get(0);
				currentPage=pageInfoEle.attr("page-number");
				totalPages=pageInfoEle.attr("total-number-of-pages");
				Integer curPageNum=Integer.parseInt(currentPage.trim());
				Integer totalPageNum=Integer.parseInt(totalPages.trim());
				if(curPageNum==totalPageNum){
					return false;
				}else if(curPageNum<totalPageNum){
					return true;
				}
			}
			log.debug("Finish parsing the user profile of "+reviewer.getReviewerId());
		}catch(Exception ex){
			log.info(appUrl);
			log.error(ex.getMessage());
			ex.printStackTrace();
		}
		return false;
	}

    @Override
    public boolean developerProfileParser(Developer developer, StringBuilder developerProfileStr) {
        try{
            log.debug("Start to parse the developer profile of "+developer.getDeveloperId());
            org.jsoup.nodes.Document doc = Jsoup.parse(developerProfileStr.toString());
            Elements body= doc.getElementsByClass("center-stack");
            boolean flag=false;//indicate where there is a next page
            if(body.size()==1) {
                Element centerStack = body.get(0);
                Elements albums=centerStack.select("div.lockup-container.paginate.application");
                for(Element album:albums) {
                    try {// in case one albums has error
                        String albumType="ios";
                        String []types=album.attr("see-all-href").split("&");
                        for (String type:types){
                            String key="softwareType=";
                            if(type.startsWith(key)){
                                albumType=type.substring(key.length());
                                break;
                            }
                        }
                        HashSet<String> appIds=new HashSet<String>();
                        Elements apps = album.getElementsByClass("artwork-link");
                        for (Element app : apps) {
                            String href = app.attr("href").split("/id")[1];
                            String appId = href.substring(0, href.length() - 5);
                            appIds.add(appId.trim());
                        }
                        developer.setAppList(albumType,appIds);
                        //check whether there has a next page to be crawled
                        Elements paginates=album.select("div.paginated-content.elastic-lockups");
                        if(paginates.size()==1){
                            Element paginate=paginates.get(0);
                            String currentPageStr=paginate.attr("page-number");
                            String totalPageStr=paginate.attr("total-number-of-pages");
                            int currentPage=Integer.parseInt(currentPageStr.trim());
                            int lastPage=Integer.parseInt(totalPageStr.trim());
                            flag=flag||currentPage<lastPage;
                        }
                    }catch (Exception ex){
                        log.error(ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
            log.debug("Finish parsing the developer profile of "+developer.getDeveloperId());
            return flag;
        }catch(Exception ex){
            log.error(ex.getMessage());
            ex.printStackTrace();
        }
        return false;
    }

}

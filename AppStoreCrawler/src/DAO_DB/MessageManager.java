package DAO_DB;



import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import configuration.CrawlerConfig;

import data.*;
import utility.MultiDateFormat;
import utility.SystemLogger;

public class MessageManager extends DataManager{
	private static SystemLogger log = SystemLogger.getLogger(MessageManager.class);
	
	public MessageManager() throws DataControlException{
		
	}
	public MessageManager(DbObject dbo){
		super(dbo);
	}
	
	public int insertToAppData(String databaseName,AppData appData){
		String sqlStr=null;
		MultiDateFormat sdf=new MultiDateFormat("yyyy-MM-dd HH:mm:ss");
		try{
			if(appData.getLatestModified()==null){
				appData.setLatestModified("1990-01-01 00:00:00");
			}
			String sqlStatement="(app_id,app_name,	developer_id,developer_name,developer_company," +
					"average_rating,total_raters,rated_summary,\"1star_num\",\"2star_num\",\"3star_num\"," +
					"\"4star_num\",	\"5star_num\",	current_version,required_android_version,category,sub_category,	" +
					"price,currency,thumbnail,store_location,crawler_point,description,latest_modified,	" +
					"latest_action,installs,content_rated,language,	requirements,url,	latest_reveiw_id,size," +
					"permission,contact_phone,contact_email,contact_website,	promoted_text,	" +
					"promoted_screenshot,	Promoted_video,recent_changes,rank) values("+
					this.makeSqlStr(appData.getAppId())+","+this.makeSqlStr(appData.getAppName())+","+	this.makeSqlStr(appData.getDeveloperId())+","+	this.makeSqlStr(appData.getDeveloperName())+","
					+	this.makeSqlStr(appData.getDeveloperCompany())+","+	this.makeSqlStr(appData.getAverageRating())+","+	this.makeSqlStr(appData.getTotalRaters())+","+	this.makeSqlStr(appData.getRatedSummary())+","
					+	this.makeSqlStr(appData.getRatingNum(1))+","+	this.makeSqlStr(appData.getRatingNum(2))+","+	this.makeSqlStr(appData.getRatingNum(3))+","+	this.makeSqlStr(appData.getRatingNum(4))+","
					+	this.makeSqlStr(appData.getRatingNum(5))+","+	this.makeSqlStr(appData.getCurrentVersion())+","+	this.makeSqlStr(appData.getRequiredAndroidVersion())+","+	this.makeSqlStr(appData.getCategory())+","
					+	this.makeSqlStr(appData.getSubCategory())+","+	this.makeSqlStr(appData.getPrice())+","+	this.makeSqlStr(appData.getCurrency())+","+	this.makeSqlStr(appData.getThumbNail())+","+	this.makeSqlStr(appData.getStoreLocation())+","
					+	this.makeSqlStr(appData.getCrawlerPoint())+","+	this.makeSqlStr(appData.getDescription())+","+	this.makeSqlStr(sdf.selfFormat(sdf.parse(appData.getLatestModified())))+","+	this.makeSqlStr(appData.getLatestAction())+","+	this.makeSqlStr(appData.getInstalls())+","
					+	this.makeSqlStr(appData.getContentRated())+","+	this.makeSqlStr(appData.getLanguage())+","+	this.makeSqlStr(appData.getRequirements())+","+	this.makeSqlStr(appData.getUrl())+","+	this.makeSqlStr(appData.getLatestReviewId())+","+	this.makeSqlStr(appData.getSize())+","
					+	this.makeSqlStr(appData.getPermission())+","+	this.makeSqlStr(appData.getContactPhone())+","+	this.makeSqlStr(appData.getContactEmail())+","+	this.makeSqlStr(appData.getContactWebsite())+","+	this.makeSqlStr(appData.getPromotedText())+","+	this.makeSqlStr(appData.getPromotedScreenshot())+","
					+	this.makeSqlStr(appData.getPromotedVideo())+","+	this.makeSqlStr(appData.getRecentChanges())+","+	this.makeSqlStr(appData.getRank())+")";
			
//			sqlStr="UPDATE "+databaseName+".AppData SET "+sqlStatement+" WHERE app_id="+this.makeSqlStr(appData.getAppId());
//			int result=this.doUpdate(sqlStr);
			int result=0;
			if(result==0){
				log.info("Insert "+this.makeSqlStr(appData.getAppId())+" in the database");
//				sqlStr="INSERT INTO "+databaseName+".AppData SET "+sqlStatement;
				sqlStr="INSERT INTO "+"AppData  "+sqlStatement;
				result=this.doInsert(sqlStr);
			}else{
				log.info("Update "+this.makeSqlStr(appData.getAppId())+" in the database");
			}

			return result;
		}catch(Exception ex){
			log.error(sqlStr);
			log.error(ex);
			ex.printStackTrace();
		}finally{
			this.closeSession();
		}
		return -1;
	}
	public String loadLatestReveiwIdForApp(String databaseName,String appId){
		String sqlStr=null;
		String latestReveiwId=null;
		try{
			sqlStr="SELECT latest_reveiw_id FROM "+databaseName+".AppData WHERE app_id="+this.makeSqlStr(appId)+" LIMIT 1";
			sqlStr="SELECT latest_reveiw_id FROM AppData WHERE app_id="+this.makeSqlStr(appId)+" LIMIT 1";

			ResultSet rs=this.doSelect(sqlStr);
			while(rs.next()){
				latestReveiwId=rs.getString("latest_reveiw_id");
			}
		}catch(Exception ex){
			log.error(sqlStr);
			log.error(ex);
		}finally{
			this.closeSession();
		}
		return latestReveiwId;
	}
	public int insertToAppRatings(String databaseName,AppRatingInfo ari){
		String sqlStr=null;
		int result=-1;
		try{
			String indexStr="day"+ari.getToday();
			String statement=
					"category="+this.makeSqlStr(ari.getCategory())+","+
					"store_location="+this.makeSqlStr(ari.getStore_location())+","+
					"crawler_point="+this.makeSqlStr(ari.getCrawler_point())+","+
					indexStr+"="+makeSqlStr(ari.getRating())+","+
					"title="+this.makeSqlStr(ari.getTitle())+","+
					"update_time=NOW()";
			String tableName="AppRatings_";
			if("NA".equals(ari.getStore_location())){
				tableName+=ari.getMonth();
			}else{
				tableName+=ari.getStore_location()+"_"+ari.getMonth();
			}
			//When the table is large, it is very time consuming
			sqlStr="UPDATE "+databaseName+"."+tableName+" SET "+statement
					+" WHERE app_id="+makeSqlStr(ari.getAppId())+" AND category= "+makeSqlStr(ari.getCategory())+" AND store_location="+makeSqlStr(ari.getStore_location())+ " AND crawler_point="+makeSqlStr(ari.getCrawler_point());
			result=this.doUpdate(sqlStr);
			if(result==0){
				statement="app_id="+this.makeSqlStr(ari.getAppId())+","+
				"category="+this.makeSqlStr(ari.getCategory())+","+
				"store_location="+this.makeSqlStr(ari.getStore_location())+","+
				"crawler_point="+this.makeSqlStr(ari.getCrawler_point())+","+
				indexStr+"="+makeSqlStr(ari.getRating())+","+
				"update_time=NOW()"+","+
				"title="+this.makeSqlStr(ari.getTitle())+"";
				sqlStr="INSERT INTO "+databaseName+"."+tableName+" SET "+statement;
				result= this.doInsert(sqlStr);
			}else{
				log.debug("UPDATE: "+sqlStr);
			}
		}catch(Exception ex){
			log.error(sqlStr);
			log.error(ex);
            ex.printStackTrace();
		}finally{
			this.closeSession();
		}
		return result;
	}
	public int insertToAppComment(String databaseName,Comment comment){
		String sqlStr=null;
		int result=-1;
		try{
			 MultiDateFormat sdf=new MultiDateFormat("yyyy-MM-dd HH:mm:ss");
			String sqlStatement="("+"reviewer,"+"reviewer_id,"+"date,"+"device,"+"device_version,"+"rating,"
							+"comment,"+"comment_title,"+"helpfulness_ratio,"+"helpfulness_agree,"+"helpfulness_total,"
					+"review_id,"+"app_id"+") values ("+this.makeSqlStr(comment.getReviewer())+","+this.makeSqlStr(comment.getReviewerId())+","
					+this.makeSqlStr(sdf.selfFormat(comment.getDate()))+","+this.makeSqlStr(comment.getDevice())+","+this.makeSqlStr(comment.getDeviceVersion())+","
					+this.makeSqlStr(comment.getRating())+","+this.makeSqlStr(comment.getComment())+","+this.makeSqlStr(comment.getCommentTitle())+","
					+comment.getHelpfulnessRatio()+","+comment.getHelpfulnessAgree()+","+comment.getHelpfulnessTotal()+","+this.makeSqlStr(comment.getReviewId())+","
					+this.makeSqlStr(comment.getAppId())+")";
			/**
			 * The reason to comment this part of code is due to the performance.
			 * It's really slow to update then insert for large traffic of comment.
			 * It is better to remove duplicated rows later rather than check it here.
			 */
//			 sqlStr="UPDATE "+databaseName+".Comment SET "+sqlStatement+" WHERE review_id="+makeSqlStr(comment.getReviewId());
			 result=0;//this.doUpdate(sqlStr);
			 if(result==0){
//				 sqlStr="INSERT INTO "+databaseName+".Comment SET " +sqlStatement;
				 sqlStr="INSERT INTO "+"Comment " +sqlStatement;

				 result= this.doInsert(sqlStr);
			 }
			
			return result;
		}catch(Exception ex){
			log.error(sqlStr);
			log.error(ex);
			ex.printStackTrace();
		}finally{
			this.closeSession();
		}
		return -1;
	}
	public int insertToReviewers(String databaseName,Reviewer reviewer){
		String sqlStr=null;
		int result=-1;
		try{
			 String appIdsOrdedStr="";
			 for(String reviewId:reviewer.getAppIdsOrded()){
				 appIdsOrdedStr+=reviewId+"_";
			 }
			 if(appIdsOrdedStr.length()>1){//delete the last colon
				appIdsOrdedStr=appIdsOrdedStr.substring(0, appIdsOrdedStr.length()-1);
			 }
			 String reviewDateStr="";
			 for(String dateStr:reviewer.getReviewDateStrings()){
				 reviewDateStr+=dateStr+"_";
			 }
			 if(reviewDateStr.length()>1){//delete the last colon
				 reviewDateStr=reviewDateStr.substring(0, reviewDateStr.length()-1);
			 }
			 
			 String reviewRatingStr="";
			 for(String ratingStr:reviewer.getReviewRatings()){
				 reviewRatingStr+=ratingStr+"_";
			 }
			 if(reviewRatingStr.length()>1){//delete the last colon
				 reviewRatingStr=reviewRatingStr.substring(0, reviewRatingStr.length()-1);
			 }
			 String developerIdStr="";
			 for(String didStr:reviewer.getDeveloperIdList()){
				 developerIdStr+=didStr+"_";
			 }
			 if(developerIdStr.length()>1){//delete the last colon
				 developerIdStr=developerIdStr.substring(0, developerIdStr.length()-1);
			 }
			 
			 String appVersionStr="";
			 for(String appVersion:reviewer.getAppVersionList()){
				 appVersionStr+=appVersion+"_";
			 }
			 if(appVersionStr.length()>1){//delete the last colon
				 appVersionStr=appVersionStr.substring(0, appVersionStr.length()-1);
			 }
			 
			 /*String sqlStatement=
					"reviewer_id="+this.makeSqlStr(reviewer.getReviewerId())+","+
					"app_ids_ordered="+this.makeSqlStr(appIdsOrdedStr)+","+
					"review_ratings="+this.makeSqlStr(reviewRatingStr)+","+
					"review_dates="+this.makeSqlStr(reviewDateStr)+","+
					"app_versions="+this.makeSqlStr(appVersionStr)+","+
					"size="+this.makeSqlStr(reviewer.getReviewSize())+","+
					"developer_id="+this.makeSqlStr(developerIdStr)+","+
					"app_id="+this.makeSqlStr(reviewer.getAppId())+","+
					"status="+this.makeSqlStr(reviewer.getStatus());*/
			String sqlStatement="(reviewer_id,app_ids_ordered,review_ratings,review_dates,app_versions,size,developer_id,app_id,status) values("
					+this.makeSqlStr(reviewer.getReviewerId())+","+this.makeSqlStr(appIdsOrdedStr)+","+this.makeSqlStr(reviewRatingStr)+","
					+this.makeSqlStr(reviewDateStr)+","+this.makeSqlStr(appVersionStr)+","+this.makeSqlStr(reviewer.getReviewSize())+","
					+this.makeSqlStr(developerIdStr)+","+this.makeSqlStr(reviewer.getAppId())+","+this.makeSqlStr(reviewer.getStatus())+")";
					
//			 sqlStr="UPDATE "+databaseName+".Reviewers SET "+sqlStatement+" WHERE reviewer_id="+makeSqlStr(reviewer.getReviewerId());
//			 result=this.doUpdate(sqlStr);
//			 if(result==0){
//				sqlStr="INSERT INTO "+databaseName+".Reviewers SET " +sqlStatement;
				sqlStr="INSERT INTO "+"Reviewers " +sqlStatement;
				result= this.doInsert(sqlStr);
//			 }
			
		}catch(Exception ex){
			log.error(sqlStr);
			log.error(ex);
			ex.printStackTrace();
		}finally{
			this.closeSession();
		}
		return -1;
	}
    //save app id into database
    public int insertAppIdToDB(String databaseName,AppId appId) {
        String sqlStr=null;
        try {
            StringBuilder sqlStatement = new StringBuilder();
            sqlStatement.append("(app_id,store_location,category,sub_category,crawler_point,date)");
            sqlStatement.append(" values( ");
            sqlStatement.append(this.makeSqlStr(appId.getAppId()) + ",");
            sqlStatement.append(this.makeSqlStr(appId.getStoreLocation()) + ",");
            sqlStatement.append(this.makeSqlStr(appId.getCategory()) + ",");
            sqlStatement.append(this.makeSqlStr(appId.getSubCategory()) + ",");
            sqlStatement.append(this.makeSqlStr(appId.getCrawlerPoint()) + ",");
            sqlStatement.append("now())");
            sqlStr = " INSERT INTO AppIds " + sqlStatement.toString();
            return this.doInsert(sqlStr);
        }catch (Exception ex){
            log.info(sqlStr);
            log.error(ex.getMessage());
            ex.printStackTrace();
        }finally {
            this.closeSession();
        }
        return -1;
    }
    //save developer into database
    public int insertDeveloperToDB(String databaseName,Developer developer){
        if(developer.isEmpty()){
            return -1;
        }
        String sqlStr=null;
        try {
            StringBuilder sqlStatement = new StringBuilder();
            sqlStatement.append("(developer_id,store_location,crawler_point,date,app_type,app_ids)");
            sqlStatement.append(" values( ");
            sqlStatement.append(this.makeSqlStr(developer.getDeveloperId()) + ",");
            sqlStatement.append(this.makeSqlStr(developer.getStoreLocation()) + ",");
            sqlStatement.append(this.makeSqlStr(developer.getCrawlerPoint()) + ",");
            sqlStatement.append("now(),");
            //set app type & app id list
            StringBuilder appTypes=new StringBuilder();
            StringBuilder appIds=new StringBuilder();;
            for(String appType:developer.getAppList().keySet()){
                appTypes.append("-"+appType);
                appIds.append("-");
                StringBuilder appIdsTemp=new StringBuilder();;
                for(String app:developer.getAppList().get(appType)){
                    appIdsTemp.append("_"+app);
                }
                appIdsTemp.delete(0,1);//remove the first "_"
                appIds.append(appIdsTemp);
            }
            appTypes.delete(0,1);//remove the first -
            appIds.delete(0,1);
            sqlStatement.append(this.makeSqlStr(appTypes.toString())).append(",");
            sqlStatement.append(this.makeSqlStr(appIds.toString())).append(")");
            sqlStr = " INSERT INTO Developers " + sqlStatement.toString();
            return this.doInsert(sqlStr);
        }catch (Exception ex){
            log.info(sqlStr);
            log.error(ex.getMessage());
            ex.printStackTrace();
        }finally {
            this.closeSession();
        }
        return -1;
    }

	public  ConcurrentLinkedQueue<String> loadReviewerIdFromComments(String databaseName,int startPoint){
		return loadReviewerIdFromComments(databaseName,"Comment",startPoint);
	}
	public  ConcurrentLinkedQueue<String> loadReviewerIdFromComments(String databaseName,String table,int startPoint){
		String sqlStr=null;
		try{
			ConcurrentLinkedQueue<String> reviewerIdList=new ConcurrentLinkedQueue<String>();
			sqlStr="SELECT distinct(reviewer_id) FROM "+databaseName+"."+table+" WHERE id>"+startPoint+" order by id asc";// 
			ResultSet rs=this.doSelect(sqlStr);
			while(rs.next()){
				String reviewerId=rs.getString("reviewer_id");
				if(reviewerId!=null){
					reviewerIdList.add(reviewerId);
				}
			}
			return reviewerIdList;
		}catch(Exception ex){
			log.info(sqlStr);
			log.error(ex);
		}finally{
			this.closeSession();
		}
		return null;
	}
	/**
	 * For correlation coefficient calculation only
	 */
	public  ConcurrentLinkedQueue<String> loadAppIds(String databaseName,int startPoint){
		String sqlStr=null;
		try{
			ConcurrentLinkedQueue<String> appIdsList=new ConcurrentLinkedQueue<String>();
			sqlStr="SELECT distinct(app_id) FROM "+databaseName+".AppData WHERE id>"+startPoint+" order by id asc";// 
			ResultSet rs=this.doSelect(sqlStr);
			while(rs.next()){
				String reviewerId=rs.getString("app_id");
				if(reviewerId!=null){
					appIdsList.add(reviewerId);
				}
			}
			return appIdsList;
		}catch(Exception ex){
			log.info(sqlStr);
			log.error(ex);
		}finally{
			this.closeSession();
		}
		return null;
	}
	
	public  ConcurrentLinkedQueue<AppData> loadAppIdFromAppRatings(String databaseName,String storeLocation,long startPoint){
		String sqlStr=null;
		try{
			ConcurrentLinkedQueue<AppData> appIdsList=new ConcurrentLinkedQueue<AppData>();
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			int month = cal.get(Calendar.MONTH)+1;
			do{
				String tableName="";
				if("".equals(storeLocation)){
					tableName+="AppRatings_"+month;
				}else{
					tableName+="AppRatings_"+storeLocation+"_"+month;
				}
				
				sqlStr="SELECT app_id,id,category, store_location,crawler_point FROM "+databaseName+"."+tableName+" WHERE id>"+startPoint+" group by app_id order by id asc";// 
				ResultSet rs=this.doSelect(sqlStr);
				while(rs.next()){
					String appId=rs.getString("app_id");
					if(appId!=null){
						AppData ad=new AppData(appId);
						ad.setCategory(rs.getString("category"));
						ad.setStoreLocation(rs.getString("store_location"));
						ad.setCrawlerPoint(rs.getString("crawler_point"));
						ad.setRank(rs.getInt("id"));
						appIdsList.add(ad);
					}
				}
				month--;
			}while(appIdsList.size()==0&&month>=1);
			return appIdsList;
		}catch(Exception ex){
			log.info(sqlStr);
			log.error(ex);
		}finally{
			this.closeSession();
		}
		return null;
	}
	public AppData loadAppDataFromDB(String appId){
		AppData appData=null;
		try{
			String appDataTB= CrawlerConfig.getInstance().getTbNameAppData();
			String commentTB=CrawlerConfig.getInstance().getTbNameComment();
			String reviewersTB=CrawlerConfig.getInstance().getTbNameReviewers();
			String adSql="SELECT * FROM "+appDataTB+" WHERE app_id="+this.makeSqlStr(appId);
			ResultSet rs=this.doSelect(adSql);
			while(rs.next()){
				if(appData==null){
					appData=new AppData(appId);
				}
//				appData.setAppName(rs.getString("app_name"));
//				appData.setDeveloperId(rs.getString("Developer_id"));
//				appData.setDeveloperName(rs.getString("Developer_name"));
//				appData.setDeveloperCompany(rs.getString("Developer_company"));
				appData.setAverageRating(rs.getFloat("average_rating"));
				appData.setTotalRaters(rs.getInt("total_raters"));
//				appData.setRatedSummary(rs.getString("Rated_summary"));
				int []ratingNum=new int[5];
				ratingNum[0]=rs.getInt("1star_num");
				ratingNum[1]=rs.getInt("2star_num");
				ratingNum[2]=rs.getInt("3star_num");
				ratingNum[3]=rs.getInt("4star_num");
				ratingNum[4]=rs.getInt("5star_num");
				appData.setRatingNum(ratingNum);
//				appData.setCurrentVersion(rs.getString("current_version"));
//				appData.setRequiredAndroidVersion(rs.getString("required_android_version"));
//				appData.setCategory(rs.getString("category"));
//				appData.setSubCategory(rs.getString("sub_category"));
//				appData.setPrice(rs.getString("price"));
//				appData.setCurrency(rs.getString("currency"));
//				appData.setThumbNail(rs.getString("thumbnail"));
//				appData.setStoreLocation(rs.getString("store_location"));
//				appData.setCrawlerPoint(rs.getString("crawler_point"));
//				appData.setDescription(rs.getString("Description"));
//				appData.setLatestModified(rs.getString("Latest_modified"));
//				appData.setLatestAction(rs.getString("Latest_action"));
//				appData.setInstalls(rs.getString("Installs"));
//				appData.setContentRated(rs.getString("Content_rated"));
//				appData.setLanguage(rs.getString("Language"));
//				appData.setRequirements(rs.getString("Requirements"));
//				appData.setUrl(rs.getString("url"));
//				appData.setSize(rs.getString("size"));
//				appData.setPermission(rs.getString("Permission"));
//				appData.setContactPhone(rs.getString("Contact_phone"));
//				appData.setContactWebsite(rs.getString("Contact_website"));
//				appData.setContactEmail(rs.getString("Contact_email"));
//				appData.setPromotedText(rs.getString("Promoted_text"));
//				appData.setPromotedScreenshot(rs.getString("Promoted_screenshot"));
//				appData.setPromotedVideo(rs.getString("Promoted_video"));
//				appData.setRecentChanges(rs.getString("Recent_changes"));
//				appData.setLatestReviewId(rs.getString("latest_review_id"));
//				appData.setRank(rs.getInt("rank"));
				String commentSql="SELECT * FROM "+commentTB+" WHERE app_id ="+this.makeSqlStr(appId)+" ORDER BY date asc";
				ResultSet rsComment=this.doSelect(commentSql);
				MultiDateFormat sdf=new MultiDateFormat("MM/dd/yy HH:mm aa");

				while(rsComment.next()){
					Comment comment=new Comment();
//					comment.setReviewer(rsComment.getString("reviewer"));
					comment.setReviewerId(rsComment.getString("reviewer_id"));
					comment.setDate(sdf.parse(rsComment.getString(("date"))));
//					comment.setDevice(rsComment.getString("device"));
					comment.setDeviceVersion(rsComment.getString("device_version"));
					comment.setRating(rsComment.getFloat("rating"));
//					comment.setComment(rsComment.getString("comment"));
//					comment.setCommentTitle(rsComment.getString("comment_title"));
//					comment.setHelpfulnessRatio(rsComment.getFloat("helpfulness_ratio"));
//					comment.setHelpfulnessAgree(rsComment.getInt("helpfulness_agree"));
//					comment.setHelpfulnessTotal(rsComment.getInt("helpfulness_total"));
//					comment.setReviewId(rsComment.getString("review_id"));
					comment.setAppId(rsComment.getString("app_id"));
					appData.addComment(comment);

				}
				rsComment.close();
				String reviewSql="SELECT * FROM "+reviewersTB+" WHERE app_ids_ordered like '%"+appData.getAppId()+"%'";
//				String reviewSql="SELECT * FROM "+reviewersTB+" WHERE app_id='"+appData.getAppId()+"'";

				ResultSet rsReviewer=this.doSelect(reviewSql);
				while(rsReviewer.next()){
					Reviewer reviewer=new Reviewer(rsReviewer.getString("reviewer_id"));
//					reviewer.add(rsReviewer.getString("app_ids_ordered"), rsReviewer.getString("review_ratings"), rsReviewer.getString("review_dates"), rsReviewer.getString("developer_id"), rsReviewer.getString("app_versions"), rsReviewer.getInt("size"));
					reviewer.add(rsReviewer.getString("app_ids_ordered"), rsReviewer.getString("review_ratings"), rsReviewer.getString("review_dates"), null, rsReviewer.getString("app_versions"), rsReviewer.getInt("size"));
					appData.getReviewList().add(reviewer);
				}
				rsReviewer.close();
			}
			rs.close();
		}catch(Exception ex){
			log.error(ex.getMessage());
			ex.printStackTrace();
		}finally{
			this.closeSession();
		}
		
		return appData;
	}
	public AppData loadAppDataFromDBWithoutReviewer(String appId){
		AppData appData=null;
		try{
			String adSql="SELECT * FROM AppData WHERE app_id="+this.makeSqlStr(appId);
			ResultSet rs=this.doSelect(adSql);
			while(rs.next()){
				if(appData==null){
					appData=new AppData(appId);
				}
				appData.setAppName(rs.getString("app_name"));
//				appData.setDeveloperId(rs.getString("Developer_id"));
//				appData.setDeveloperName(rs.getString("Developer_name"));
//				appData.setDeveloperCompany(rs.getString("Developer_company"));
				appData.setAverageRating(rs.getFloat("average_rating"));
//				appData.setTotalRaters(rs.getInt("total_raters"));
//				appData.setRatedSummary(rs.getString("Rated_summary"));
//				int []ratingNum=new int[5];
//				ratingNum[0]=rs.getInt("1star_num");
//				ratingNum[1]=rs.getInt("2star_num");
//				ratingNum[2]=rs.getInt("3star_num");
//				ratingNum[3]=rs.getInt("4star_num");
//				ratingNum[4]=rs.getInt("5star_num");
//				appData.setRatingNum(ratingNum);
//				appData.setCurrentVersion(rs.getString("current_version"));
//				appData.setRequiredAndroidVersion(rs.getString("required_android_version"));
//				appData.setCategory(rs.getString("category"));
//				appData.setSubCategory(rs.getString("sub_category"));
//				appData.setPrice(rs.getString("price"));
//				appData.setCurrency(rs.getString("currency"));
//				appData.setThumbNail(rs.getString("thumbnail"));
//				appData.setStoreLocation(rs.getString("store_location"));
//				appData.setCrawlerPoint(rs.getString("crawler_point"));
//				appData.setDescription(rs.getString("Description"));
//				appData.setLatestModified(rs.getString("Latest_modified"));
//				appData.setLatestAction(rs.getString("Latest_action"));
//				appData.setInstalls(rs.getString("Installs"));
//				appData.setContentRated(rs.getString("Content_rated"));
//				appData.setLanguage(rs.getString("Language"));
//				appData.setRequirements(rs.getString("Requirements"));
//				appData.setUrl(rs.getString("url"));
//				appData.setSize(rs.getString("size"));
//				appData.setPermission(rs.getString("Permission"));
//				appData.setContactPhone(rs.getString("Contact_phone"));
//				appData.setContactWebsite(rs.getString("Contact_website"));
//				appData.setContactEmail(rs.getString("Contact_email"));
//				appData.setPromotedText(rs.getString("Promoted_text"));
//				appData.setPromotedScreenshot(rs.getString("Promoted_screenshot"));
//				appData.setPromotedVideo(rs.getString("Promoted_video"));
//				appData.setRecentChanges(rs.getString("Recent_changes"));
//				appData.setLatestReviewId(rs.getString("latest_review_id"));
//				appData.setRank(rs.getInt("rank"));
				String commentSql="SELECT * FROM Comment WHERE app_id ="+this.makeSqlStr(appId)+" ORDER BY date asc";
				ResultSet rsComment=this.doSelect(commentSql);
				MultiDateFormat sdf=new MultiDateFormat("MM/dd/yy HH:mm aa");

				while(rsComment.next()){
					Comment comment=new Comment();
					comment.setReviewer(rsComment.getString("reviewer"));
					comment.setReviewerId(rsComment.getString("reviewer_id"));
					comment.setDate(sdf.parse(rsComment.getString(("date"))));
					comment.setDevice(rsComment.getString("device"));
					comment.setDeviceVersion(rsComment.getString("device_version"));
					comment.setRating(rsComment.getFloat("rating"));
					comment.setComment(rsComment.getString("comment"));
					comment.setCommentTitle(rsComment.getString("comment_title"));
//					comment.setHelpfulnessRatio(rsComment.getFloat("helpfulness_ratio"));
//					comment.setHelpfulnessAgree(rsComment.getInt("helpfulness_agree"));
//					comment.setHelpfulnessTotal(rsComment.getInt("helpfulness_total"));
					comment.setReviewerId(rsComment.getString("review_id"));
					comment.setAppId(rsComment.getString("app_id"));
					appData.addComment(comment);

				}
				rsComment.close();
			}
			rs.close();
		}catch(Exception ex){
			log.error(ex.getMessage());
			ex.printStackTrace();
		}finally{
			this.closeSession();
		}
		
		return appData;
	}
	public void clearDuplicatedRecords(){
		String sql="SELECT app_id FROM "+CrawlerConfig.getInstance().getTbNameReviewers()+" ORDER BY id desc limit 1";
		try{
			ResultSet rs=this.doSelect(sql);
			if(rs.next()){
				String appId=rs.getString("app_id");
				String sql2="SELECT app_id FROM "+CrawlerConfig.getInstance().getTbNameAppData()+" WHERE app_id="+this.makeSqlStr(appId);
				ResultSet rs2=this.doSelect(sql2);
				if(!rs2.next()){
					String delSql="DELETE FROM "+CrawlerConfig.getInstance().getTbNameAppData()+" WHERE app_id="+appId;
					this.doDelete(delSql);
					delSql="DELETE FROM "+CrawlerConfig.getInstance().getTbNameComment()+" WHERE app_id="+appId;
					this.doDelete(delSql);
					delSql="DELETE FROM "+CrawlerConfig.getInstance().getTbNameReviewers()+" WHERE app_id="+appId;
					this.doDelete(delSql);
				}
				rs2.close();
			}
			rs.close();
		}catch(Exception ex){
			log.error(ex.getMessage());
		}finally{
			this.closeSession();
		}
	}
	public void savePopularApps(String starterSeed,String popularApp){
		String sql="INSERT INTO "+CrawlerConfig.getInstance().getTbNamePopularApps()+" (app_id,starter_seed) values("+this.makeSqlStr(popularApp)+","+this.makeSqlStr(starterSeed)+")";
		try{
			this.doInsert(sql);
		}catch(Exception ex){
			log.error(sql);
			log.error(ex.getMessage());
		}finally{
			this.closeSession();
		}
	}
	public ArrayList<String> loadPopularApps(String starterSeed){
		String sql="SELECT app_id FROM "+CrawlerConfig.getInstance().getTbNamePopularApps()+" WHERE starter_seed="+this.makeSqlStr(starterSeed);
		ArrayList<String> popularApps=new ArrayList<String>();
		try{
			ResultSet rs=this.doSelect(sql);
			while(rs.next()){
				popularApps.add(rs.getString("app_id"));
			}
			
		}catch(Exception ex){
			log.error(sql);
			log.error(ex.getMessage());
		}finally{
			this.closeSession();
		}
		return popularApps;
	}

   public ArrayList<String> loadStoreParameters(String appStore,String type){
       String sql="SELECT code,attr1,attr2 FROM StoreParameters WHERE app_store="
               +this.makeSqlStr(appStore)+" AND type="+this.makeSqlStr(type);
       ArrayList<String> ret=new ArrayList<String>();
       try{
           ResultSet rs=this.doSelect(sql);
           while(rs.next()){
               ret.add(rs.getString("code")+"_"+rs.getString("attr1")+"_"+rs.getString("attr2"));
           }

       }catch(Exception ex){
           log.error(sql);
           log.error(ex.getMessage());
       }finally{
           this.closeSession();
       }
       return ret;
   }
}

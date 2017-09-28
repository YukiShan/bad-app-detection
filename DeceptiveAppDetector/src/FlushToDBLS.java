import DAO_DB.FlushToDB;
import data.AppData;
import data.Comment;
import data.Reviewer;
import utility.DataPersistence;

public class FlushToDBLS extends FlushToDB{
    protected FlushToDBLS(){

    }
    public static FlushToDB getInstance(){
        synchronized (FlushToDBLS.class) {
            if (ftdb == null) {
                ftdb = new FlushToDBLS();
            }
            return ftdb;
        }
    }
    /**
     * If special needs for some specific app stores, override this method
     * @param obj
     */
    @Override
    protected void saveToDB(Object obj){
        if(obj instanceof AppData){
            //flush app data to database
            AppData appData=(AppData)obj;
            log.info("Flush "+appData.getAppId()+" to database. ");

            for(Reviewer reviewer:appData.getReviewList()){
                reviewer.setAppId(appData.getAppId());
                getLock();
                try{
                    DataPersistence.getInstance().saveReviewerToDB(reviewer);
                }finally{
                    releaseLock();
                }

            }
            //comment and overview
            for(Comment comment:appData.getCommentList()){
                comment.setAppId(appData.getAppId());
                getLock();
                try{
                    DataPersistence.getInstance().saveCommentToDB(comment);
                }finally{
                    releaseLock();
                }
            }
            getLock();
            try{
                DataPersistence.getInstance().saveAppDataToDB(appData);
            }finally{
                releaseLock();
            }
            log.info("Finish flushing  "+appData.getAppId()+" to database");

        }else if(obj instanceof String){//flush popular apps to DB
            String objStr=(String)obj;
            String []objStrs=objStr.split(",");
            getLock();
            try{
                DataPersistence.getInstance().savePopularAppsToDB(objStrs[0], objStrs[1]);
            }finally{
                releaseLock();
            }
        }
    }
}

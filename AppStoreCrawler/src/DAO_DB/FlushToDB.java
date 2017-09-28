package DAO_DB;

import configuration.CrawlerConfig;
import data.*;
import utility.DataPersistence;
import utility.SystemLogger;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class FlushToDB {
	protected static SystemLogger log = SystemLogger.getLogger(FlushToDB.class);
    //buffered object waiting to be flushed
	final ConcurrentLinkedQueue<Object> buffer=new ConcurrentLinkedQueue<Object>();
    private final Integer bufferCapacity=new Integer(50000);
	private volatile int currentThreadNum =0;
    private volatile int maxThreadNum=0;
	public static ReentrantLock  lock = new ReentrantLock ();
	protected static FlushToDB ftdb=null;
	private boolean stopFlag=false;
    private volatile boolean hasLock=false;
	protected FlushToDB(){
        this.init();
        this.running();
	}
    public static FlushToDB getInstance(){
        synchronized (FlushToDB.class) {
            if (ftdb == null) {
                ftdb = new FlushToDB();
            }
            return ftdb;
        }
	}
	protected void init(){
        if(CrawlerConfig.getInstance().getDbFlusherThreadNum() <= 0){
            this.maxThreadNum =0;
        }else if(DataPersistence.getInstance().getDbMode().toLowerCase().equalsIgnoreCase("sqlite")){
			this.maxThreadNum =1;
		}else if(DataPersistence.getInstance().getDbMode().toLowerCase().equalsIgnoreCase("mysql")){
			this.maxThreadNum =CrawlerConfig.getInstance().getDbFlusherThreadNum();
		}else {
			this.maxThreadNum =1;
		}
        if(DataPersistence.getInstance().getDbMode().equalsIgnoreCase("sqlite")){
            this.hasLock=true;
        }else{
            this.hasLock=false;
        }
//        log.debug("DBFlusher thread size is set to "+this.maxThreadNum);
	}
	/**
	 * Save the obj to a buffer for flushing to DB/file later.
	 * @param obj
	 */
	public void saveObject(Object obj){
		if(obj==null){
			log.error("Cannot save object as it is null.");
			return;
		}
        if(this.currentThreadNum ==0){return;}
        try {
//            log.info(obj.toString());
            this.buffer.add(obj);
            synchronized (this.buffer){
                this.buffer.notify();//notify the consumer
            }
            if (buffer.size() > this.bufferCapacity) {
                log.info("Buffer size exceeds " + this.bufferCapacity);
                synchronized (bufferCapacity){
                    bufferCapacity.wait(); //let the producer thread wait
                }
            }
        }catch (Exception ex){
            log.error(ex);
            ex.printStackTrace();
        }
	}
    public void saveObjects(Collection objects){
        if(objects == null || objects.size()==0){
            return;
        }
        if(this.currentThreadNum ==0){return;}
        try{
            this.buffer.addAll(objects);
            synchronized (this.buffer){
                this.buffer.notifyAll();
            }
            if(this.buffer.size()>this.bufferCapacity){
                log.info("Reach the maximum buffer limit");
                synchronized (this.bufferCapacity){
                    this.bufferCapacity.wait();
                }
            }
        }catch (Exception ex){
            log.error(ex);
        }
    }
    private Object getObject(){
        if(this.buffer==null){
            return null;
        }
        Object obj=this.buffer.poll();

        try {
            if(obj==null) {
                log.debug("Buffer is empty!");
                while ((obj = this.buffer.poll()) == null) {
                    synchronized (this.buffer){
                        this.buffer.wait();
                    }
                }
//                log.info(obj.toString());
            }
            if(this.buffer.size()<this.bufferCapacity/2){
                synchronized (this.bufferCapacity){
                    this.bufferCapacity.notifyAll();
                }
            }
        }catch (Exception ex){
            log.error(ex);
        }
        return obj;
    }
	public boolean isDoneAndWait(){
        try{
            while(buffer.size()!=0){
                Thread.sleep(2000);
            }
        }catch(Exception ex){
            log.error(ex.getMessage());
        }
		return true;
	}
	public void getLock(){
		if(hasLock){
			log.debug("Getting lock for DB file. [sqlite]");
			lock.lock();
		}
	}
	public void releaseLock(){
		if(hasLock){
            if(lock.isLocked()) {
                log.debug("Releasing lock for DB file. [sqlite]");
                lock.unlock();
            }
		}
	}
	protected void running(){
		for(int i=0;i<this.maxThreadNum;i++){
            currentThreadNum++;
            this.startOneThread(i);
		}
        Thread deamonThread=new Thread(new Runnable(){
           public void run(){
               //check the thread num
               while(!stopFlag){
                   init();
                   while (currentThreadNum < maxThreadNum) {
                       log.info("New FlushToDB thread created.");
                       startOneThread(currentThreadNum++);
                   }
               }
           }
        });
        deamonThread.setName("FlushToDB");
        deamonThread.start();
	}
    private void startOneThread(final int token){
        Thread thread=new Thread(new Runnable(){
            public void run(){
                int tokenTemp=token;
                while(!stopFlag && tokenTemp<maxThreadNum){
                    try{
                        Object obj=getObject();
                        saveToDB(obj);
                    }catch(Exception ex){
                        log.error(ex);
                    }
                }
                log.info("Thread exit!");
                currentThreadNum--;
            }
        });
        thread.setName("FTDB_"+token);
        thread.start();
    }

    /**
     * If special needs for some specific app stores, override this method
     * @param obj
     */
    protected void saveToDB(Object obj){
        if(obj instanceof AppData){
            //flush app data to database
            AppData appData=(AppData)obj;
            log.info("Flush app "+appData.getAppId()+" to database. ");
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

        }else if(obj instanceof Reviewer){
            getLock();
            try{
                Reviewer reviewer=(Reviewer)obj;
                DataPersistence.getInstance().saveReviewerToDB(reviewer);
            }finally {
                releaseLock();
            }
        }else if(obj instanceof AppRatingInfo){
            getLock();
            try{
                AppRatingInfo appRatingInfo=(AppRatingInfo)obj;
                DataPersistence.getInstance().saveRatingsToDB(appRatingInfo);
            }finally {
                releaseLock();
            }
        }else if(obj instanceof AppId){
            getLock();
            try{
                AppId appId=(AppId)obj;
                DataPersistence.getInstance().saveAppIdToDB(appId);
            }finally {
                releaseLock();
            }
        }else if(obj instanceof Developer){
            getLock();
            try{
                Developer developer=(Developer)obj;
                log.info("Flush developer "+developer.getDeveloperId());
                DataPersistence.getInstance().saveDeveloperToDB(developer);
            }finally {
                releaseLock();
            }
        }
    }

    /**
     * Explicit exit all the threads
     */
	public void exit(){
		stopFlag=true;
		buffer.clear();
		this.ftdb=null;
	}
}

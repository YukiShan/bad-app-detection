package DAO_DB;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Properties;
import java.util.logging.Logger;


public class DBMonitor extends Thread {

    private static Logger log = Logger.getLogger(DBMonitor.class.getName());
    private static volatile boolean running = false;
    private DataControl dataControl = null;
    private static DBMonitor instance = new DBMonitor();
    private static volatile boolean initialed = false;
    private RandomAccessFile fi = null;
    private FileChannel fc = null;
    private String fileName = null;
    private long interval = 1000L;

    private DBMonitor(){}

    private void init(){
        dataControl = DataControl.getInstance();
        Properties prop = dataControl.getProp();
        fileName = prop.getProperty("DataControl_MonitorFilePath");//System.getProperty("gmd_home") + "/ha/";
        try{
            File file = new File(fileName);
            if(!file.exists()){
                file.mkdir();
            }
            fileName = fileName + "/" + "DBMonitor";
            file = new File(fileName);
            if(!file.exists()){
                file.createNewFile();
            }
            interval = Long.parseLong(prop.getProperty("DataControl_MonitorInterval", "1000"));
            fi = new RandomAccessFile(file, "rw");
            fc = fi.getChannel();
        }catch(Exception e){
            log.warning("Get exception when to initial monitor file:"+e);
            System.exit(0);
        }
        initialed = true;
    }

    public static DBMonitor getInstance() {
        if (!instance.initialed) {
            synchronized (instance) {
                if (!instance.initialed) {
                    instance.init();
                }
            }
        }
        return instance;
    }

    public void run(){
        try{
            while (running) {
                try{
                    Thread.currentThread().sleep(interval);
                    if(!dataControl.isHandover()&& readFile()){
                        log.warning("DBMonitor in DataControl find DB is handover!");
                        dataControl.setHandover(true);
                    }
                }catch(Exception e){
                    log.warning("Get Exception when to check DB monitor file:"+e);
                }
            }
        }finally{
            if(fc != null){
                try{
                    fc.close();
                }catch(Exception e){
                    log.warning("Get Exception when to close file channel:"+e);
                }
            }
            if(fi != null){
                try {
                    fi.close();
                } catch (Exception e) {
                    log.warning("Get Exception when to close RandomAccessFile:" + e);
                }
            }
            stopMonitor();
        }
    }

    public synchronized void startMonitor(){
        running = true;
        Thread thread = new Thread(this);
        thread.start();
    }

    public synchronized void stopMonitor(){
        running = false;
    }

    public boolean readFile(){
        String flag = null;
        synchronized(fi){
            try{
                byte[] temp = new byte[(int) fi.length()];
                fi.read(temp);
                fi.seek(0);
                if(temp == null || temp.length == 0){
                    return false;
                }
                else{
                    flag = new String(temp);
                }
            }catch(Exception e){
                log.warning("Get Exception when to read DB monitor file:"+e);
                e.printStackTrace();
            }
        }
        if(flag.startsWith("Y")){
            return true;
        }
        return false;
    }

    public void writeFile(){
        FileLock fileLock = null;
        try{
            synchronized(fi){
                fileLock = fc.tryLock();
                while (fileLock == null || !fileLock.isValid()) {
                    fileLock = fc.tryLock();
                    Thread.currentThread().sleep(10L);
                }
                fi.write("Y".getBytes());
                fi.seek(0);
            }
        }catch(Exception e){
            log.warning("Get Exception when to write file:"+e);
        }finally{
            if(fileLock != null){
                try{
                    fileLock.release();
                }catch(Exception e){
                    log.warning("Get Exception when to release file lock:"+e);
                }
            }
        }
    }
}

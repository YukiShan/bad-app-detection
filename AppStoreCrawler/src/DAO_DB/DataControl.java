package DAO_DB;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class DataControl {
    private static Logger log = Logger.getLogger(DataControl.class.getName());
    private static boolean initialized = false;
    private static ConcurrentHashMap dsConfigs;
    private static Properties mprop;
    private static ConcurrentHashMap dataManagers = new ConcurrentHashMap();
    private static DataControl instance = new DataControl();
    private static DBMonitor dbMonitor;
    private static volatile boolean handover = false;
    private static boolean canHandover = false;
    private static int failureLimit = 5;

    private DataControl() {}

    public static DataControl getInstance() {
        return instance;
    }

    /** Initiate DataControl.
     * @param configFile
     * @throws
     */
    public static void init(String configFile) throws DataControlException {
        Properties prop;
        if (!initialized) {
            try {
                prop = new Properties();
                prop.load(new FileInputStream(configFile));
            } catch (Exception e) {
                throw new DataControlException(
                        "Fail to initialize DataControl. Error reading property file.",
                        e);
            }
            init(prop);
        }
    }

    /** Initiate DataControl.
     * @param prop
     * @throws DataControlException
     */
    public static void init(Properties prop) throws DataControlException {
        if (!initialized) {
            mprop = prop;

            canHandover = Boolean.parseBoolean(prop.getProperty("DataControl_HandOver", "false"));
            if(canHandover){
                try {
                    failureLimit = Integer.parseInt(prop.getProperty(
                            "DataControl_FailureLimit", "10"));
                } catch (Exception e) {
                    throw new DataControlException(e);
                }
                DBMonitor dbMonitor = DBMonitor.getInstance();
                handover = dbMonitor.readFile();
                dbMonitor.startMonitor();
            }

            try {
                dsConfigs = readDsNames(prop);  //Database names
            } catch (ConfigurationException ex) {
                throw new DataControlException(ex);
            }
            createSessionFactories(dsConfigs);
            initialized = true;
        }
    }

   
    private static void createSessionFactories(ConcurrentHashMap<String,Object> dsConfigs) throws
            DataControlException {
        DbObject dsConfig;
        DataManager[] dms;
        SessionFactory[] factories = null;
        for (String dsName:dsConfigs.keySet()) {
            if(dsName.startsWith("backup")){
                continue;
            }
            factories = new SessionFactory[2];
            dsConfig = (DbObject) dsConfigs.get(dsName);
            dms = dsConfig.getDataManagers();
            if (dms == null) {
                throw new DataControlException(
                        "No DataManager is configured to be associated with Data Source: " +
                        dsName);
            }

            if (!handover) {
                factories[0] = getSessionFactory(dsConfig);
            }
            if (canHandover) {
                dsConfig = (DbObject) dsConfigs.get("backup" + dsName);
                if (dsConfig != null) {
                    factories[1] = getSessionFactory(dsConfig);
                } else {
                    factories[1] = factories[0];
                }
            }

            for (int i = 0; i < dms.length; i++) {
                // associate datamanager with the corresponding session factory
                dms[i].setSessionFactory(factories);
                dms[i].setFailureLimit(failureLimit);
                // put data manager in a hashtable for relater retreval
                dataManagers.put(dms[i].getClass().getName(), dms[i]);
            }
        }
    }
    private static SessionFactory getSessionFactory(String dsConfig)throws
    DataControlException {
    	Configuration config=new Configuration();
    	return config.configure(dsConfig).buildSessionFactory();
    	
    }
    public static SessionFactory getSessionFactory(DbObject dsConfig)throws
            DataControlException {
        Configuration config = (new Configuration())
                 .setProperty("hibernate.connection.driver_class",
                              dsConfig.getDriver())
                 .setProperty("hibernate.connection.url",
                              dsConfig.getUrl())
                 .setProperty("hibernate.connection.username",
                              dsConfig.getUsername())
                 .setProperty("hibernate.connection.password",
                              dsConfig.getPassword())
                 .setProperty("hibernate.c3p0.min_siz",
                              String.valueOf(dsConfig.getMinPoolSize()))
                 .setProperty("hibernate.c3p0.max_size",
                              String.valueOf(dsConfig.getMaxPoolSize()))
                 .setProperty("hibernate.c3p0.timeout",
                              String.valueOf(dsConfig.getTimeout()))
                 .setProperty("hibernate.c3p0.max_statements",
                              String.valueOf(dsConfig.getMaxStatement()))
                 .setProperty("hibernate.c3p0.idle_test_period",
                              String.valueOf(3000))
                 .setProperty("hibernate.connection.autocommit",
                              "false")
                 .setProperty("hibernate.dialect",
                              dsConfig.getHibernateDialect());
        return  config.buildSessionFactory();
    }

    public static DataManager getDataManager(String dmName) throws DataControlException{

        DataManager dm =  (DataManager) dataManagers.get(dmName);
        if (dm == null)
            throw new DataControlException("Fail to get " + dmName + " from DataControl.");

        // Call the init method of dmName class
        try {
            dm.initDataManager();
        } catch (Exception ex) {
            throw new DataControlException("Throw exceptions when called the initDataManager() method in " +
                dmName + "class: " + ex.getMessage());
        }
        return dm;
    }
    private static ConcurrentHashMap readDsNames(Properties prop) throws
            ConfigurationException {
        dsConfigs = new ConcurrentHashMap<String,Object>();
        String strDsNames = prop.getProperty("DataControl_DS_Names", "db1");
        if (strDsNames == null) {
            throw new ConfigurationException(
                    "No data source configured to be connected.");
        } else {
            strDsNames = strDsNames.trim();
        }
        StringTokenizer dbNames = new StringTokenizer(strDsNames, ",");
        String dsName = null;
        String prefix = null;
        while (dbNames.hasMoreTokens()) {
            dsName = dbNames.nextToken().trim();
            // Let DSConfig further parse the properties file
            dsConfigs.put(dsName, new DbObject(prop, dsName));
            prefix = "backup" + dsName;
            if (canHandover && prop.getProperty("DS_backup" + dsName + "_Driver") != null) {
                dsConfigs.put(prefix,new DbObject(prop, prefix, true));
            }
        }
        return dsConfigs;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public boolean isHandover() {
        return handover;
    }

    public static Properties getProp() {
        return mprop;
    }

    public synchronized void setHandover(boolean handoverFlag) {
        log.warning("DataControl set DB status to handover");
        if(!handover){
            handover = handoverFlag;
            Iterator iter = dataManagers.values().iterator();
            while (iter.hasNext()) {
                DataManager dm = (DataManager) (iter.next());
                dm.closePrimaryFactory();
            }
        }
    }

    public boolean getCanHandover(){
        return canHandover;
    }
}

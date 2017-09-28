package DAO_DB;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;


public abstract class DataManager {
    protected SessionFactory[] factories = null;
    private ThreadLocal session = new ThreadLocal();
    protected static Logger logger = LogManager.getLogManager().getLogger("");
    private static volatile int failureCount = 0;
    private static volatile int failureLimit = 5;
    private Object mutex = new Object();
    private DataControl dataControl = DataControl.getInstance();
    protected Statement stmt;

    /**
     * Its sub-class must have one nullary constructor.
     */
    public DataManager() {
    }

    public void initDataManager() throws DataControlException,
        DataManagerException{
    }

    /**
     * Legacy support
     * This constructor does nothing
     * @param dbo DbObject
     */
    public DataManager(DbObject dbo) {
        // Do nothing
    }

    /**************************************************************************
     *                       Hibernate supporting functions                   *
     **************************************************************************/

    /**
     * To be implement for subclasses: to return associated entity class <br>
     * For the consideration of easy migration, a default implementation is
     * provided. However, for any data manager that is using Hibernate
     * infrastructure should override this function to return corresponding
     * class.
     */
    public Class getAssociatedClass() {
        return null;
    }

    /**
     * Assign the corresponding SessionFactory
     * Can only be accessed by com.aicent.db package
     * @param factory SessionFactory
     */
    void setSessionFactory(SessionFactory[] factory) {
        this.factories = factory;
    }

    /**
     * Get an available session, creat a new one if none is available
     * @return Session
     */
    public Session currentSession() {
        Session s = (Session) session.get();
        // Open a new Session, if this Thread has none yet
        if (s == null) {
            if (dataControl.getCanHandover() && dataControl.isHandover()) {
                s = factories[1].openSession();
            } else {
                s = factories[0].openSession();
            }
            s.setCacheMode(CacheMode.NORMAL);
            session.set(s);
        }
        if (s.isDirty()) {
            s.flush();
        }

        return s;
    }

    /**
     * Close current session
     */
    public void closeSession() {
        try {
            Session s = (Session) session.get();
            session.set(null);
            if (s != null) {
                s.close();
            }
        } catch (HibernateException ex) {
            exceptionHandler(ex);
            ex.printStackTrace();
        }
    }


    /**************************************************************************
     *                       Legacy Supporting Functions                      *
     **************************************************************************/

    /** Excutes an Insert statement
     * @param strSQL An SQL Insert Statement as a String
     * @throws DataControlException
     * @return Returns a row count of the Insert statement
     */
    public int doInsert(String strSQL) throws DataControlException {
        return doUpdate(strSQL);
    }

    /** Excutes an Update statement
     * @param strSQL
     * @throws DataControlException
     * @return
     */
    protected int doUpdate(String strSQL) throws DataControlException {
        Statement stmt = null;
        try {
            Session sess = currentSession();
            Connection conn = sess.connection();
            stmt = conn.createStatement();
            int line = stmt.executeUpdate(strSQL);
            conn.commit();
            zeroFailureCount();
            return line;
        } catch (Exception e) {
            logger.finest("SQL Statement that cause error: " + strSQL);
            //exceptionHandler(e);
            e.printStackTrace();
            throw new DataControlException("Database connection error: " +
                                           e.getMessage());
            
        }
    }

    /** Excutes an Delete statement
     * @param strSQL An SQL Insert Statement as a String
     * @throws DataControlException
     * @return Returns a row count of the Insert statement
     */
    public int doDelete(String strSQL) throws DataControlException {
        return doUpdate(strSQL);
    }


    protected Statement getStmt() throws DataControlException {
        try {
            Session sess = currentSession();
            Connection conn = sess.connection();
            stmt = conn.createStatement();
            zeroFailureCount();
        } catch (Exception e) {
            exceptionHandler(e);
            throw new DataControlException("Database query error: " +
                                           e.getMessage());
        } finally {
            return stmt;
        }
    }

    protected Statement getBatchUpdateStmt() throws DataControlException {
        return getStmt();
    }

    protected boolean isSupportBatchUpdate() throws Exception {
        boolean br = false;
        try {
            Session sess = currentSession();
            Connection conn = sess.connection();
            DatabaseMetaData dmd = conn.getMetaData();
            br = dmd.supportsBatchUpdates();
        } catch (Exception e) {
            exceptionHandler(e);
            throw new Exception(e);
        } finally {
            closeSession();
            return br;
        }
    }

    protected int[] doBatchUpdate() throws DataControlException {
        try {
            return stmt.executeBatch();
        } catch (SQLException e) {
            exceptionHandler(e);
            throw new DataControlException("Database query error: " +
                                           e.getMessage());
        }
    }

    protected int[] batchUpdate() throws DataControlException {
        try {
            return stmt.executeBatch();
        } catch (SQLException e) {
            exceptionHandler(e);
            throw new DataControlException("Database query error: " +
                                           e.getMessage());
        }
    }


    /** Executes a Select statement
     * @param strSQL a SQL Select Statement as a String
     * @throws DataControlException
     * @return
     */
    public ResultSet doSelect(String strSQL) throws DataControlException {
        Statement stmt = null;
        ResultSet rs = null;

        try {
            Session sess = currentSession();
            Connection conn = sess.connection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(strSQL);
//            conn.close();
            zeroFailureCount();
        } catch (Exception e) {
            logger.finest("SQL Statement that cause error: " + strSQL);
            exceptionHandler(e);
            throw new DataControlException("Database query error: " +
                                           e.getMessage());
        }
        return rs;
    }

    /** Parses a String as a SQL variable
     * @param strSQL A SQL string to be parsed
     * @return
     */
    public String makeSqlStr(String strSQL) {
        if (strSQL == null) {
            return "NULL";
        }
        strSQL = strSQL.replaceAll("\\\\", "");
//        strSQL = strSQL.replaceAll("'", "\\\\'"); //for mysql
        strSQL = strSQL.replaceAll("'", "''");
        return "'" + strSQL + "'"; 

    }
    protected String makeSqlStr(float f){
    	return Float.toString(f);
    }
    protected String makeSqlStr(int i) {
        return Integer.toString(i);
    }

    protected String makeSqlStr(long l) {
        return Long.toString(l);
    }

    protected String makeSqlStr(short s) {
        return Integer.toString(s);
    }

    protected String makeSqlStr(java.util.Date d) {
        return formatMySqlDate(d);
    }

    protected String formatMySqlDate(java.util.Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return "'" + formatter.format(date) + "'";
    }

    /** Calculates the row count of a specific ResultSet
     * @param rs A ResultSet
     * @return
     */
    public int getRowCount(ResultSet rs) {
        int rowCount, crrRow;
        try {
            crrRow = rs.getRow();
            rs.last();
            rowCount = rs.getRow();
            if (crrRow == 0) {
                rs.beforeFirst();
            } else {
                rs.absolute(crrRow);
            }
            zeroFailureCount();
        } catch (Exception e) {
            exceptionHandler(e);
            rowCount = 0;
        }
        return rowCount;
    }

    /**
     * Creates a new table for the given month. If table already exist,
     * then rename the old table
     * @param date Calendar
     * @throws DataManagerException
     * @return String Created Table Name
     */
    public String createMonthlyTable(Calendar date, String prefix) throws
            DataManagerException {
        String tableName = null;
        SimpleDateFormat bakTableSurfix = new SimpleDateFormat(
                "'_bk'_yyMMdd_kkmmssSSS");

        tableName = getMonthlyTableName(date, prefix);

        try {
            if (doesTableExist(tableName)) {
                java.util.Date currentTime = new java.util.Date();
                // FOR DEBBUG ONLY
                if ("true".equalsIgnoreCase((String) System.getProperty("DEBUG"))) {
                    doUpdate("DROP TABLE " + tableName);
                } else {
                    doUpdate("ALTER TABLE " + tableName + " RENAME TO " +
                             tableName + bakTableSurfix.format(currentTime));
                }
            }
            doUpdate("CREATE TABLE " + tableName + " (" + getTableDef() + ")");
            zeroFailureCount();
            return tableName;
        } catch (Exception e) {
            exceptionHandler(e);
            throw new DataManagerException(e);
        }

    }

    /**
     * getTableDef is to be override by implementing classes
     * @return String
     */
    protected String getTableDef() {
        return null;
    }

    /**
     *
     * @param date Calendar
     * @param prefix String
     * @return String
     */
    protected static String getMonthlyTableName(Calendar date, String prefix) {
        SimpleDateFormat tableFormat = new SimpleDateFormat("'" + prefix +
                "'_MM_yyyy");
        return tableFormat.format(date.getTime());
    }

    /**
     *
     * @param tableName String
     * @return boolean
     */
    public boolean doesTableExist(String tableName) {
        String sqlStr = "SHOW TABLES LIKE '" + tableName + "'";
        ResultSet rs = null;
        try {
            rs = doSelect(sqlStr);
            zeroFailureCount();
            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            exceptionHandler(e);
            return false;
        }
        finally{
            if(rs != null)
            {
                try{
                    rs.close();
                    rs = null;
                }catch(SQLException e)
                {
                    logger.warning("SQL Resultset close cause error! ");
                }
            }
        }
    }


    /** returns a database connection
     * @throws DataManagerException
     * @return returns a database connection
     */
    protected Connection getCon() throws DataManagerException {
        try {
            Session sess = currentSession();
            Connection connection = sess.connection();
            zeroFailureCount();
            return connection;
        } catch (Exception ex) {
            exceptionHandler(ex);
            throw new DataManagerException(ex.getMessage());
        }
    }

    public String isNullStmt(String columnName) {
        if (columnName == null)
            return "''";
        String stmt = " (" + columnName + " is null or " + columnName + "='') ";
        return stmt;
    }

    /** Abstract method add()
     * adds a Data object into the database
     * @param data
     * @throws
     */

    public void exceptionHandler(Exception e) {
        String message = e.getMessage();
        System.out.println("exceptionHandler: " + message + ". failureCount=" + failureCount);
        if (message.contains("Communications link failure") ||
            message.contains("Connection reset") ||
            message.contains("connection abort") ||
            message.contains("Cannot open connection") ||
            message.contains("Connection refused") ||
            message.contains("Software caused connection abort: recv failed")) {
            synchronized (mutex) {
                failureCount++;
            }
        }
        if (failureCount >= failureLimit) {
            synchronized (mutex) {
                if (dataControl.getCanHandover() && !dataControl.isHandover()) {
                    dataControl.setHandover(true);
                    DBMonitor.getInstance().writeFile();
                    //DBMonitor.getInstance().sendHandoverFlagFile();
                    failureCount = 0;
                } else {
                    failureCount = 0;
                }
            }
        }
    }

    public int getFailureLimit() {
            return failureLimit;
    }

    private void zeroFailureCount() {
        synchronized (mutex) {
            failureCount = 0;
        }
    }

    public void setFailureLimit(int failureLimit) {
        this.failureLimit = failureLimit;
    }

    public void closePrimaryFactory() {
        try{
            if(factories[0] != null && !factories[0].isClosed()){
                factories[0].close();
            }
        } catch (HibernateException e) {
            logger.warning("close primary factory error! ");
        }
    }
}
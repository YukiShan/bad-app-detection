package DAO_DB;
public class DataManagerException extends java.lang.Exception {

    /**
     * Creates a new instance of <code>DataControlException</code> without detail message.
     */
    public DataManagerException() {
    }


    /**
     * Constructs an instance of <code>DataControlException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public DataManagerException(String msg) {
        super(msg);
    }

    public DataManagerException(Throwable cause) {
        super(cause);
    }

    public DataManagerException(String msg, Throwable cause){
        super(msg, cause);
    }

}

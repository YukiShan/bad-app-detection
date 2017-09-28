package DAO_DB;
public class DataControlException extends java.lang.Exception {

    /**
     * Creates a new instance of <code>DataControlException</code> without detail message.
     */
    public DataControlException() {
    }

    public DataControlException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an instance of <code>DataControlException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public DataControlException(String msg) {
        super(msg);
    }


    public DataControlException(String message, Throwable cause) {
        super(message, cause);
    }

}

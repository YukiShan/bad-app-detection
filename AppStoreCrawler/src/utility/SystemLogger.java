package utility;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;



public class SystemLogger extends Logger {
    static String FQCN = SystemLogger.class.getName();
    private static SystemLoggerFactory systemLoggerFactory = new SystemLoggerFactory();
    private int size = 1024;

    protected SystemLogger(String name) {
        super(name);
    }

    public static SystemLogger getLogger(String name){
        return (SystemLogger)Logger.getLogger(name,systemLoggerFactory);
    }

    public static SystemLogger getLogger(Class clazz){
        return (SystemLogger)SystemLogger.getLogger(clazz.getName());
    }
    public void debug(Object message) {
        super.log(FQCN,Level.DEBUG,message,null);
    }
    public void debug(Object message, Throwable t) {
        super.log(FQCN,Level.DEBUG,message,t);
    }

    public void info(Object message) {
        super.log(FQCN,Level.INFO,message,null);
    }

    public void info(Object message, Throwable t) {
        super.log(FQCN,Level.INFO,message,t);
    }

    public void warn(Object message) {
        super.log(FQCN,Level.toLevel(Level.WARN_INT),message,null);
    }

    public void warn(Object message, Throwable t) {
        super.log(FQCN,Level.toLevel(Level.WARN_INT),message,t);
    }

    public void error(Object message) {
        super.log(FQCN,Level.ERROR,message,null);
    }

    public void error(Object message, Throwable t) {
        super.log(FQCN,Level.ERROR,message,t);
    }

    public void fatal(Object message) {
        super.log(FQCN,Level.FATAL,message,null);
    }

    public void fatal(Object message, Throwable t) {
        super.log(FQCN,Level.FATAL,message,t);
    }
}

    
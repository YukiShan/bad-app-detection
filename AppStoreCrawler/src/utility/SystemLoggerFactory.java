package utility;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;

public class SystemLoggerFactory implements LoggerFactory {
    public SystemLoggerFactory() {
    }

    /**
     * makeNewLoggerInstance
     *
     * @param string String
     * @return Logger
     * @todo Implement this org.apache.log4j.spi.LoggerFactory method
     */
    public Logger makeNewLoggerInstance(String name) {
        return new SystemLogger(name);
    }

    public static void main(String[] args) {
    }
}
package utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import configuration.BaseConfig;
import configuration.CrawlerConfig;

public class MultiDateFormat extends SimpleDateFormat{
    private static SystemLogger log = SystemLogger.getLogger(MultiDateFormat.class);
    String formatStr="yyyy-MM-dd HH:mm:ss";
    static HashSet<String> formatStrings=new HashSet<String>();
	
	public MultiDateFormat(){
		
	}
	public MultiDateFormat(String dateString){
		formatStrings.addAll(DataPersistence.getInstance().getBaseConfig().getDateFormates());
		if(!formatStrings.contains(dateString)){
			formatStrings.add(dateString);
		}
        this.formatStr=dateString;
	}
	
	public Date parse(String dateString) throws ParseException {
		for (String formatString : formatStrings){
	        try{
	            return new SimpleDateFormat(formatString).parse(dateString);
	        }
	        catch (ParseException e) {
//	        	log.error(e);
	        }
	    }
		throw new ParseException("Unparseable date: \"" + dateString + "\"",1 );
	}
	public String selfFormat(Date date){
		SimpleDateFormat sdf=new SimpleDateFormat(formatStr);
		return sdf.format(date);
	}
}

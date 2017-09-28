/**
 * 
 */
package crawlerParser;

import java.util.concurrent.ConcurrentLinkedQueue;

import data.*;


/**
 * @author xiezhen
 * This is an interface for parsing content of the web sites.
 * All the parsers should implement this interface.
 */
public interface ContentParser {
	/**
	 * Parse the id from the app list page
	 * @param idList: ConcurrentLinkedQueue for storing id
	 * @return true-if there is next page false-otherwise
	 */
	int idParser(ConcurrentLinkedQueue<AppId> idList,StringBuilder idPage,String ...para);
	
    /**
     * Parse the overview information of an App from the content
     * @param overviewPage: the page content to be parsed
     * @param appData: the object of the app whose overview to be parsed
     * @return true - if successfully parse the page, false- failed to parse the page
     */
	boolean overviewParser(StringBuilder overviewPage,AppData appData);
	
	/**
	 * Parse the Comments from the comments page and store them to a queue
	 * @param commentsPage: the content of the web page
     * @param currentPageNum: the current comment page
	 * @param appData: the appData object of the application object
	 * @return  true - if there is next page false  false - it is the last page
	 */
	boolean CommentsParser(StringBuilder commentsPage,
			int currentPageNum,AppData appData);
	
	/**
	 * Parse the rank of each App from the rank page
	 * @param ariList
	 * @param rankingPage
     * @param param
	 * @return true-if there is next page false-otherwise
	 */
	boolean RankParser(ConcurrentLinkedQueue<AppRatingInfo> ariList,StringBuilder rankingPage,String ...param);
	/**
	 * Parse user profile
     * @param reviewer: The user object who profile to be parsed
	 * @param userProfileStr: The content of page to be parsed
     * @return true - successfully parsed the content false - failed to parse the content
	 */
	boolean userProfileParser(Reviewer reviewer,StringBuilder userProfileStr);

    /**
     * Parse developer profile
     * @param developer: The developer object whose profile to be parsed
     * @param developerProfileStr: The page content to be parsed
     * @return true - successfully parsed the content false - failed to parse the content
     */
    boolean developerProfileParser(Developer developer,StringBuilder developerProfileStr);
}

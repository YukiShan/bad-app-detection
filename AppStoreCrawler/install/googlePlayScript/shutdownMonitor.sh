#!/bin/bash

#get the process id 
crawler_name="GooglePlayCrawler";
ui=$(ps -ef|grep -i $crawler_name|grep -v grep|awk '{print $2}')

#kill the process if exist
if [ "$ui" == "" ]; then
 echo "$crawler_name monitor is not running."

else 
  echo " $crawler_name monitor is being stopped."
	kill -9 $ui
fi


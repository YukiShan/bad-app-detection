#!/bin/bash

#get the process id 
crawler_name="DeceptiveAppDetector";

ui=$(ps -ef|grep -i $crawler_name|grep -v grep|awk '{print $2}')

#start the monitor if not exist.
if [ "$ui" == "" ]; then
 echo "start $crawler_name monitor"
 java -jar $crawler_name".jar" &  1>& /dev/null

else 
  echo "$crawler_name monitor is running!"
fi


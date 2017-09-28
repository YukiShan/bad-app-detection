#!/bin/bash


#mysql -uroot < sql ;

sqlite_db_path=$(grep "^DS_db1_URL=jdbc:sqlite:" ../../database.properties |awk -F":" '{print $3}')
echo $sqlite_db_path
sqlite_db_path=$(echo "$sqlite_db_path"|tr -d '\r')
rm "$sqlite_db_path"
sqlite3 "$sqlite_db_path" < sqlite.sql
python load_itunes.py "$sqlite_db_path"
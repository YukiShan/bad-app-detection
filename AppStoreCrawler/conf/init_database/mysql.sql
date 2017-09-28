# Database: crawler
# Table: 'AppData'
# 

####### create database #########

DROP DATABASE Crawler_apple_pub;
CREATE DATABASE Crawler_apple_pub;
USE Crawler_apple_pub;
#DROP DATABASE Crawler_googlepaly;
#CREATE DATABASE Crawler_googleplay;
#USE Crawler_googleplay;

####### create tables #########


CREATE TABLE `AppData` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app_id` varchar(100) DEFAULT '',
  `app_name` mediumblob,
  `Developer_id` varchar(100) DEFAULT '',
  `Developer_name` text,
  `Developer_company` mediumblob,
  `average_rating` float DEFAULT '0',
  `total_raters` int(11) DEFAULT '0',
  `Rated_summary` text,
  `1star_num` int(11) DEFAULT '0',
  `2star_num` int(11) DEFAULT '0',
  `3star_num` int(11) DEFAULT '0',
  `4star_num` int(11) DEFAULT '0',
  `5star_num` bigint(11) DEFAULT '0',
  `current_version` varchar(50) DEFAULT '',
  `required_android_version` varchar(100) DEFAULT '',
  `category` varchar(50) DEFAULT '',
  `sub_category` varchar(50) DEFAULT '',
  `price` text,
  `currency` varchar(10) DEFAULT '',
  `thumbnail` varchar(200) DEFAULT '',
  `store_location` varchar(100) DEFAULT '',
  `crawler_point` varchar(100) DEFAULT '',
  `Description` mediumblob,
  `Latest_modified` datetime DEFAULT '0000-00-00 00:00:00',
  `Latest_action` text,
  `Installs` varchar(50) DEFAULT '',
  `Content_rated` text,
  `Language` text,
  `Requirements` mediumblob,
  `url` mediumblob,
  `size` varchar(20) DEFAULT '',
  `Permission` text,
  `Contact_phone` text,
  `Contact_website` text,
  `Contact_email` text,
  `Promoted_text` text,
  `Promoted_screenshot` text,
  `Promoted_video` text,
  `Recent_changes` mediumblob,
  `latest_reveiw_id` text,
  `rho` float DEFAULT '0',
  `adjusted_rating` float DEFAULT '0',
  `average_weekly` float DEFAULT '0',
  `weeks` int(11) DEFAULT '0',
  `rank` int(11) DEFAULT '0',
  `Note` text,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

# Database: crawler
# Table: 'AppRatings'
# 
#CREATE TABLE `AppRatings_m1` (
#  `id` int(11) NOT NULL AUTO_INCREMENT,
#  `app_id` varchar(100) NOT NULL DEFAULT '',
#  `title` mediumblob,
#  `update_time` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
#  `category` int(11) DEFAULT '0',
#  `store_location` int(11) DEFAULT '0',
#  `crawler_point` int(11) DEFAULT '0',
#  `day1` varchar(256) NOT NULL DEFAULT '',
#  `day2` varchar(256) NOT NULL DEFAULT '',
#  `day3` varchar(256) NOT NULL DEFAULT '',
#  `day4` varchar(256) NOT NULL DEFAULT '',
#  `day5` varchar(256) NOT NULL DEFAULT '',
#  `day6` varchar(256) NOT NULL DEFAULT '',
#  `day7` varchar(256) NOT NULL DEFAULT '',
#  `day8` varchar(256) NOT NULL DEFAULT '',
#  `day9` varchar(256) NOT NULL DEFAULT '',
#  `day10` varchar(256) NOT NULL DEFAULT '',
#  `day11` varchar(256) NOT NULL DEFAULT '',
#  `day12` varchar(256) NOT NULL DEFAULT '',
#  `day13` varchar(256) NOT NULL DEFAULT '',
#  `day14` varchar(256) NOT NULL DEFAULT '',
#  `day15` varchar(256) NOT NULL DEFAULT '',
#  `day16` varchar(256) NOT NULL DEFAULT '',
#  `day17` varchar(256) NOT NULL DEFAULT '',
#  `day18` varchar(256) NOT NULL DEFAULT '',
#  `day19` varchar(256) NOT NULL DEFAULT '',
#  `day20` varchar(256) NOT NULL DEFAULT '',
#  `day21` varchar(256) NOT NULL DEFAULT '',
#  `day22` varchar(256) NOT NULL DEFAULT '',
#  `day23` varchar(256) NOT NULL DEFAULT '',
#  `day24` varchar(256) NOT NULL DEFAULT '',
#  `day25` varchar(256) NOT NULL DEFAULT '',
#  `day26` varchar(256) NOT NULL DEFAULT '',
#  `day27` varchar(256) NOT NULL DEFAULT '',
#  `day28` varchar(256) NOT NULL DEFAULT '',
#  `day29` varchar(256) NOT NULL DEFAULT '',
#  `day30` varchar(256) NOT NULL DEFAULT '',
#  `day31` varchar(256) NOT NULL DEFAULT '',
#  PRIMARY KEY (`id`)
#) ENGINE=MyISAM DEFAULT CHARSET=utf8;
#CREATE TABLE AppRatings_m2 LIKE AppRatings_m1;
#CREATE TABLE AppRatings_m3 LIKE AppRatings_m1;
#CREATE TABLE AppRatings_m4 LIKE AppRatings_m1;
#CREATE TABLE AppRatings_m5 LIKE AppRatings_m1;
#CREATE TABLE AppRatings_m6 LIKE AppRatings_m1;
#CREATE TABLE AppRatings_m7 LIKE AppRatings_m1;
#CREATE TABLE AppRatings_m8 LIKE AppRatings_m1;
#CREATE TABLE AppRatings_m9 LIKE AppRatings_m1;
#CREATE TABLE AppRatings_m10 LIKE AppRatings_m1;
#CREATE TABLE AppRatings_m11 LIKE AppRatings_m1;
#CREATE TABLE AppRatings_m12 LIKE AppRatings_m1;

# Database: crawler
# Table: 'Comment'
# 
CREATE TABLE `Comment` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `appdata_id` int(11) DEFAULT '0',
  `reviewer` mediumblob,
  `reviewer_id` varchar(100) DEFAULT '',
  `date` datetime DEFAULT '0000-00-00 00:00:00',
  `device` varchar(100) DEFAULT '',
  `device_version` mediumblob,
  `rating` int(11) DEFAULT '0',
  `comment` mediumblob,
  `comment_title` mediumblob,
  `helpfulness_ratio` float DEFAULT '0',
  `helpfulness_agree` int(11) DEFAULT '0',
  `helpfulness_total` int(11) DEFAULT '0',
  `review_id` text,
  `app_id` varchar(200) DEFAULT '',
  `contribution` float DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

# Database: crawler
# Table: 'Reviewers'
# 
CREATE TABLE `Reviewers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `reviewer_id` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `app_ids_ordered` text COLLATE utf8_unicode_ci,
  `review_ratings` text COLLATE utf8_unicode_ci,
  `review_dates` text COLLATE utf8_unicode_ci,
  `app_versions` text COLLATE utf8_unicode_ci,
  `size` int(11) DEFAULT '0',
  `developer_id` text COLLATE utf8_unicode_ci,
  `cluster_id` int(11) DEFAULT '-1',
  `status` int(11) DEFAULT '0',
  `app_id` varchar(50) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

# Database: crawler
# Table: 'Developers''
#
CREATE TABLE `Developers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `developer_id` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `store_location` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `crawler_point` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `app_type` text COLLATE utf8_unicode_ci,
  `app_ids` text COLLATE utf8_unicode_ci,
  `date` datetime DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

# Database: crawler
# Table: 'AppIds'
#
CREATE TABLE `AppIds` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app_id` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `store_location` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `category` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `sub_category` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `crawler_point` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `date` datetime DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

# Database: crawler
# Table: 'AppIds'
#
CREATE TABLE `StoreParameters` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `app_store` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `type` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `attr1` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `attr2` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `attr3` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `attr4` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `attr5` varchar(50) COLLATE utf8_unicode_ci DEFAULT '',
  `date` datetime DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


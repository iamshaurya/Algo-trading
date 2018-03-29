# ************************************************************
# Sequel Pro SQL dump
# Version 4499
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 127.0.0.1 (MySQL 5.7.20)
# Database: nse
# Generation Time: 2018-02-25 10:28:49 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table kite_account
# ------------------------------------------------------------

CREATE TABLE `kite_account` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `api_key` varchar(150) NOT NULL DEFAULT '',
  `secret_key` varchar(250) NOT NULL DEFAULT '',
  `user_id` varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table historical_candles
# ------------------------------------------------------------

CREATE TABLE `historical_candles` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `security_name` varchar(150) NOT NULL,
  `open` double(8,4) NOT NULL,
  `close` double(8,4) NOT NULL,
  `high` double(8,4) NOT NULL,
  `low` double(8,4) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `day` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table instruments
# ------------------------------------------------------------

CREATE TABLE `instruments` (
  `security_name` varchar(150) NOT NULL DEFAULT '',
  `security_token` bigint(20) NOT NULL,
  `instrument_type` varchar(50) NOT NULL DEFAULT '',
  `exchange` varchar(50) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table nse_100
# ------------------------------------------------------------

CREATE TABLE `nse_100` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `symbol` varchar(255) NOT NULL DEFAULT '',
  `security_token` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table prev_nse_volatility
# ------------------------------------------------------------

CREATE TABLE `prev_nse_volatility` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `symbol` varchar(255) NOT NULL DEFAULT '',
  `daily_vol` double(8,4) NOT NULL DEFAULT '0.0000',
  `yearly_vol` double(8,4) NOT NULL DEFAULT '0.0000',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table trade_ledger
# ------------------------------------------------------------

CREATE TABLE `trade_ledger` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `position_type` tinyint(4) NOT NULL,
  `atr` double(8,4) NOT NULL,
  `sl` double(8,4) NOT NULL,
  `tp` double(8,4) NOT NULL,
  `trade_entry_price` double(8,4) NOT NULL,
  `trade_exit_price` double(8,4) DEFAULT NULL,
  `security_name` varchar(150) NOT NULL DEFAULT '',
  `security_code` int(11) DEFAULT NULL,
  `order_id` varchar(200) DEFAULT '',
  `quantity` int(11) NOT NULL,
  `trade_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status` tinyint(4) NOT NULL,
  `trade_exit_reason` tinyint(3) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table trade_strategy
# ------------------------------------------------------------

CREATE TABLE `trade_strategy` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `security_name` varchar(150) NOT NULL DEFAULT '',
  `security_token` bigint(20) NOT NULL,
  `strategy_type` int(10) NOT NULL,
  `day` int(11) NOT NULL,
  `prefered_position` tinyint(4) NOT NULL,
  `margin_multiplier` double(8,4) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

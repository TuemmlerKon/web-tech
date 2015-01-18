CREATE TABLE IF NOT EXISTS `user` (
`ID` int(11) NOT NULL,
  `prename` varchar(255) NOT NULL,
  `surname` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `roles` varchar(255) NOT NULL,
  `activation` varchar(255) NOT NULL,
  `storage` int(13) NOT NULL DEFAULT '0',
  `storage_used` int(13) NOT NULL DEFAULT '0',
  `createdate` datetime NOT NULL,
  `lastlogin` datetime DEFAULT NULL,
  `default_encrypt` tinyint(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;

ALTER TABLE `user`
 ADD PRIMARY KEY (`ID`);

ALTER TABLE `user`
MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=0;
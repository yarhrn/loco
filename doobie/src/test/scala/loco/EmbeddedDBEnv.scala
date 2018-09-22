package loco

import com.wix.mysql.EmbeddedMysql.anEmbeddedMysql
import com.wix.mysql.distribution.Version.v5_7_latest

object EmbeddedDBEnv {
  val schema = "loco"

  val mysqld = anEmbeddedMysql(v5_7_latest)
    .addSchema(schema).start()

  val port = mysqld.getConfig.getPort
  val username = mysqld.getConfig.getUsername
  val password = mysqld.getConfig.getPassword
}
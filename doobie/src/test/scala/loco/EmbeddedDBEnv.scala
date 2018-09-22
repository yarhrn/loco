package loco

import com.wix.mysql.EmbeddedMysql.anEmbeddedMysql
import com.wix.mysql.distribution.Version.v5_7_latest

object EmbeddedDBEnv {
  val schema = "loco"

  val db = anEmbeddedMysql(v5_7_latest).addSchema(schema).start()

  val port = db.getConfig.getPort
  val username = db.getConfig.getUsername
  val password = db.getConfig.getPassword
}
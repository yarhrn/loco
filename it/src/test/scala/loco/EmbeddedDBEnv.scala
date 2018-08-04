package loco

import com.wix.mysql.EmbeddedMysql.anEmbeddedMysql
import com.wix.mysql.distribution.Version.v5_7_latest
import com.wix.mysql.Sources.fromString

object EmbeddedDBEnv {

  val schema = "loco"

  val mysqld = anEmbeddedMysql(v5_7_latest)
    .addSchema(schema, fromString(
      """
        create table increment_events
        (
        	aggregate_id varchar(250) not null,
        	events text null,
        	created_at datetime(3) null,
        	aggregate_version int not null,
        	primary key (aggregate_id, aggregate_version)
        )
      """)).start()


  val port = mysqld.getConfig.getPort
  val username = mysqld.getConfig.getUsername
  val password = mysqld.getConfig.getPassword


}

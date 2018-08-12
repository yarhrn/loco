package loco

import com.wix.mysql.Sources
import org.scalatest.{BeforeAndAfterEach, Suite}

trait ITTest extends Suite with BeforeAndAfterEach {


  def schemaScript: String

  override def beforeEach(): Unit = {
    EmbeddedDBEnv.mysqld.reloadSchema(EmbeddedDBEnv.schema, Sources.fromString(schemaScript))
  }
}

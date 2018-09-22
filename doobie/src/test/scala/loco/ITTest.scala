package loco

import com.wix.mysql.Sources
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait ITTest extends Suite with BeforeAndAfterEach with BeforeAndAfterAll {


  def schemaScript: String

  override def beforeEach(): Unit = {
    EmbeddedDBEnv.mysqld.reloadSchema(EmbeddedDBEnv.schema, Sources.fromString(schemaScript))
  }

  override def afterAll() = {
    EmbeddedDBEnv.mysqld.stop()
  }
}
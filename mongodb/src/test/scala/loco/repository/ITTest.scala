package loco.repository

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait ITTest extends Suite with BeforeAndAfterEach with BeforeAndAfterAll {


  def schemaScript: String

  override def beforeAll(): Unit = {
    EmbeddedDBEnv.givenUniqueIndex()
  }
}
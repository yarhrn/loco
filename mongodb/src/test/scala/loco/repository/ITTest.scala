package loco.repository

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait ITTest extends Suite with BeforeAndAfterEach with BeforeAndAfterAll {


  override def beforeAll(): Unit = {
    EmbeddedDBEnv.givenUniqueIndex()
  }
}
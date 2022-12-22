package loco

import java.sql.DriverManager
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.testcontainers.utility.DockerImageName

trait EmbeddedPosrtesqlDBEnv extends BeforeAndAfterAll {
  self: Suite =>

  override def beforeAll(): Unit = {
    postgres.start()
    val connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
    val statement = connection.createStatement()
    val source = io.Source.fromResource("./example.sql")
    statement.execute(source.getLines().mkString("\n"))
    source.close()
    connection.close()

  }

  override def afterAll(): Unit = {
    postgres.stop()
  }

  var postgres: PostgreSQLContainer = new PostgreSQLContainer(Some(DockerImageName.parse("postgres:11")))

}

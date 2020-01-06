package loco

import java.sql.DriverManager

import org.scalatest.{BeforeAndAfterAll, Suite}
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql.distribution.Version

trait EmbeddedPosrtesqlDBEnv extends BeforeAndAfterAll {
  self: Suite =>

  override def beforeAll(): Unit = {
    val postgres1 = new EmbeddedPostgres(Version.V11_1)
    postgres1.start()
    val connection = DriverManager.getConnection(postgres1.getConnectionUrl.get, EmbeddedPostgres.DEFAULT_USER, EmbeddedPostgres.DEFAULT_PASSWORD)
    val statement = connection.createStatement()
    val source = io.Source.fromResource("./example.sql")
    statement.execute(source.getLines().mkString("\n"))
    source.close()
    connection.close()

    postgres = postgres1
  }

  override def afterAll(): Unit = {
    postgres.stop()
  }

  var postgres:EmbeddedPostgres = _

}

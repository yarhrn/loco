package loco.repository

import cats.effect.IO
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.client.model.{IndexOptions, Indexes}
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.{MongodConfigBuilder, Net}
import de.flapdoodle.embed.mongo.distribution.Version
import loco.IncrementFixture
import loco.repository.persistent.Codec
import org.reactivestreams.{Subscriber, Subscription}

object EmbeddedDBEnv {
  val port = 12345
  val starter = MongodStarter.getDefaultInstance
  val bindIp = "localhost"

  val mongodConfig = new MongodConfigBuilder()
    .version(Version.Main.PRODUCTION)
    .net(new Net(bindIp, port, false))
    .build()

  val db = starter.prepare(mongodConfig).start()

  val client = MongoClients.create(s"mongodb://$bindIp:$port")
  implicit val codec = Codec.fromJsonCodec(IncrementFixture.jsonValueCodec)
  val collection = client.getDatabase("loco").getCollection("increment")

  def givenUniqueIndex(aggregateId: String = "aggregate_id", version: String = "version"): Unit = {
    IO.async { cb: (Either[Throwable, Unit] => Unit) =>
      collection.createIndex(Indexes.ascending(aggregateId, version),
        new IndexOptions().unique(true)).subscribe(new Subscriber[String]() {
        def onSubscribe(s: Subscription): Unit = {
          s.request(1) // <--- Data requested and the insertion will now occur

        }
        def onNext(success: String): Unit = {

        }
        def onError(t: Throwable): Unit = {
          t.printStackTrace()
          cb(Left(t))
        }
        def onComplete(): Unit = {
          cb(Right(()))
        }
      })
    }.unsafeRunSync()
  }


}
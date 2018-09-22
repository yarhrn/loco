package loco.repository.mongo


import cats.data.NonEmptyList
import cats.effect.IO
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.MongoClients
import com.mongodb.client.model.{IndexOptions, Indexes}
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.{MongodConfigBuilder, Net}
import de.flapdoodle.embed.mongo.distribution.Version
import loco.IncrementFixture._
import loco.domain.{AggregateVersion, MetaEvent}
import loco.repository.persistent.sql.Codec
import loco.test.FakeTimer
import loco.{IncrementFixture, UnitSpec}
import org.bson.Document
import org.bson.codecs.BsonTypeClassMap
import org.bson.codecs.configuration.CodecRegistry

import scala.util.Try

class MongoDBEventsRepositoryTest extends UnitSpec {


  trait ctx extends IncrementFixture {

    val port = 12345

    val starter = MongodStarter.getDefaultInstance
    val bindIp = "localhost"
    val mongodConfig = new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net(bindIp, port, false))
      .build()

    val process = starter.prepare(mongodConfig).start()

    val client = MongoClients.create(s"mongodb://$bindIp:$port")
    implicit val codec = Codec.fromJsonCodec(IncrementFixture.jsonValueCodec)
    val collection = client.getDatabase("loco").getCollection("increment")

    IO.async { cb: (Either[Throwable, Unit] => Unit) =>
      collection.createIndex(Indexes.ascending("aggregate_id", "version"),
        new IndexOptions().unique(true), new SingleResultCallback[String] {
          override def onResult(result: String, t: Throwable): Unit = {
            cb(Right(()))
          }
        })
    }.unsafeRunSync()

    val repository = new MongoDBEventsRepository[IO, IncrementEvent](collection)
    val timer = FakeTimer[IO]()
  }

  "Mongodb events repository" should "save events and retrieve events" in new ctx {

    val metaEvents: NonEmptyList[MetaEvent[IncrementEvent]] = NonEmptyList.fromListUnsafe(
      List.tabulate(10)(counter => metaEventFrom(newEvent, timer.tick().instant, counter + 1))
    )

    repository.saveEvents(metaEvents).unsafeRunSync()

    repository.fetchEvents(id, AggregateVersion.max).compile.to[List].unsafeRunSync() shouldBe metaEvents.toList
  }


}

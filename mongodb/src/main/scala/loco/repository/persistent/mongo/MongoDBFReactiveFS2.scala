/*
  copied from https://github.com/fiadliel/fs2-mongodb/blob/master/src/main/scala/org/lyranthe/fs2_mongodb/imports.scala
 */
package loco.repository.persistent.mongo

import cats.effect.ConcurrentEffect
import cats.implicits._
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.WriteModel
import com.mongodb.client.result.{DeleteResult, UpdateResult}
import com.mongodb.reactivestreams.client.MongoCollection
import fs2.interop.reactivestreams._
import org.bson.conversions.Bson

import scala.collection.JavaConverters._

object MongoDBFReactiveFS2 {


  implicit class MongoCollectionSyntax[A](collection: MongoCollection[A]) {
    def effect[F[_]]: ReactiveMongoCollectionEffect[F, A] = new ReactiveMongoCollectionEffect[F, A](collection)
  }

}

class ReactiveMongoCollectionEffect[F[_], A](val underlying: MongoCollection[A]) {

  def bulkWrite(requests: List[WriteModel[A]])(implicit F: ConcurrentEffect[F]): F[BulkWriteResult] = {
    fromPublisher(underlying.bulkWrite(requests.asJava)).compile.lastOrError
  }

  def count(implicit F: ConcurrentEffect[F]): F[Long] = {
    fromPublisher(underlying.countDocuments()).compile.lastOrError.map(_.longValue())
  }

  def count(filter: Bson)(implicit F: ConcurrentEffect[F]): F[Long] = {
    fromPublisher(underlying.countDocuments(filter)).compile.lastOrError.map(_.longValue())
  }

  def insertOne(document: A)(implicit F: ConcurrentEffect[F]): F[Unit] = {
    fromPublisher(underlying.insertOne(document)).compile.lastOrError.void
  }

  def insertMany(documents: Seq[A])(implicit F: ConcurrentEffect[F]): F[Unit] = {
    fromPublisher(underlying.insertMany(documents.asJava)).compile.lastOrError.void
  }

  def updateOne(filter: Bson, update: Bson)(implicit F: ConcurrentEffect[F]): F[UpdateResult] = {
    fromPublisher(underlying.updateOne(filter, update)).compile.lastOrError
  }

  def updateMany(filter: Bson, update: Bson)(implicit F: ConcurrentEffect[F]): F[UpdateResult] = {
    fromPublisher(underlying.updateMany(filter, update)).compile.lastOrError
  }

  def deleteMany(filter: Bson)(implicit F: ConcurrentEffect[F]): F[DeleteResult] = {
    fromPublisher(underlying.deleteMany(filter)).compile.lastOrError
  }
}

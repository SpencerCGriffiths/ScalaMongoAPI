package repositories

import cats.data.EitherT
import com.google.inject.ImplementedBy
import models.APIError.{BadAPIResponse, DatabaseError}
import models.{APIError, DataModel}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.empty
import org.mongodb.scala.model._
import org.mongodb.scala.result
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.xml.dtd.ValidationException
import scala.xml.parsing.FatalError

@ImplementedBy(classOf[DataRepository])
trait DataRepositoryTrait {
  def index(): Future[Either[APIError, Seq[DataModel]]]
  def create(book: DataModel): Future[Either[APIError, DataModel]]
  def read(id: Option[String], name: Option[String]): Future[Either[APIError, DataModel]]
  def update(id: String, book: DataModel): Future[Either[APIError, result.UpdateResult]]
  def delete(id: String): Future[Either[APIError, result.DeleteResult]]
  def deleteAll(): Future[Either[APIError, Unit]]

  //^ Methods without a body are always abstract

//  // Helper methods for filtering
//  def byID(id: String): Bson = Filters.equal("_id", id)
//  def byName(name: String): Bson = Filters.equal("name", name)
}


// This creates a new DataRepository class and injects dependencies into it required for every Mongo Repository
// extends PlayMongoRepository[DataModel] - sets the structure of our data using app.models.DataModel.scala
// every document now requires the data structure we have set
@Singleton
class DataRepository @Inject()(
                                mongoComponent: MongoComponent
                              )(implicit ec: ExecutionContext) extends PlayMongoRepository[DataModel](


  // These lines pass required params:
  collectionName = "dataModels", // is the name of the collection (can be changed)
  mongoComponent = mongoComponent,
  domainFormat = DataModel.formats, // uses the implicit val formats created. Tells the driver how to read and write the DataModel and JSON
  indexes = Seq(IndexModel( // indexes shows structure of the data stored in mongo, here we are making bookID unique
    Indexes.ascending("_id")
  )),
  replaceIndexes = false
) with DataRepositoryTrait {


  override def index(): Future[Either[APIError, Seq[DataModel]]] = {
    collection.find().toFuture().map { books =>
      Right(books)
    }.recover {
      case _ =>
        Left(APIError.DatabaseError("Error: bad response from database"))
      // TODO 02/08 10:29 - Integration testing: This will require mocking the database
      // Return here if extra time.
      // HTTP 500 Internal server error
      //NonFatal is used to catch non-fatal exceptions. It's a best practice in Scala to use NonFatal to avoid catching serious errors like OutOfMemoryError, StackOverflowError, etc.
    }
  }

  override def create(book: DataModel): Future[Either[APIError, DataModel]] = {
    collection
      .insertOne(book)
      .toFuture()
      .map(_ => Right(book))
      .recover {
        case _ =>
          Left(APIError.DatabaseError("Error: bad response from database"))
      }

  }

  private def byID(id: String): Bson =
    Filters.and(
      Filters.equal("_id", id)
    )

  private def byName(name: String): Bson = {
    Filters.and(
      Filters.equal("name", name)
    )
  }

  override def read(id: Option[String], name: Option[String]): Future[Either[APIError, DataModel]] = {
    val filter = (id, name) match {
      case (Some(id), _) => byID(id)
      case (_, Some(name)) => byName(name)
    }

    collection.find(filter).headOption().map {
      case Some(data: DataModel) => Right(data)
      case None => Left(APIError.NotFound("Data not found"))
    }.recover {
      case _ => Left(APIError.DatabaseError("Error: bad response from database"))
    }
  }

  override def update(id: String, book: DataModel): Future[Either[APIError, result.UpdateResult]] = {
    collection.replaceOne(
      filter = byID(id),
      replacement = book,
      options = new ReplaceOptions().upsert(false)
    ).toFuture().map { updateResult =>
      println(updateResult)
      Right(updateResult)
    }.recover {
      case _ => Left(APIError.DatabaseError("Error: Bad response from database"))
    }
  }


  override def delete(id: String): Future[Either[APIError, result.DeleteResult]] = {
    collection.deleteOne(
      filter = byID(id)
    ).toFuture().map { updateResult =>
      Right(updateResult)
    }.recover {
      case _ => Left(APIError.DatabaseError("Error: Bad response from database"))
    }
  }


  override def deleteAll(): Future[Either[APIError, Unit]] = {
    collection.deleteMany(empty())
      .toFuture()
      .map { _ => Right(()) }
      .recover {
        case _ => Left(APIError.DatabaseError("Error: Bad response from database"))
      }
  } //Hint: needed for tests

}

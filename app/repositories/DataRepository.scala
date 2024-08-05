package repositories

import cats.data.EitherT
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
) {



  def index(): Future[Either[APIError, Seq[DataModel]]] =
    collection.find().toFuture().map { books =>
      Right(books)
       }. recover {
      case NonFatal(e) =>
        Left(APIError.DatabaseError(s"Error: ${e.getMessage}"))
        // TODO 02/08 10:29 - Integration testing: This will require mocking the database
        // Return here if extra time.
        // HTTP 500 Internal server error
        //NonFatal is used to catch non-fatal exceptions. It's a best practice in Scala to use NonFatal to avoid catching serious errors like OutOfMemoryError, StackOverflowError, etc.
    }

  def create(book: DataModel): Future[DataModel] = {
    collection
      .insertOne(book)
      .toFuture()
      .map(_ => book)
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

  def read(id: Option[String], name: Option[String]): Future[Option[DataModel]] = {

    val filter = (id, name) match {
      case (Some(id), _) => byID(id)
      case (_, Some(name)) => byName(name)
      // already handled the non case in Application Controller so it is not required here
    }
    collection.find(filter).headOption() flatMap {
      case Some(data: DataModel) => Future.successful(Some(data))
      case None => Future.successful(None)
      //^ 31/7 10:15 - Updated this function to use Option so that you can handle Non option
    }
  }.recover {
    case _: Throwable => throw new Exception("Database error")
  }


  def update(id: String, book: DataModel): Future[result.UpdateResult] = {
    collection.replaceOne(
      filter = byID(id),
      replacement = book,
      options = new ReplaceOptions().upsert(false)
      // 31/7 10:30 When set to true a new document will be created when updating rather than returning an error
      // 31/7 10:30 if set to false then a new document will not be created and no update unless the ID exists causing it to fail
    ).toFuture()
    }


  def delete(id: String): Future[result.DeleteResult] =
    collection.deleteOne(
      filter = byID(id)
    ).toFuture()

  def deleteAll(): Future[Unit] = collection.deleteMany(empty()).toFuture().map(_ => ()) //Hint: needed for tests

}

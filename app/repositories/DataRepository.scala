package repositories

import models.DataModel
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.empty
import org.mongodb.scala.model._
import org.mongodb.scala.result
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
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

  // Each of these is a CRUD Function:
  def index(): Future[Either[Int, Seq[DataModel]]]  =
    collection.find().toFuture().map{
      case books: Seq[DataModel] => Right(books)
      case _ => Left(404)
    }

  def create(book: DataModel): Future[DataModel] =
    collection
      .insertOne(book)
      .toFuture()
      .map(_ => book)

  private def byID(id: String): Bson =
    Filters.and(
      Filters.equal("_id", id)
    )

  def read(id: String): Future[Option[DataModel]] =
    collection.find(byID(id)).headOption flatMap {
      case Some(data) => Future.successful(Some(data))
      case None => Future.successful(None)
        //^ 31/7 10:15 - Updated this function to use Option so that you can handle Non option
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

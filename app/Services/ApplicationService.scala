package Services
import Connectors.ApplicationConnector
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import cats.data.EitherT
import cats.implicits._
import models.{APIError, DataModel}
import play.api.libs.json.Format.GenericFormat

import java.awt.print.Book
import javax.inject.Inject


class ApplicationService @Inject()(connector: ApplicationConnector) {

  def getGoogleBook(urlOverride: Option[String] = None, search: String, term: String)
                   (implicit ec: ExecutionContext): EitherT[Future, APIError, DataModel] = {
    val url = urlOverride.getOrElse(s"https://www.googleapis.com/books/v1/volumes?q=$search:$term")
    //^^ Changed the URL in order to better to search
    // this creates the url. If urlOverride is provided this is used. Otherwise it constructs Url with search and term

    connector.get[JsValue](url)(Reads.JsValueReads, ec).subflatMap { json =>
      // ^^ calls the get method in the connector, uses the above url and interprets the response as JsValue (Json)
      // ^^ Reads.JsValueReads <- This is implicit val that tells the get method to read or parse the response as Json
      // ^^ ec <- implicit ExecutionContext used for handling the asynchronous operation
      if ((json \ "totalItems").asOpt[Int].getOrElse(0) == 0) {
        Left(APIError.NotFound("Error: No items found with search terms"))
      } else {
        mapToDataModel(json) match {
          // This calls the mapToDataModel method that maps the json response to the DataModel
          case Right(dataModel) => Right(dataModel)
          // If the Map was successful it returns the dataModel and will send this forwards to front end
          case Left(error) =>
            Left(error)
          // If this was unsuccessful then return the relevant error as a Left
        }
      }
    }
  }


  private def mapToDataModel(json: JsValue): Either[APIError, DataModel] = {

        (json \ "items").asOpt[JsArray].flatMap(_.value.headOption) match {
          case Some(item) =>
            // Handle normal case where items are present
            val id = (item \ "id").as[String]
            val title = (item \ "volumeInfo" \ "title").as[String]
            val description = (item \ "volumeInfo" \ "description").asOpt[String].getOrElse("")
            val pageCount = (item \ "volumeInfo" \ "pageCount").asOpt[Int].getOrElse(0)

            Right(DataModel(id, title, description, pageCount))
          case None =>
            println("correct error")
            Left(APIError.NotFound("Error: No items found with search terms"))
          // Handle case where the API didn't send an error but something is wrong/missing
        }

    }
}


// ^ TODO Information: 31/7  12:50
// ^ We are using google books api, hence the url as it is a free resource
// ^ urlOverride is a way to provide full url
// ^ term is the special keyword that can allow searching in particular fields with search


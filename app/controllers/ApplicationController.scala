package controllers

import Services.{ApplicationService, RepositoryService}
import com.mongodb.client.result.DeleteResult
import models.APIError.{BadAPIResponse, DatabaseError}
import models.{APIError, DataModel, PartialDataModel}
import play.api.http.Status.{OK, isClientError}
import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import play.api.mvc.Results
import repositories.DataRepository

import javax.inject._
import scala.concurrent.{Await, ExecutionContext, Future}
import play.api.mvc._

import scala.concurrent.duration.DurationInt


@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents, val dataRepository: DataRepository, val service: ApplicationService, val repoService: RepositoryService)(implicit val ec: ExecutionContext) extends BaseController {


  def index(): Action[AnyContent] = Action.async { implicit request =>
  // ^ Method signature, returns an Action that handles AnyContent (Json, form data etc.)
  // ^ Action.async indicates it will be handled asynchronously
  // ^ implicit request => means that the request object is implicitly available in the block
    repoService.index().map{
      //^ calls the index method on dataRepository, which returns Future[Either[Int, Seq[DataModel]]]
      //^ .map is used to transform the Future once it completes
      case Right(item: Seq[DataModel]) if item.nonEmpty => Ok {Json.toJson(item)}
      case Right(item: Seq[DataModel]) if item.isEmpty => NoContent
        // ^ if the result is a Right with Seq[DataModel] it converts to Json and returns ok
      case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        //^ if the result is Left error it returns a response with status error and a message
    }
  }

  def create(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    // ^ returns an Action handling JSON (JsValue)
    request.body.validate[DataModel] match {
      //^ Attempts to validate the JSON body of the request as a DataModel object.
      // ^ The validate method checks if the JSON can be successfully converted to a DataModel
      case JsSuccess(dataModel, _) =>
        // ^ If validation is successful "JsSuccess"  this block is executed with dataModel being the validated model
        repoService.create(dataModel).map {
          case Right(created) => Created(Json.toJson(created))
          case Left(error) => InternalServerError(Json.toJson(error.reason))
        }
          //^ Calls the create method with the dataModel and returns a Future
          //^ The map section transforms the successful result into a HTTP "created" response
      /** We could validate the fields of book here as the dataModel */
      case JsError(_) => Future(BadRequest(Json.toJson("Error in create: ")))
      // ^If validation fails JsError
      // ^ Future(BadRequest) returns a Future containing a HTTP BadRequest response
    }
  }

  def read(): Action[AnyContent] = Action.async { implicit request =>
    val idParam = request.getQueryString("id")
    val nameParam = request.getQueryString("name")

    val rawResult: Future[Either[APIError, DataModel]] = (idParam, nameParam) match {
      case (Some(id), _) => dataRepository.read(Some(id), None)
      case (_, Some(name)) => dataRepository.read(None, Some(name))
      case (None, None) => Future.successful(Left(APIError.BadRequest("Either id or name must be provided")))
    }

    rawResult.map {
      case Right(dataModel) => Ok(Json.toJson(dataModel))
      case Left(APIError.NotFound(msg)) => NotFound(Json.toJson(msg))
      case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
    }.recover {
      case e: IllegalArgumentException => BadRequest(Json.toJson(e.getMessage))
      case _: Exception => {
        val apiError = APIError.DatabaseError("Error: bad response from database")
        Status(apiError.httpResponseStatus)(Json.toJson(apiError.reason))
      }
    }
  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>

    // Step 1 validate incoming Json Object
    request.body.validate[JsObject] match {
      case JsSuccess(incomingJson, _) =>
        // PATH 1 - It is valid Json and can be a JsObject

        // Step 2 - Validate if the object can be converted to PartialDataModel
        val partialDataResult = incomingJson.validate[PartialDataModel]
        // Validate it as a PartialDataModel
        partialDataResult match {
          case JsSuccess(partialData, _) =>
            // Path 1A it is a data model

            // Step 3- perform a read to get the original data
            repoService.read(Some(id), None).flatMap {
              // Perform a read to get the original data
                case Left(APIError.NotFound(msg)) =>
                  Future.successful(NotFound(Json.toJson(msg)))
                // If the data is not present because it doesn't exist return not found

                case Left(error) =>
                  Future.successful(Status(error.httpResponseStatus)(Json.toJson(error.reason)))
                // If there is another error, return the appropriate status and error message

                case Right(existingData) =>
                  // If the data does exist

                  val updatedData = DataModel(
                    _id = existingData._id,  // ID has to stay the same
                    name = partialData.name.getOrElse(existingData.name), // if name is provided replace
                    description = partialData.description.getOrElse(existingData.description), // if provided replace
                    pageCount = partialData.pageCount.getOrElse(existingData.pageCount) // if provided replace
                  )

                // Step 4- validate the combined data as a DataModel for the update
                Json.toJson(updatedData).validate[DataModel] match {
                  // Now we have the combined data validate again that it is a DataModel
                  case JsSuccess(validatedDataModel, _) =>
                    // If we can validate the dataModel do the update

                    // Step 5- perform the update
                    repoService.update(id, validatedDataModel).map {
                      case Right(value) if value.getMatchedCount == 0 => NotFound(Json.toJson("Data not found"))
                      // If for some crazy reason we have got to here and the data isnt found return not found
                      case Right(value) if value.getMatchedCount == 1 => Accepted(Json.toJson(validatedDataModel))
                        // If the data was found and it is all good have an accepted and the validatedBook reutrned
                      case Left(error) => BadRequest(Json.toJson(error.reason))
                    }

                  case JsError(errors) =>
                    // Error - The combined data could not be validated as a DataModel
                    Future.successful(BadRequest(Json.toJson("Error: The data could not be validated as a DataModel")))
                }
            }
          case JsError(errors) =>
            // Error- the thing entered was not a valid PartialDataModel but was Json Object
            Future.successful(BadRequest(Json.toJson("Error: The input was not a valid PartialDataModel")))
        }
      case JsError(errors) =>
        // Error- the thing entered was not a valid Json Object
        Future.successful(BadRequest(Json.toJson("Error: The input was not a valid Json Object")))
    }
  }

  //  (updatedBook => Accepted)
  //^ TODO 31/7  -14:30 SCG - Other variation of update return
  //^ The above method could just return accepted however the way it is currently written allows for the appropriate Json but the object not to exist and gives more information.


  def delete(id:String): Action[AnyContent] = Action.async { implicit request =>
  repoService.delete(id).map {
    case Right(data) if data.getDeletedCount == 0 => NotFound(Json.toJson("Data not found"))
    case Right(data) if data.getDeletedCount > 0 =>  Accepted {Json.toJson("Entry has been removed")}
    case Left(error) => BadRequest(Json.toJson(error.reason))
    //^ 31/7 10:54 - delete returns a delete count of 0 or more.
  }
  }


// Using the ApplicationConnector and ApplicationService:

  def getGoogleBook(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGoogleBook(search = search, term = term).value.map {
      case Right(book) =>
        Ok(Json.toJson(book)) // Return the book as JSON if found
      case Left(error) =>
        Status(error.httpResponseStatus)(Json.toJson(error.reason))// Return error message as JSON
    }
  }

//  def getGoogleBookISBN(): Unit = {
//    val futureResult = service.getGoogleBook(search = "isbn", term = "0345339703").value
//
//    // Wait for the future to complete and get the result
//    val result = Await.result(futureResult, 5.seconds)
//
//    val completeBook = result match {
//      case Right(book) => Ok(views.html.)
//        // Handle the successful case, returning the book
//        book
//      case Left(error) =>
//        // Handle the error case, possibly converting it to an appropriate response
//        Status(error.httpResponseStatus)(Json.toJson(error.reason))
//    }
//
//    println(completeBook)
//  }

  /** Call getgooglebook with ISBN complete  */
    // browser url
        // Take the ID from the call
    // controller
        //Use the controller to call the get book method in service with the ID and isbn
    // service
        // Service naturally calls the connector to actach to googlebooks
    // connector
        // Connector connects to google books
    // Google Books
        // Google books returns a book
    // Connector
        // Connector receives this and sends the succesful response to the service
    // Service
        // Service takes the Json and converts to a data model
    // views
    // service
    // controller

}

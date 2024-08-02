package controllers

import Services.ApplicationService
import com.mongodb.client.result.DeleteResult
import models.APIError.{BadAPIResponse, DatabaseError}
import models.{APIError, DataModel}
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import play.api.mvc.Results
import repositories.DataRepository

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents, val dataRepository: DataRepository, val service: ApplicationService)(implicit val ec: ExecutionContext) extends BaseController {


  def index(): Action[AnyContent] = Action.async { implicit request =>
  // ^ Method signature, returns an Action that handles AnyContent (Json, form data etc.)
  // ^ Action.async indicates it will be handled asynchronously
  // ^ implicit request => means that the request object is implicitly available in the block
    dataRepository.index().map{
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
        dataRepository.create(dataModel).map { createdDataModel =>
          Created(Json.toJson(createdDataModel))
        }
          //^ Calls the create method with the dataModel and returns a Future
          //^ The map section transforms the successful result into a HTTP "created" response
      /** We could validate the fields of book here as the dataModel */
      case JsError(_) => Future(BadRequest(Json.toJson("Error in create: ")))
      // ^If validation fails JsError
      // ^ Future(BadRequest) returns a Future containing a HTTP BadRequest response
    }
  }

  // Could boost up this method and test that the parameters are valid etc.


  def read(): Action[AnyContent] = Action.async { implicit request =>
    val idParam = request.getQueryString("id")
    val nameParam = request.getQueryString("name")

    val rawResult: Future[Option[DataModel]] = (idParam, nameParam) match {
      case (Some(id), _) => dataRepository.read(Some(id), None)
      case (_, Some(name)) => dataRepository.read(None, Some(name))
      case (None, None) => Future.failed(new IllegalArgumentException("Either id or name must be provided"))
    }

    rawResult.map {
      case Some(dataModel) => Ok(Json.toJson(dataModel))
      case None => NotFound(Json.toJson("Data not found"))
    }.recover {
      case e: IllegalArgumentException => BadRequest(Json.toJson(e.getMessage))
      case _: Exception => {
        val apiError = DatabaseError("Error msg: bad response from database")
        Status(apiError.httpResponseStatus)(Json.toJson(apiError.reason))
        // TODO 02/08 15:15 -> Database error to be added Done - no testing
        // add a case of e: BadAPI request.. return in DataRepository would need to return this
      }
    }
  }

  def update(id:String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        dataRepository.update(id, dataModel).map {
          case result if result.getMatchedCount == 0 => NotFound(Json.toJson("Data not found"))
          case result if result.getMatchedCount == 1 => Accepted {Json.toJson(dataModel)}
        }
      case JsError(_) => Future.successful(BadRequest)
    }
  }
  //  (updatedBook => Accepted)
  //^ TODO 31/7  -14:30 SCG - Other variation of update return
  //^ The above method could just return accepted however the way it is currently written allows for the appropriate Json but the object not to exist and gives more information.


  def delete(id:String): Action[AnyContent] = Action.async { implicit request =>
  dataRepository.delete(id).map {
    case result if result.getDeletedCount == 0 => NotFound(Json.toJson("Data not found"))
    case result if result.getDeletedCount > 0 =>  Accepted {Json.toJson("Entry has been removed")}
    //^ 31/7 10:54 - delete returns a delete count of 0 or more.
  }
  }

// Using the ApplicationConnector and ApplicationService:

  def getGoogleBook(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGoogleBook(search = search, term = term).value.map {
      case Right(book) => Accepted //Hint: This should be the same as before
      case Left(error) => Status(error.httpResponseStatus)
    }
  }
}

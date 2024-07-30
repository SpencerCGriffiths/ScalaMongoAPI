package controllers

import com.mongodb.client.result.DeleteResult
import models.DataModel
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import play.api.mvc.Results
import repositories.DataRepository

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents, val dataRepository: DataRepository)(implicit val ec: ExecutionContext) extends BaseController {


  def index(): Action[AnyContent] = Action.async { implicit request =>
  // ^ Method signature, returns an Action that handles AnyContent (Json, form data etc.)
  // ^ Action.async indicates it will be handled asynchronously
  // ^ implicit request => means that the request object is implicitly available in the block
    dataRepository.index().map{
      //^ calls the index method on dataRepository, which returns Future[Either[Int, Seq[DataModel]]]
      //^ .map is used to transform the Future once it completes
      case Right(item: Seq[DataModel]) => Ok {Json.toJson(item)}
        // ^ if the result is a Right with Seq[DataModel] it converts to Json and returns ok
      case Left(error) => Status(error)(Json.toJson("Unable to find any books"))
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
        dataRepository.create(dataModel).map(_ => Created)
          //^ Calls the create method with the dataModel and returns a Future
          //^ The map section transforms the successful result into a HTTP "created" response
      /** We could validate the fields of book here as the dataModel */
      case JsError(_) => Future(BadRequest)
      // ^If validation fails JsError
      // ^ Future(BadRequest) returns a Future containing a HTTP BadRequest response
    }
  }

  def read(id: String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.read(id).map {
      case dataModel => Ok {Json.toJson(dataModel)}
      case _ => NotFound(Json.toJson("Data not found"))
    }
  }

  def update(id:String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        dataRepository.update(id, dataModel).map {
          case result if result.wasAcknowledged() => Accepted
          case result if !result.wasAcknowledged() => NotFound
        }
      case JsError(_) => Future.successful(BadRequest)
    }
  }

  def delete(id:String): Action[AnyContent] = Action.async { implicit request =>
  dataRepository.delete(id).map {
    case result if result.wasAcknowledged() => Accepted // {Json.toJson(result.wasAcknowledged())}
    //^ delete returns a result.DeleteResult -> if possible this returns true if not it returns false
    case result if !result.wasAcknowledged() => NotFound(Json.toJson("Data not found")) //
  }
  }
}

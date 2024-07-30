package controllers

import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import play.api.mvc.Results

import javax.inject._


@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  def index(): Action[AnyContent] = Action {
//    TODO
    /** Generates a ‘200 OK’ result. */
    val Ok = new Status(OK)

    Ok
  }
  def create(): Action[AnyContent] = {
    // Post
    TODO
  }

  def read(id: String): Action[AnyContent] = {
    // Get
    TODO
  }

  def update(id:String): Action[AnyContent] = {
    // PUT
    TODO
  }

  def delete(id:String): Action[AnyContent] = {
    // Delete
    TODO
  }
}

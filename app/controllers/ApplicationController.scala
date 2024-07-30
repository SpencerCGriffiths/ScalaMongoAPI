package controllers

import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import javax.inject._


@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  def index(): Action[AnyContent] = TODO

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

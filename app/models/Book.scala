package models

import play.api.libs.json.{Json, OFormat}

import scala.concurrent.Future

case class Book(
                 _id: String,
                 name: String,
                 description: String,
                 pageCount: Int
               )

object Book {
  implicit val format: OFormat[Book] = Json.format[Book]

}

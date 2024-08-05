package models

import play.api.libs.json.{Json, OFormat}

case class PartialDataModel(name: Option[String],
                            description: Option[String],
                            pageCount: Option[Int]
                           )


object PartialDataModel {
  implicit val formats: OFormat[PartialDataModel] = Json.format[PartialDataModel]
}
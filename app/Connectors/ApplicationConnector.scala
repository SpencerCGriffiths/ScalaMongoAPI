package Connectors

import cats.data.EitherT
import com.google.inject.Singleton
import models.APIError
import play.api.libs.json.OFormat
import play.api.libs.ws.{WSClient, WSResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationConnector @Inject()(ws: WSClient) {

  def get[Response](url: String)(implicit rds: OFormat[Response], ec: ExecutionContext): EitherT[Future, APIError, Response] = {
    val request = ws.url(url)
    val response = request.get()
    EitherT {
      response
        .map {
          result =>
            Right(result.json.as[Response])
        }
        .recover { case _: WSResponse =>
          Left(APIError.BadAPIResponse(500, "Could not connect"))
        }
    }
  }

}

// TODO 31/7 12:35 - This is .get[Response]()
// ^  This is a GET method and the url we are calling must expect this
// ^  implicit rds: 0Format[Response] parses the Json response model as our model
// ^  [Response] is a type parameter for our method
    // ^ This allows us to use it for several models i.e. get[Tea]("tea.com"), get[Coffee]("coffee.com")
// ^ ws.url(url) creates our request using the url
// ^ The response is made using WSClient's .get() method.
// ^ The result's json value is parsed as our response model
// ^ Current assuming that the response will have json body, that the body can be parsed in to our model and that the request was successful... but error handling will come later
// WE WILL UTILISE THIS IN SERVICES RATHER THAN CONTROLLER TO SEPARATE CONCERNS

// TODO 1/8 10:05 Information RE EitherT
//^ request.get() will give a WSResponse. We then try to parse the JSON body as Response
//^ This was a Future[Response] -> When that doesn't work catch error with .recover returning Future[APIError]
//^ EitherT allows us to return either
//^ You cannot have APIError[Response] or Response[APIError] (EitherT only allows the first type i.e. Future to be applied to the remaining
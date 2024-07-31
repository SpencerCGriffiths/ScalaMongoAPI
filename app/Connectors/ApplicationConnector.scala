package Connectors

import com.google.inject.Singleton
import play.api.libs.json.OFormat
import play.api.libs.ws.WSClient

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationConnector @Inject()(ws: WSClient) {
  def get[Response](url: String)(implicit rds: OFormat[Response], ec: ExecutionContext): Future[Response] = {
    val request = ws.url(url)
    val response = request.get()
    response.map {
      result =>
        result.json.as[Response]
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
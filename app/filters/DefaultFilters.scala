package filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter


class DefaultFilters @Inject() (implicit val mat: Materializer) extends HttpFilters {

  val gzipFilter = new GzipFilter(
    shouldGzip = (request, response) =>
      response.header.headers.get("content-type").exists( _.startsWith("text/javascript") )
  )

  def filters = Seq(gzipFilter)
}


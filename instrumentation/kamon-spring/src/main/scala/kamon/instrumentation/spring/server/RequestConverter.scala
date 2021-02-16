package kamon.instrumentation.spring.server

import kamon.instrumentation.http.HttpMessage
import org.springframework.web.servlet.HandlerMapping

import java.util.Collections
import javax.servlet.http.HttpServletRequest

object RequestConverter {
  def toRequest(incoming: HttpServletRequest): RequestReader = {
    new RequestReader {
      override def request: HttpServletRequest = incoming
    }
  }


  trait RequestReader extends HttpMessage.Request {
    def request: HttpServletRequest

    override def url: String = {
      // This attribute can be null if the HandlerAdapter doesn't support it
      // so we need a fallback.
      // Default Spring HandlerAdapters will have it, but if someone writes
      // a custom HandlerAdapter they might omit this.
      // At which point, we have a possible cardinality problem
      Option(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
        .map(_.toString)
        .getOrElse(request.getServletPath)
    }

    override def path: String = {
      url
    }

    override def method: String = {
      request.getMethod
    }

    override def host: String = {
      request.getRemoteHost
    }

    override def port: Int = {
      request.getServerPort
    }

    override def read(header: String): Option[String] = {
      Option(request.getHeader(header)).flatMap(value => {
        if (value.isEmpty) {
          None
        } else {
          Some(value)
        }
      })
    }

    override def readAll(): Map[String, String] = {
      val builder = Map.newBuilder[String, String]
      val headerNames = Option(request.getHeaderNames)
        .getOrElse(Collections.enumeration(Collections.emptyList()))

      // while loop because scala 2.11
      while (headerNames.hasMoreElements) {
        val header = headerNames.nextElement()
        // Technically, this could be null
        // so better not risk it
        Option(request.getHeader(header))
          .foreach(value => builder += (header -> value))
      }

      builder.result()
    }
  }

}

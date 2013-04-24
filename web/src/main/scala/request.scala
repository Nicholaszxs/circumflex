package pro.savant.circumflex
package web

import scala.collection.Iterator
import scala.collection.JavaConversions._
import scala.xml._
import core._
import javax.servlet.http.HttpServletRequest
import java.util.{Locale, Date}
import javax.servlet.ServletInputStream
import org.apache.commons.io.IOUtils
import org.apache.commons.fileupload._
import org.apache.commons.fileupload.disk._
import org.apache.commons.fileupload.servlet._
import java.io.{File, BufferedReader}
import java.lang.String
import java.util.{Enumeration => JEnumeration}
import collection.mutable.Map

/*!# HTTP Request

The `HttpRequest` class wraps specified `raw` `HttpServletRequest` and allows you to
use core Scala classes to operate with Servlet API.

This class is designed to efficiently cover mostly used methods of `HttpServletRequest`,
however, you still can access the `raw` field, which holds actual request. For more
information refer to Java Servlet API.

Since Circumflex is UTF-friendly it will implicitly set character encoding of request body
to `UTF-8`. Feel free to change it if your application requires so.
*/
class HttpRequest(val raw: HttpServletRequest) {

  /*!## Request Basics */

  /*!`protocol` returns the name and version of the protocol the request uses (e.g. `HTTP/1.1`). */
  def protocol = raw.getProtocol

  /*! `scheme` returns the name of the scheme used to make this request (e.g. "http", "https"
    or "ftp") */
  def scheme = raw.getScheme

  /*! `isSecure` returns `true` if the request was made using a secure channel, such as HTTPS. */
  def isSecure = raw.isSecure

  /*! `isXHR` returns true if this request is XMLHttpRequest.*/
  def isXHR: Boolean = headers.getOrElse("X-Requested-With", "") == "XMLHttpRequest"

  /*! `method` returns the name of HTTP method with which the request was made, it's value is
    overriden by request paremeter `_method` to provide a workaround for browsers.

    Also note that if the method is overriden by the `_method` parameter, the original method is
    available via the `originalMethod` method. */
  val originalMethod = raw.getMethod.toLowerCase

  val method = params.get("_method") match {
    case Some(m) => m.trim.toLowerCase
    case _ => originalMethod
  }

  /*! `uri` returns the decoded request URI without query string (this URI can be modified by
      certain routes, use `originalUri` to access unmodified original URI).

      Note that JSESSIONID encoded into URI is not reported via method `uri` (thus, it does not
  participate in matching).*/
  val originalUri = {
    var u = decodeURI(raw.getRequestURI)
    val sid = ";jsessionid=" + sessionId
    val i = u.indexOf(sid)
    if (i != -1 && i == u.length - sid.length)    // endsWith
      u = u.substring(0, i)
    u
  }

  def uri = ctx.getAs[String]("cx.web.uri").getOrElse(originalUri)

  /*! `queryString` returns the query string that is contained in the request URL
  after the path (`?` character included, undecoded). */
  def queryString = {
    if (raw.getQueryString == null) "" else
      "?" + raw.getQueryString
  }

  /*! `url` reconstructs an URL the client used to make the request
  (host and URI are decoded, query string is undecoded). */
  def url = web.origin + originalUri + queryString

  raw.setCharacterEncoding("UTF-8")

  /*! ## Prefixes

  When inside subroutes the `prefix` method returns the portion of initial URI
  that is stripped by all subroutes in the way.

  E.g. in following router:

  ``` {.scala}
  sub("/user") = {

    sub("/:id") = {

      get("/profile") = "Prefix is " + request.prefix

    }

  }
  ```

  for URL `/user/1/profile` the output will be:

  ```
  Prefix is /user/1
  ```

  Accordingly, `setPrefix` updates current prefix with specified value and
  `resetPrefix` clears out the prefix.

  */
  def prefix = raw.getContextPath +
      ctx.getAs[String]("cx.web.uriPrefix").getOrElse("")

  def setPrefix(newPrefix: String) {
    ctx.update("cx.web.uriPrefix", newPrefix)
  }

  def resetPrefix() {
    ctx -= "cx.web.uriPrefix"
  }

  /*! The `localUri` method returns "subroute-local URI" -- the request URI
  stripped from currently set `prefix` as returned by corresponding method.

  If URI does not start with `prefix`, the original `uri` is returned.
   */
  def localUri = {
    val u = uri
    val p = prefix
    if (u.startsWith(p)) u.substring(p.length)
    else u
  }

  /*!## Server information

  Following methods provide information about the server:

    * `serverHost` returns the host name of the server for which this request
      was originated;

    * `serverPort` returns the port number to which the request was sent;

    * `localIp` returns the Internet Protocol address of the interface on which
      the request was received;

    * `localHost` returns the host name of the IP interface on which
      the request was received;

    * `localPort` returns the IP port on which the request was received.
  */
  def host = headers.get("Host").getOrElse(serverHost)
  def serverHost: String = raw.getServerName
  def serverPort: Int = raw.getServerPort
  def localIp: String = raw.getLocalAddr
  def localHost: String = raw.getLocalName
  def localPort: Int = raw.getLocalPort

  /*! ## Client information

  Following methods can be used to retrieve basic information about the client:

    * `remoteIp` returns the Internet Protocol address of the client
      (or last proxy) that sent the request;

    * `remoteHost` returns the host name of the client (or last proxy)
      that sent the request;

    * `sessionId` returns session identifier specified by the client;
  */
  def remoteIp: String = Seq("X-Forwarded-For", "X-Real-Ip",
    "X-Client-Ip", "Proxy-Client-Ip", "WL-Proxy-Client-Ip",
    "Http-Client-Ip", "Http-X-Forwarded-For")
      .flatMap(key =>
    headers.find(p => p._1.equalsIgnoreCase(key) ||
        p._1.equalsIgnoreCase(key.replace('-', '_'))))
      .headOption
      .map(_._2)
      .getOrElse(raw.getRemoteAddr)

  def remoteHost: String = raw.getRemoteHost
  def sessionId = raw.getRequestedSessionId

  /*!## Locale

  A list of preferred locales is specified by the value of the `Accept-Language`
  header of the request. You can access this list and the most preferred locale
  (with maximum relative quality factor) using `locales` and `locale` fields.
  */
  def locale: Locale = raw.getLocale

  lazy val locales: Seq[Locale] = raw.getLocales
      .asInstanceOf[JEnumeration[Locale]]
      .toSeq

  /*!## Cookies

  The `cookies` field provides access to request cookies. You can use rich
  functionality of Scala collections to work with cookies:

  ``` {.scala}
  request.cookies.find(_.name == "my.cookie")
  request.cookies.filter(_.secure)
  request.cookies.groupBy(_.name)
  ```
  */
  lazy val cookies: Seq[HttpCookie] = {
    val cs = raw.getCookies
    if (cs == null) Nil
    else cs.map(c => HttpCookie(c))
  }

  /*!## Headers

  Request headers contain operational information about the request.
  Circumflex Web Framework lets you access request headers via the `headers` object.
  */
  object headers extends Map[String, String] {

    def +=(kv: (String, String)): this.type = this

    def -=(key: String): this.type = this

    def iterator: Iterator[(String, String)] = raw.getHeaderNames
        .asInstanceOf[JEnumeration[String]]
        .map(k => (k -> raw.getHeader(k)))

    def get(key: String): Option[String] = any2option(raw.getHeader(key))

    def getAsMillis(key: String): Option[Long] = {
      val m = raw.getDateHeader(key)
      if (m == -1) None else Some(m)
    }

    def getAsInt(key: String): Option[Long] = {
      val m = raw.getIntHeader(key)
      if (m == -1) None else Some(m)
    }

    def getAsDate(key: String): Option[Date] = getAsMillis(key).map(new Date(_))

  }

  /*!## Parameters

  Request parameters can be accessed via the `params` object.

  Unlike the `params` object of `pro.savant.circumflex.web` package,
  this object does not report match results -- only request parameters
  as specified in query string, form fields, etc.
  */
  object params extends Map[String, String] with KeyValueCoercion {

    def +=(kv: (String, String)): this.type = this

    def -=(key: String): this.type = this

    def iterator: Iterator[(String, String)] = raw.getParameterNames
        .asInstanceOf[JEnumeration[String]]
        .flatMap(k => list(k).iterator.map(v => (k -> v)))

    def get(key: String): Option[String] = any2option(raw.getParameter(key))

    /*! Use the `list` method to retrieve multi-value parameters.*/
    def list(key: String): Seq[String] = {
      val values = raw.getParameterValues(key)
      if (values == null) Nil
      else values.toList
    }

  }

  /*!## Body

  Circumflex Web Framework lets you access the body of the request via `body`
  object. Following methods can be used to work with request body:

    * `encoding` returns or sets the name of the character encoding used
      in the body of the request (as mentioned above, we implicitly set it to `UTF-8`);

    * `isMultipart` returns `true` if the request has `multipart/form-data`
      content and is suitable for multipart operations;

    * `length` returns the length, in bytes, of the request body;

    * `contentType` returns the MIME type of the body of the request;

    * `reader` opens `java.io.BufferedReader` to read the request body;

    * `stream` opens `javax.servlet.ServletInputStream` to read the request body;

    * `asXml` attempts to read the request body as XML element, an exception
      is thrown if parse fails;

    * `asString` reads request body into `String` using request `encoding`.

  Note that due to the limitations of Servlet API, you can only access one of
  `reader`, `stream`, `xml` or `toString` methods (and only once).
  An `IllegalStateException` is thrown if you access more than one
  of these methods.
  */
  object body {

    def isMultipart: Boolean = ServletFileUpload.isMultipartContent(raw)

    def encoding: String = raw.getCharacterEncoding

    def encoding_=(enc: String) {
      raw.setCharacterEncoding(enc)
    }

    def length: Int = raw.getContentLength

    def contentType: String = raw.getContentType

    def reader: BufferedReader = raw.getReader

    def stream: ServletInputStream = raw.getInputStream

    lazy val asXml: Elem = XML.load(stream)

    lazy val asString = IOUtils.toString(stream, encoding)

    /*!## Multipart Requests & File Uploading {#multipart}

    Standard Servlet API doesn't provide any capabilities to work with requests of MIME type
    `multipart/form-data` which are usually used by the clients to upload files on the server.

    Circumfex API uses [Apache Commons FileUpload](http://commons.apache.org/fileupload) to
    simplify this things for you. Commons FileUpload API is very robust and can be used in a
    number of different ways, depending upon the requirements of your application.

    Commons FileUpload offers you two approaches to deal with `multipart/form-data` requests:

      * _traditional API_ relies on `FileItemFactory` which can be configured to keep small files
      in memory and to store larger files on the disk, you set threshold sizes and operate with
      convenient `FileItem` objects, which could be queried for different information, like the
      name of the corresponding field, it's size in bytes, content type, etc.
      * _streaming API_ does not use intermediate storage facilities and allows you to work with
      `FileItemStream` objects, which show better performance and lower memory usage.

    Circumflex Web Framework provides support for both FileUpload styles via `parseFileItems` and
    `parseFileStreams` methods respectively.

    Note, however, that you can only use one of them (and only once) while working with the request
    (it's the limitation of accessing request body in Servlet API mentioned above, so `reader`,
    `stream`, `asXml` and `asString` methods will also interfere with FileUpload).

    For more information about configuring a FileUpload environment that will suit your needs,
    visit [Commons FileUpload Project Page](http://commons.apache.org/fileupload).
    */

    def parseFileItems(factory: FileItemFactory): Seq[FileItem] =
      if (isMultipart) {
        val uploader = new ServletFileUpload(factory)
        asScalaBuffer(uploader.parseRequest(raw).asInstanceOf[java.util.List[FileItem]])
      } else Nil

    def parseFileItems(sizeThreshold: Int, tempStorage: File): Seq[FileItem] =
      parseFileItems(new DiskFileItemFactory(sizeThreshold, tempStorage))

    def parseFileItems(sizeThreshold: Int, tempStorage: String): Seq[FileItem] =
      parseFileItems(sizeThreshold, new File(tempStorage))

    def parseFileStreams(): Iterator[FileItemStream] = if (isMultipart) {
      val it = new ServletFileUpload().getItemIterator(raw)
      new Iterator[FileItemStream]() {
        def next(): FileItemStream = it.next
        def hasNext: Boolean = it.hasNext
      }
    } else Iterator.empty

  }

  /*!## Include & Forward

  The methods `forward` and `include` use request dispatcher mechanism provided by Servlet API
  to forward the request processing to or to include partial result from different web application
  object (such as servlet).
  */
  def forward(uri: String) {
    raw.getRequestDispatcher(uri).forward(raw, response.raw)
  }

  def include(uri: String) {
    raw.getRequestDispatcher(uri).include(raw, response.raw)
  }

  override def toString: String = method + " " + uri + " " + protocol

  /*!## Session

  Session is a convenient in-memory storage presented by Servlet API which allows
  web applications to maintain state of their clients.

  A special identifier, session ID, is generated once the session is initiated.
  Clients then, to identify themselves within application, send session ID
  as a cookie with every request.

  Circumflex Web Framework lets you access session attributes via the `session` object.

  Note that if session was not already created for the request, it will only be created
  if you attempt to add an attribute into it via `update` or `+` method, all other methods
  will return empty values without implicitly creating a session.
  */
  val session = new HttpSession

}

package pro.savant.circumflex
package web

import java.io.Serializable
import javax.servlet.http.Cookie

/*!# HTTP Cookies

HTTP is stateless protocol. [RFC2965](http://www.faqs.org/rfcs/rfc2965.html) describes
a way for a web application to send state information to the user agent and for the user
agent to return the state information to the origin web application. This state information
is often referred to as _cookie_.

Circumflex Web Framework helps you set response cookies and access request cookies
throughout your application using case class `HttpCookie`. It is mutable and dead simple.
*/
case class HttpCookie(var name: String,
                      var value: String,
                      var domain: String = null,
                      var path: String = null,
                      var comment: String = null,
                      var secure: Boolean = false,
                      var maxAge: Int = -1)
    extends Serializable {

  /*! You can convert `HttpCookie` back to `javax.servlet.Cookie` using `convert` method. */
  def convert(): Cookie = {
    val c = new Cookie(name, value)
    if (domain != null) c.setDomain(domain)
    if (path != null) c.setPath(path)
    if (comment != null) c.setComment(comment)
    c.setSecure(secure)
    c.setMaxAge(maxAge)
    c
  }

  override def toString = name + " = " + value

}

/*! Depending on your application needs you can obtain an instance of `HttpCookie` by
supplying `javax.servlet.Cookie` as an argument to `apply` method of `HttpCookie` singleton:

```
HttpCookie(rawCookie)
```
*/
object HttpCookie {
  def apply(cookie: Cookie): HttpCookie =
    new HttpCookie(
      cookie.getName,
      cookie.getValue,
      cookie.getDomain,
      cookie.getPath,
      cookie.getComment,
      cookie.getSecure,
      cookie.getMaxAge)
}
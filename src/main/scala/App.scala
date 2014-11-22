package java_src

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.net.URL
import unfiltered.request._
import unfiltered.response.{Html5, Redirect, ResponseString}
import scala.util.control.NonFatal
import scala.xml.{Elem, XML}
import scalaz.{-\/, \/, \/-}

final class App extends unfiltered.filter.Plan {
  import java_src.App._

  def intent = {
    case GET(Path(Seg(org :: Nil))) =>
      searchByGroupId(org) match {
        case \/-(Nil) =>
          defaultView(<p>{"not found groupId=" + org}</p>)
        case \/-(list) =>
          defaultView(<ul>{
            list.map{ name =>
              <li><a href={s"$JavaSrcURL$org/$name"}>{name}</a></li>
            }
          }</ul>)
        case -\/(error) =>
          defaultView(<pre>{error.toString + "\n" + error.getStackTrace.mkString("\n")}</pre>)
      }
    case GET(Path(Seg(org :: name :: Nil)) & Params(p)) =>
      val baseUrl = baseURL(p)
      def showVesions = defaultView(<ul>{
        versions(baseUrl, org, name).map{ v =>
          <li><a href={s"$JavaSrcURL$org/$name/$v"}>{v}</a></li>
        }
      }</ul>)

      if(p.get("latest").toList.flatten.isEmpty){
        showVesions
      }else{
        latestVersion(baseUrl, org, name) match {
          case Some(v) =>
            Redirect(s"$JavaSrcURL$org/$name/$v")
          case None =>
            showVesions
        }
      }
   case GET(Path(Seg(org :: name :: version :: Nil)) & Params(p)) =>
     val baseUrl = baseURL(p)
     viewList(baseUrl, org, name, version)
   case GET(Path(Seg(org :: name :: version :: path)) & Params(p)) =>
     val baseUrl = baseURL(p)
     viewFile(baseUrl, org, name, version, path.mkString("/"))
  }

}

object App {

  final val BASE = "base"

  final val JavaSrcURL = "http://java-src.appspot.com/"

  final val sonatype = "https://oss.sonatype.org/content/repositories/releases/"

  private[this] val downloadZip: String => Map[String, Array[Byte]] = { url =>
    sbt.Using.urlInputStream(new URL(url)){ in =>
      sbt.Using.zipInputStream(in){ zipIn =>
        Iterator.continually(zipIn.getNextEntry).takeWhile(null ne).filterNot(_.isDirectory).map{ f =>
          val name = f.getName
          val bytes = toByteArray(zipIn)
          zipIn.closeEntry()
          name -> bytes
        }.toMap
      }
    }

  }

  private[this] val cache = new Cache(downloadZip)

  private def baseURL(params: Params.Map): String =
    param(params, BASE).getOrElse(sonatype)

  private def param(params: Params.Map, key: String): Option[String] =
    params.get(key).toList.flatten.find(_.trim.nonEmpty)

  private def metadataXml(baseUrl: String, org: String, name: String): Option[Elem] =
    try {
      val url = s"$baseUrl/${org.replace('.', '/')}/$name/maven-metadata.xml"
      Some(XML.load(url))
    }catch{
      case _: _root_.org.xml.sax.SAXParseException => // ignore
        None
      case NonFatal(e) =>
        e.printStackTrace()
        None
    }

  def latestVersion(baseUrl: String, org: String, name: String): Option[String] =
    metadataXml(baseUrl, org, name).flatMap{ x =>
      (x \ "versioning" \ "latest").headOption.map(_.text)
    }

  private def versions(baseUrl: String, org: String, name: String): List[String] =
    metadataXml(baseUrl, org, name) match {
      case Some(x) =>
        (x \\ "version").map(_.text).toList.sorted
      case None =>
        Nil
    }

  private def viewFile(baseUrl: String, org: String, name: String, version: String, path: String) = {
    cache.apply(buildURL(baseUrl, org, name, version)).get(path) match {
      case Some(bytes) =>
        val str = new String(bytes, "UTF-8")
        val ext = path.split('.').lastOption.getOrElse("scala") // TODO
        val highlightjs = "http://cdnjs.cloudflare.com/ajax/libs/highlight.js/8.4/"
        Html5(<html>
          <head>
            <meta name="robots" content="noindex,nofollow" />
            <link rel="stylesheet" href={highlightjs + "styles/github.min.css"} />
            <script src={highlightjs + "highlight.min.js"}>;</script>
            <script src={highlightjs + "languages/scala.min.js"}>;</script>
            <script>hljs.initHighlightingOnLoad();</script>
          </head>
          <body>
            <pre><code class={ext} style="font-family: Consolas, Menlo, 'Liberation Mono', Courier, monospace;">{str}</code></pre>
          </body>
        </html>)
      case None =>
        ResponseString("not found")
    }
  }

  private def viewList(baseUrl: String, org: String, name: String, version: String) = defaultView(
    <ul>{
      cache.apply(buildURL(baseUrl, org, name, version)).map(_._1).toList.sorted.map {
        path =>
          val url = {
            val u = s"$JavaSrcURL$org/$name/$version/$path"
            if(baseUrl == sonatype) u
            else s"$u?$BASE=$baseUrl"
          }
          <li>
            <a target="_blank" href={url}>{path}</a>
          </li>
      }
    }</ul>
  )

  private def defaultView(x: Elem) = Html5(
    <html>
      <head>
        <meta name="robots" content="noindex,nofollow" />
      </head>
      <body><div>{x}</div></body>
    </html>
  )

  private def buildURL(baseUrl: String, org: String, name: String, version: String): String =
    s"$baseUrl/${org.replace('.', '/')}/$name/$version/$name-$version-sources.jar"

  private[this] def toByteArray(in: InputStream): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    transfer(in, out)
    out.toByteArray
  }

  private[this] def transfer(in: InputStream, out: OutputStream): Unit = {
    val buffer = new Array[Byte](1024 * 32)
    @annotation.tailrec
    def read() {
      val byteCount = in.read(buffer)
      if (byteCount >= 0) {
        out.write(buffer, 0, byteCount)
        read()
      }
    }
    read()
  }

  def searchByGroupId(groupId: String): httpz.Error \/ List[String] = {
    import httpz._
    import httpz.native._

    val req = Request(
      url = "http://search.maven.org/solrsearch/select",
      params = Map(
        "q" -> s"g:$groupId",
        "rows" -> "256",
        "wt" -> "json"
      )
    )

    Core.json[MavenSearch](req).interpret.map(
      _.response.docs.filter(_.hasSourcesJar).map(_.artifactId).sorted
    )
  }
}

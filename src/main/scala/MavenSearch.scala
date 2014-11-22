package java_src

import argonaut.CodecJson
import httpz.JsonToString

final case class MavenSearch(response: Response) extends JsonToString[MavenSearch]

object MavenSearch{

  implicit val codecJson: CodecJson[MavenSearch] =
    CodecJson.casecodec1(apply, unapply)(
      "response"
    )

}


final case class Doc(
  artifactId: String,
  text: List[String]
) extends JsonToString[Doc]{
  def hasSourcesJar: Boolean = text.contains("-sources.jar")
}

object Doc {

  implicit val codecJson: CodecJson[Doc] =
    CodecJson.casecodec2(apply, unapply)(
      "a",
      "text"
    )

}

final case class Response(
  docs: List[Doc]
) extends JsonToString[Response]

object Response {

  implicit val responseCodecJson: CodecJson[Response] =
    CodecJson.casecodec1(apply, unapply)(
      "docs"
    )

}

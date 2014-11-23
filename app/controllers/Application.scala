package controllers

import play.api.mvc._
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._ // JSON library
import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._ // Combinator syntax
import scalax.collection.Graph // or scalax.collection.mutable.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._
import scalax.collection.io.dot._
import scalax.collection.edge.LDiEdge,
  scalax.collection.edge.Implicits._
import scala.sys.process._
import java.io.ByteArrayInputStream

object Application extends Controller {
  case class Competency(id: String, parentID: Option[String], name: String, level: String)
  case class CompetencyTree(competencies: Map[String, Competency])

  implicit val competencyReads: Reads[Competency] = (
    (JsPath \ "gsx$competence" \ "$t").read[String](minLength[String](2)) and
      (JsPath \ "gsx$parent" \ "$t").read[String].map(str => if (str.trim.isEmpty) None else Some(str.trim)) and
      (JsPath \ "gsx$nom" \ "$t").read[String] and
      (JsPath \ "gsx$niveau" \ "$t").read[String]
  )(Competency.apply _)

  implicit val competencyTreeReads: Reads[CompetencyTree] =
    (JsPath \ "feed" \ "entry").read[Seq[Competency]].map( seq => CompetencyTree(seq.map(c => (c.id -> c)).toMap) )

  val URL = "https://spreadsheets.google.com/feeds/list/1Sy54xBD9lMEVUU3-XppvIwAS2hFENxkrilMMATBt_K0/od6/public/values?alt=json"
  /**
   * The index page.
   */
  def index = Action.async { implicit req =>
    WS.url(URL).get().map { response =>
      val tree = response.json.as[CompetencyTree]
      val edges = tree.competencies.values.flatMap {
        case Competency(id, Some(parentID), _ , _) => Some(id ~> parentID)
        case _ => None
      }

      val root = DotRootGraph (
        directed = true,
        id       = None
      )

      def edgeTransformer(innerEdge: Graph[String,DiEdge]#EdgeT):
          Option[(DotGraph,DotEdgeStmt)] = {
        val edge = innerEdge.edge
        Some(root,
          DotEdgeStmt(edge.from.toString,
            edge.to.toString))
      }

      val colorMap = Map("A" -> "#339900 ", "B" -> "#cc9900 ", "C" -> "#ff0000 ")
      def nodeTransformer(innerNode: Graph[String,DiEdge]#NodeT):
          Option[(DotGraph,DotNodeStmt)] = {
        val competency = tree.competencies(innerNode.toString)
        Some((root, DotNodeStmt(innerNode.toString, Seq(DotAttr("style", "filled"), DotAttr("label", competency.name), DotAttr("fillcolor", colorMap(competency.level))))))
      }

      val graph = Graph(edges.toSeq:_*)
      val inputString = graph.toDot(root, edgeTransformer, cNodeTransformer = Some(nodeTransformer), iNodeTransformer = Some(nodeTransformer))
      println(inputString)
      val input = new ByteArrayInputStream(inputString.getBytes("UTF-8"))
      ("dot -Tpng:quartz" #< input) #> new java.io.File("public/images/test.png") !

      Ok(views.html.index(""))
    }
  }
}

package org.scalatra.example.atmosphere

import java.util.Date

import org.json4s.JsonDSL._
import org.json4s._
import org.scalatra._
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.scalatra.scalate.ScalateSupport

import scala.concurrent.ExecutionContext.Implicits.global

import org.eclipse.rdf4j.rio._
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager
import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.query.TupleQueryResult
import org.eclipse.rdf4j.OpenRDFException
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.query.BooleanQuery
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.rio.RDFFormat

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException

import org.scalatra.CorsSupport

case class MethodAndData(method:String,data:Map[String,Object])

class DashboardServlet extends ScalatraServlet 
  with ScalateSupport with JValueResult 
  with JacksonJsonSupport with SessionSupport 
  with AtmosphereSupport with CorsSupport
{
  val graphDB: GraphDBConnector = new GraphDBConnector
  val cxn = GraphDbConnection.getDbConnection()

  protected implicit val jsonFormats: Formats = DefaultFormats
  
  before()
  {
      contentType = formats("json")
  }

  options("/*"){
    response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"))
  }

  atmosphere("/connectToWebsocket")
  {
      new AtmosphereClient 
      {
        def receive: AtmoReceive = 
        {
          case Connected =>
            println("Client %s is connected" format uuid)
            broadcast(("author" -> "Someone") ~ ("message" -> "joined the room") ~ ("time" -> (new Date().getTime.toString )), Everyone)
          case Disconnected(ClientDisconnected, _) =>
            broadcast(("author" -> "Someone") ~ ("message" -> "has left the room") ~ ("time" -> (new Date().getTime.toString )), Everyone)
          case JsonMessage(json) => 
          {
            try
            { 
                val userInput = request.body
                println(userInput)
                val parsedResult = parse(userInput)
                val extractedResult = parsedResult.extract[MethodAndData]
                val method = extractedResult.method
                method match {
                    case "addLink" => {postAddLink(extractedResult.data); broadcast(userInput)}
                    case "moveNode" => {postMoveNode(extractedResult.data); broadcast(userInput)}
                    case "checkUserCredentials" => send(checkUserCredentials(extractedResult.data))
                    case "getUserGraph" => send(getUserGraph(extractedResult.data))
                    case "createNewUser" => send(createNewUser(extractedResult.data))
                    case "removeLink" => {postRemoveLink(extractedResult.data); broadcast(userInput)}
                    case "changeLink" => {postChangeLink(extractedResult.data); broadcast(userInput)}
                    case "removeNode" => {postRemoveNode(extractedResult.data); broadcast(userInput)}
                    case "changeNode" => {postChangeNode(extractedResult.data); broadcast(userInput)}
                    case "addNode" => {postAddNode(extractedResult.data); broadcast(userInput)}
                    case _ => send("Unexpected method name in JSON")
                }
            }
            catch
            {
                case e1: JsonParseException => send("Unable to parse JSON")
                case e2: MappingException => send("Unable to parse JSON")
                case e3: JsonMappingException => send("Did not receive any content in the request body")
            }
          }
        }
      }
  }

  def postAddLink(data: Map[String,Object])
  {
     try
     {
        val user = data("user").asInstanceOf[String]
        val origin = data("origin").asInstanceOf[String]
        val target = data("target").asInstanceOf[String]
        val label = data("label").asInstanceOf[String]
        var citation = ""
        if (data.contains("citation"))
        {
           citation = data("citation").asInstanceOf[String]
        }
        println("Received a postAddLink request from user " + user)
        val local_cxn = GraphDbConnection.getNewDbConnection()
        graphDB.postAddLink(user,origin,target,label,citation,local_cxn)
        local_cxn.close()
     }
     catch
     {
        case e: RuntimeException => e.printStackTrace
     }
  }

  def postMoveNode(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       val node = data("node").asInstanceOf[String]
       val xpos = data("xpos").asInstanceOf[String]
       val ypos = data("ypos").asInstanceOf[String]
       println("Received a postMoveNode request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postMoveNode(user,node,xpos,ypos,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: RuntimeException => e.printStackTrace
     }
  }

  def checkUserCredentials(data: Map[String,Object]): String =
  {
    try
    {
        val user = data("user").asInstanceOf[String]
        val pw = data("password").asInstanceOf[String]
        println("Received a checkUserCredentials request from user " + user)
        val res = graphDB.checkUserCredentials(user, pw, cxn)
        if (res == "Wrong Username")
        {
            val noContentMessage = "User \"" + user + "\" does not exist"
            return noContentMessage
        }
        else if (res == "Wrong Password")
        {
            val noContentMessage = "Password incorrect for user \"" + user + "\""
            return noContentMessage
        }
        else return res
     }
     catch
     {
        case e: RuntimeException => {e.printStackTrace; return e.toString}
     }
  }

  def getUserGraph(data: Map[String,Object]): String =
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       println("Received a getUserGraph request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       val graphres = graphDB.getUserGraphNoPassword(user, cxn)
       local_cxn.close()
       return graphres
     }
     catch
     {
        case e: RuntimeException => {e.printStackTrace; return e.toString}
     }
  }

  def createNewUser(data: Map[String,Object]): String =
  {
    try
     {
       val user = data("user").asInstanceOf[String]
       val pw = data("password").asInstanceOf[String]
       println("Received a createNewUser request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       val graphres = graphDB.createNewUser(user, pw, cxn)
       local_cxn.close()
       return graphres
     }
     catch
     {
        case e: RuntimeException => {e.printStackTrace; return e.toString}
     }
  }

  def postRemoveLink(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       val origin = data("origin").asInstanceOf[String]
       val target = data("target").asInstanceOf[String]
       val label = data("label").asInstanceOf[String]
       println("Received a postRemoveLink request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postRemoveLink(user,origin,target,label,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: RuntimeException => e.printStackTrace
     }
  }

  def postRemoveNode(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       val node = data("node").asInstanceOf[String]
       println("Received a postRemoveNode request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postRemoveNode(user,node,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: RuntimeException => e.printStackTrace
     }
  }

  def postChangeLink(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       val origin = data("origin").asInstanceOf[String]
       val target = data("target").asInstanceOf[String]
       val oldLabel = data("oldLabel").asInstanceOf[String]
       val newLabel = data("newLabel").asInstanceOf[String]
       var citation = ""
        if (data.contains("citation"))
        {
           citation = data("citation").asInstanceOf[String]
        }
       println("Received a postChangeLink request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postChangeLink(user,origin,target,oldLabel,newLabel,citation,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: RuntimeException => e.printStackTrace
     }
  }

  def postChangeNode(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       val oldNode = data("oldNode").asInstanceOf[String]
       val newNode = data("newNode").asInstanceOf[String]
       println("Received a postChangeNode request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postChangeNode(user,oldNode,newNode,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: RuntimeException => e.printStackTrace
     }
  }

  def postAddNode(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       val node = data("node").asInstanceOf[String]
       val xpos = data("xpos").asInstanceOf[String]
       val ypos = data("ypos").asInstanceOf[String]
       println("Received a postAddNode request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postAddNode(user,node,xpos,ypos,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: RuntimeException => e.printStackTrace
     }
  }

error {
    case t: Throwable => t.printStackTrace()
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path =>
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }
}
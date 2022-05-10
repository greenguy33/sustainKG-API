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
            //broadcast(("author" -> "Someone") ~ ("message" -> "joined the room") ~ ("time" -> (new Date().getTime.toString )), Everyone)
          case Disconnected(ClientDisconnected, _) =>
            println("Client %s is disconnected" format uuid)
            //broadcast(("author" -> "Someone") ~ ("message" -> "has left the room") ~ ("time" -> (new Date().getTime.toString )), Everyone)
          case JsonMessage(json) =>
          {
            try
            { 
                println(json)
                val extractedResult = json.extract[MethodAndData]
                val method = extractedResult.method
                method match {
                    case "addLink" => {postAddLink(extractedResult.data); broadcast(json)}
                    case "moveNode" => {postMoveNode(extractedResult.data); broadcast(json)}
                    case "getUserGraph" => send(getUserGraph(extractedResult.data))
                    case "createNewUser" => send(createNewUser(extractedResult.data))
                    case "removeLink" => {postRemoveLink(extractedResult.data); broadcast(json)}
                    case "changeLink" => {postChangeLink(extractedResult.data); broadcast(json)}
                    case "removeNode" => {postRemoveNode(extractedResult.data); broadcast(json)}
                    case "changeNode" => {postChangeNode(extractedResult.data); broadcast(json)}
                    case "addNode" => {postAddNode(extractedResult.data); broadcast(json)}
                    case "addVote" => {postAddVote(extractedResult.data); broadcast(json)}
                    case "checkUserCredentials" => {
                        val res = checkUserCredentials(extractedResult.data)
                        send(res(0))
                        if (res(0) == "Login Successful") broadcast("User " + res(1) + " logged into the room")
                    }
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
        assert (user != "", "User field is blank")
        val origin = data("origin").asInstanceOf[String]
        assert (origin != "", "Origin field is blank")
        val target = data("target").asInstanceOf[String]
        assert (target != "", "Target field is blank")
        val label = data("label").asInstanceOf[String]
        assert (label != "", "Label field is blank")
        var citation = ""
        if (data.contains("citation"))
        {
           citation = data("citation").asInstanceOf[String]
           assert (citation != "", "Citation field is blank")
        }
        println("Received a postAddLink request from user " + user)
        val local_cxn = GraphDbConnection.getNewDbConnection()
        graphDB.postAddLink(user,origin,target,label,citation,local_cxn)
        local_cxn.close()
     }
     catch
     {
        case e: Throwable => e.printStackTrace
     }
  }

  def postMoveNode(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       assert (user != "", "User field is blank")
       val node = data("node").asInstanceOf[String]
       assert (node != "", "Node field is blank")
       val xpos = data("xpos").asInstanceOf[String]
       assert (xpos != "", "Xpos field is blank")
       val ypos = data("ypos").asInstanceOf[String]
       assert (ypos != "", "Ypos field is blank")
       println("Received a postMoveNode request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postMoveNode(user,node,xpos,ypos,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: Throwable => e.printStackTrace
     }
  }

  def checkUserCredentials(data: Map[String,Object]): Array[String] =
  {
    try
    {
        val user = data("user").asInstanceOf[String]
        assert (user != "", "User field is blank")
        val pw = data("password").asInstanceOf[String]
        assert (pw != "", "Password field is blank")
        println("Received a checkUserCredentials request from user " + user)
        val res = graphDB.checkUserCredentials(user, pw, cxn)
        if (res == "Wrong Username") return Array("User \"" + user + "\" does not exist", user)
        else if (res == "Wrong Password") return Array("Password incorrect for user \"" + user + "\"")
        else return Array("Login Successful", user)
     }
     catch
     {
        case e: Throwable => {e.printStackTrace; return Array(e.toString)}
     }
  }

  def getUserGraph(data: Map[String,Object]): String =
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       assert (user != "", "User field is blank")
       println("Received a getUserGraph request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       val graphres = graphDB.getUserGraphNoPassword(user, cxn)
       local_cxn.close()
       return graphres
     }
     catch
     {
        case e: Throwable => {e.printStackTrace; return e.toString}
     }
  }

  def createNewUser(data: Map[String,Object]): String =
  {
    try
     {
       val user = data("user").asInstanceOf[String]
       assert (user != "", "User field is blank")
       val pw = data("password").asInstanceOf[String]
       assert (pw != "", "Password field is blank")
       println("Received a createNewUser request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       val graphres = graphDB.createNewUser(user, pw, cxn)
       local_cxn.close()
       return graphres
     }
     catch
     {
        case e: Throwable => {e.printStackTrace; return e.toString}
     }
  }

  def postRemoveLink(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       assert (user != "", "User field is blank")
       val origin = data("origin").asInstanceOf[String]
       assert (origin != "", "Origin field is blank")
       val target = data("target").asInstanceOf[String]
       assert (target != "", "Target field is blank")
       val label = data("label").asInstanceOf[String]
       assert (label != "", "Label field is blank")
       println("Received a postRemoveLink request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postRemoveLink(user,origin,target,label,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: Throwable => e.printStackTrace
     }
  }

  def postRemoveNode(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       assert (user != "", "User field is blank")
       val node = data("node").asInstanceOf[String]
       assert (node != "", "Node field is blank")
       println("Received a postRemoveNode request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postRemoveNode(user,node,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: Throwable => e.printStackTrace
     }
  }

  def postChangeLink(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       assert (user != "", "User field is blank")
       val origin = data("origin").asInstanceOf[String]
       assert (origin != "", "Origin field is blank")
       val target = data("target").asInstanceOf[String]
       assert (target != "", "Target field is blank")
       val oldLabel = data("oldLabel").asInstanceOf[String]
       assert (oldLabel != "", "oldLabel field is blank")
       val newLabel = data("newLabel").asInstanceOf[String]
       assert (newLabel != "", "newLabel field is blank")
       var citation = ""
        if (data.contains("citation"))
        {
           citation = data("citation").asInstanceOf[String]
           assert (citation != "", "Citation field is blank")
        }
       println("Received a postChangeLink request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postChangeLink(user,origin,target,oldLabel,newLabel,citation,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: Throwable => e.printStackTrace
     }
  }

  def postChangeNode(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       assert (user != "", "User field is blank")
       val oldNode = data("oldNode").asInstanceOf[String]
       assert (oldNode != "", "oldNode field is blank")
       val newNode = data("newNode").asInstanceOf[String]
       assert (newNode != "", "newNode field is blank")
       println("Received a postChangeNode request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postChangeNode(user,oldNode,newNode,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: Throwable => e.printStackTrace
     }
  }

  def postAddNode(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       assert (user != "", "User field is blank")
       val node = data("node").asInstanceOf[String]
       assert (node != "", "Node field is blank")
       val xpos = data("xpos").asInstanceOf[String]
       assert (xpos != "", "Xpos field is blank")
       val ypos = data("ypos").asInstanceOf[String]
       assert (ypos != "", "Ypos field is blank")
       println("Received a postAddNode request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postAddNode(user,node,xpos,ypos,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: Throwable => e.printStackTrace
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

  def postAddVote(data: Map[String,Object])
  {
     try
     {
       val user = data("user").asInstanceOf[String]
       assert (user != "", "User field is blank")
       val origin = data("origin").asInstanceOf[String]
       assert (origin != "", "Origin field is blank")
       val target = data("target").asInstanceOf[String]
       assert (target != "", "Target field is blank")
       val label = data("label").asInstanceOf[String]
       assert (label != "", "Label field is blank")
       val voteCount = data("voteCount").asInstanceOf[String]
       assert (voteCount != "", "voteCount field is blank")
       println("Received a postVote request from user " + user)
       val local_cxn = GraphDbConnection.getNewDbConnection()
       graphDB.postVote(user,origin,target,label,voteCount,local_cxn)
       local_cxn.close()
     }
     catch
     {
        case e: Throwable => e.printStackTrace
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
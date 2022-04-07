package graphdb.ics.uci.edu

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

case class LoginInfo(user:String,password:String)
case class UserName(user:String)
case class postMoveNodeInput(user:String,node:String,xpos:String,ypos:String)
case class postAddLinkInput (user:String,origin:String,target:String,label:String,citation:String)
case class postRemoveLinkInput(user:String,origin:String,target:String,label:String)
case class postChangeLinkInput(user:String,origin:String,target:String,oldLabel:String,newLabel:String)
case class postRemoveNodeInput(user:String,node:String)
case class postChangeNodeInput(user:String,oldNode:String,newNode:String)
case class postAddNodeInput(user:String,node:String,xpos:String,ypos:String)

class DashboardServlet extends ScalatraServlet 
  with ScalateSupport with JValueResult 
  with JacksonJsonSupport with SessionSupport 
  with AtmosphereSupport {

  implicit protected val jsonFormats: Formats = DefaultFormats

  val graphDB: GraphDBConnector = new GraphDBConnector
  val cxn = GraphDbConnection.getDbConnection()

  get("/") {
    contentType="text/html"
    ssp("/index")
  }

  // post("/updateCitation")
  // {
    
  // }

  post("/createNewUser")
  {
      println("Received a new user request")
      try 
      { 
          val userInput = request.body
          val extractedResult = parse(userInput).extract[LoginInfo]
          val userName = extractedResult.user
          val pw = extractedResult.password
          print(userInput)
          
          if (userName.size == 0) BadRequest(Map("message" -> "Unable to parse JSON"))
          else if (pw.size == 0) BadRequest(Map("message" -> "Unable to parse JSON"))
          else
          {
              try
              {
                  val res = graphDB.createNewUser(userName, pw, cxn)
                  if (res == "User exists")
                  {
                      val noContentMessage = "User \"" + userName + "\" already exists"
                      NoContent(Map("message" -> noContentMessage))
                  }
                  else Ok(Map("message" -> res))
              }
              catch
              {
                  case e: RuntimeException => 
                  {
                      e.printStackTrace()
                      InternalServerError(Map("message" -> e.toString()))
                  }
              }
          }
      } 
      catch 
      {
          case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e2: MappingException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e3: JsonMappingException => BadRequest(Map("message" -> "Did not receive any content in the request body"))
      }
  }

  post("/checkUserCredentials")
  {
      println("Received a login request")
      try 
      { 
          val userInput = request.body
          println("received: " + userInput)
          val extractedResult = parse(userInput).extract[LoginInfo]
          val userName = extractedResult.user
          val pw = extractedResult.password

          if (userName.size == 0) BadRequest(Map("message" -> "Unable to parse JSON"))
          else if (pw.size == 0) BadRequest(Map("message" -> "Unable to parse JSON"))
          else
          {
              try
              {
                  val res = graphDB.checkUserCredentials(userName, pw, cxn)
                  if (res == "Wrong Username")
                  {
                      val noContentMessage = "User \"" + userName + "\" does not exist"
                      NoContent(Map("message" -> noContentMessage))
                  }
                  else if (res == "Wrong Password")
                  {
                      val noContentMessage = "Password incorrect for user \"" + userName + "\""
                      NoContent(Map("message" -> noContentMessage))
                  }
                  else res
              }
              catch
              {
                  case e: RuntimeException => 
                  {
                      e.printStackTrace()
                      InternalServerError(Map("message" -> e.toString()))
                  }
              }
          }
      } 
      catch 
      {
          case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e2: MappingException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e3: JsonMappingException => BadRequest(Map("message" -> "Did not receive any content in the request body"))
      }
  }

  /* this route could accept input with or without a password, depending on if we are using UCI authentication.
  For now just using comments to remove the password requirement.*/
  post("/getUserGraph")
  {
      println("Received a post request")
      try 
      { 
          val userInput = request.body
          println("received: " + userInput)
          val extractedResult = parse(userInput).extract[UserName]
          val userName = extractedResult.user
          println("getting graph for user: " + userName)

          if (userName.size == 0) BadRequest(Map("message" -> "Unable to parse JSON"))
          //else if (pw.size == 0) BadRequest(Map("message" -> "Unable to parse JSON"))
          else
          {
              try
              {
                  //val res = graphDB.getUserGraph(userName, pw, cxn)
                  graphDB.getUserGraphNoPassword(userName, cxn)
                  /*if (res == "Wrong Username")
                  {
                      val noContentMessage = "User \"" + userName + "\" does not exist"
                      NoContent(Map("message" -> noContentMessage))
                  }
                  else if (res == "Wrong Password")
                  {
                      val noContentMessage = "Password incorrect for user \"" + userName + "\""
                      NoContent(Map("message" -> noContentMessage))
                  }
                  else res*/
              }
              catch
              {
                  case e: RuntimeException => 
                  {
                      e.printStackTrace()
                      InternalServerError(Map("message" -> e.toString()))
                  }
              }
          }
      } 
      catch 
      {
          case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e2: MappingException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e3: JsonMappingException => BadRequest(Map("message" -> "Did not receive any content in the request body"))
      }
  }

  atmosphere("/postAddLink")
  {
      new AtmosphereClient {
        def receive: AtmoReceive = {
          case JsonMessage(json) => 
          {
            try
            { 
                val userInput = request.body
                println(userInput)
                val parsedResult = parse(userInput)
                val extractedResult = parsedResult.extract[postAddLinkInput]
                println("Received a postAddLink request from user " + extractedResult.user)
                if (extractedResult.user.size == 0) send("Unable to parse username")
                else
                {
                    try
                    {
                        val local_cxn = GraphDbConnection.getNewDbConnection()
                        graphDB.postAddLink(extractedResult.user,extractedResult.origin,extractedResult.target,extractedResult.label,extractedResult.citation,local_cxn)
                        local_cxn.close()
                        broadcast(userInput)
                    }
                    catch
                    {
                        case e: RuntimeException => 
                        {
                            e.printStackTrace()
                            send(e.toString())
                        }
                    }
                }
            } 
            catch 
            {
                case e1: JsonParseException => send("Unable to parse JSON")
                case e2: MappingException => send("Unable to parse JSON")
                case e3: JsonMappingException => send("Did not receive any content in the request body")
            }
          }
      }}
  }

  atmosphere("/postRemoveLink")
  {
      new AtmosphereClient {
        def receive: AtmoReceive = {
          case JsonMessage(json) => 
          {
            try
            { 
                val userInput = request.body
                println(userInput)
                val parsedResult = parse(userInput)
                val extractedResult = parsedResult.extract[postRemoveLinkInput]
                println("Received a postRemoveLink request from user " + extractedResult.user)
                if (extractedResult.user.size == 0) send("Unable to parse username")
                else
                {
                    try
                    {
                        val local_cxn = GraphDbConnection.getNewDbConnection()
                        graphDB.postRemoveLink(extractedResult.user,extractedResult.origin,extractedResult.target,extractedResult.label,local_cxn)
                        local_cxn.close()
                        broadcast(userInput)
                    }
                    catch
                    {
                        case e: RuntimeException => 
                        {
                            e.printStackTrace()
                            send(e.toString())
                        }
                    }
                }
            } 
            catch 
            {
                case e1: JsonParseException => send("Unable to parse JSON")
                case e2: MappingException => send("Unable to parse JSON")
                case e3: JsonMappingException => send("Did not receive any content in the request body")
            }
          }
      }}
  }

  atmosphere("/postChangeLink")
  {
      new AtmosphereClient {
        def receive: AtmoReceive = {
          case JsonMessage(json) => 
          {
            try
            { 
                val userInput = request.body
                println(userInput)
                val parsedResult = parse(userInput)
                val extractedResult = parsedResult.extract[postChangeLinkInput]
                println("Received a postChangeLink request from user " + extractedResult.user)
                if (extractedResult.user.size == 0) send("Unable to parse username")
                else
                {
                    try
                    {
                        val local_cxn = GraphDbConnection.getNewDbConnection()
                        graphDB.postChangeLink(extractedResult.user,extractedResult.origin,extractedResult.target,extractedResult.oldLabel,extractedResult.newLabel,local_cxn)
                        local_cxn.close()
                        broadcast(userInput)
                    }
                    catch
                    {
                        case e: RuntimeException => 
                        {
                            e.printStackTrace()
                            send(e.toString())
                        }
                    }
                }
            } 
            catch 
            {
                case e1: JsonParseException => send("Unable to parse JSON")
                case e2: MappingException => send("Unable to parse JSON")
                case e3: JsonMappingException => send("Did not receive any content in the request body")
            }
          }
      }}
  }

  atmosphere("/postAddNode")
  {
      new AtmosphereClient {
        def receive: AtmoReceive = {
          case JsonMessage(json) => 
          {
            try
            { 
                val userInput = request.body
                println(userInput)
                val parsedResult = parse(userInput)
                val extractedResult = parsedResult.extract[postAddNodeInput]
                println("Received a postAddNode request from user " + extractedResult.user)
                if (extractedResult.user.size == 0) send("Unable to parse username")
                else
                {
                    try
                    {
                        val local_cxn = GraphDbConnection.getNewDbConnection()
                        graphDB.postAddNode(extractedResult.user,extractedResult.node,extractedResult.xpos,extractedResult.ypos,local_cxn)
                        local_cxn.close()
                        broadcast(userInput)
                    }
                    catch
                    {
                        case e: RuntimeException => 
                        {
                            e.printStackTrace()
                            send(e.toString())
                        }
                    }
                }
            } 
            catch 
            {
                case e1: JsonParseException => send("Unable to parse JSON")
                case e2: MappingException => send("Unable to parse JSON")
                case e3: JsonMappingException => send("Did not receive any content in the request body")
            }
          }
      }}
  }

  atmosphere("/postRemoveNode")
  {
      new AtmosphereClient {
        def receive: AtmoReceive = {
          case JsonMessage(json) => 
          {
            try
            { 
                val userInput = request.body
                println(userInput)
                val parsedResult = parse(userInput)
                val extractedResult = parsedResult.extract[postRemoveNodeInput]
                println("Received a postRemoveNode request from user " + extractedResult.user)
                if (extractedResult.user.size == 0) send("Unable to parse username")
                else
                {
                    try
                    {
                        val local_cxn = GraphDbConnection.getNewDbConnection()
                        graphDB.postRemoveNode(extractedResult.user,extractedResult.node,local_cxn)
                        local_cxn.close()
                        broadcast(userInput)
                    }
                    catch
                    {
                        case e: RuntimeException => 
                        {
                            e.printStackTrace()
                            send(e.toString())
                        }
                    }
                }
            } 
            catch 
            {
                case e1: JsonParseException => send("Unable to parse JSON")
                case e2: MappingException => send("Unable to parse JSON")
                case e3: JsonMappingException => send("Did not receive any content in the request body")
            }
          }
      }}
  }

  atmosphere("/postMoveNode")
  {
      new AtmosphereClient {
        def receive: AtmoReceive = {
          case JsonMessage(json) => 
          {
            try
            { 
                val userInput = request.body
                println(userInput)
                val parsedResult = parse(userInput)
                val extractedResult = parsedResult.extract[postMoveNodeInput]
                println("Received a postMoveNode request from user " + extractedResult.user)
                if (extractedResult.user.size == 0) send("Unable to parse username")
                else
                {
                    try
                    {
                        val local_cxn = GraphDbConnection.getNewDbConnection()
                        graphDB.postMoveNode(extractedResult.user,extractedResult.node,extractedResult.xpos,extractedResult.ypos,local_cxn)
                        local_cxn.close()
                        broadcast(userInput)
                    }
                    catch
                    {
                        case e: RuntimeException => 
                        {
                            e.printStackTrace()
                            send(e.toString())
                        }
                    }
                }
            } 
            catch 
            {
                case e1: JsonParseException => send("Unable to parse JSON")
                case e2: MappingException => send("Unable to parse JSON")
                case e3: JsonMappingException => send("Did not receive any content in the request body")
            }
          }
      }}
  }

  atmosphere("/postChangeNode")
  {
      new AtmosphereClient {
        def receive: AtmoReceive = {
          case JsonMessage(json) => 
          {
            try
            { 
                val userInput = request.body
                println(userInput)
                val parsedResult = parse(userInput)
                val extractedResult = parsedResult.extract[postChangeNodeInput]
                println("Received a postChangeNode request from user " + extractedResult.user)
                if (extractedResult.user.size == 0) send("Unable to parse username")
                else
                {
                    try
                    {
                        val local_cxn = GraphDbConnection.getNewDbConnection()
                        graphDB.postChangeNode(extractedResult.user,extractedResult.oldNode,extractedResult.newNode,local_cxn)
                        local_cxn.close()
                        broadcast(userInput)
                    }
                    catch
                    {
                        case e: RuntimeException => 
                        {
                            e.printStackTrace()
                            send(e.toString())
                        }
                    }
                }
            } 
            catch 
            {
                case e1: JsonParseException => send("Unable to parse JSON")
                case e2: MappingException => send("Unable to parse JSON")
                case e3: JsonMappingException => send("Did not receive any content in the request body")
            }
          }
      }}
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

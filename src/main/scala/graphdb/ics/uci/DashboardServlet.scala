package edu.upenn.turbo

import org.scalatra._
/*import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import org.json4s._
import org.json4s.JsonDSL._*/
import org.json4s.MappingException
import java.nio.file.NoSuchFileException

import java.nio.file.{Files, Paths}
import java.nio.file.attribute.BasicFileAttributes

// RDF4J imports
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

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import java.io.File
import java.util.ArrayList

import org.apache.solr.client.solrj._
import org.apache.solr.client.solrj.impl.HttpSolrClient

import org.neo4j.driver.exceptions.ServiceUnavailableException

case class UserName(user: String, password: String)
case class GraphInput(user: String, nodes: Array[Object], links: Array[Object])

class DashboardServlet extends ScalatraServlet with JacksonJsonSupport with DashboardProperties
{
  val graphDB: GraphDBConnector = new GraphDBConnector
  val cxn = GraphDbConnection.getDbConnection()
  val wpCxn = GraphDbConnection.getWpDbConnection()
  val logger = LoggerFactory.getLogger("turboAPIlogger")

  protected implicit val jsonFormats: Formats = DefaultFormats
  before()
  {
      contentType = formats("json")
  }

  post("/getUserGraph")
  {
      logger.info("Received a post request")
      try 
      { 
          val userInput = request.body
          logger.info("received: " + userInput)
          val extractedResult = parse(userInput).extract[UserName]
          val userName = extractedResult.user
          val pw = extractedResult.password
          logger.info("getting graph for user: " + userName)

          if (userName.size == 0) BadRequest(Map("message" -> "Unable to parse JSON"))
          else if (pw.size == 0) BadRequest(Map("message" -> "Unable to parse JSON"))
          else
          {
              try
              {
                  val res = graphDB.getUserGraph(userName, pw, cxn)
                  if (res == "Login failed")
                  {
                      val noContentMessage = "Login failed for user \"" + userName + "\""
                      NoContent(Map("message" -> noContentMessage))
                  }
                  else if (res.size == 0)
                  {
                      val noContentMessage = "No graph present for \"" + userName + "\""
                      NoContent(Map("message" -> noContentMessage))
                  }
                  else res
              }
              catch
              {
                  case e: RuntimeException => 
                  {
                      println(e.toString)
                      InternalServerError(Map("message" -> "There was a problem retrieving results from the triplestore."))
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

  post("/postUserGraph")
  {
      logger.info("Received a post request")
      try 
      { 
          val userInput = request.body
          val parsedResult = parse(userInput)
          val extractedResult = parsedResult.extract[GraphInput]
          val userName = extractedResult.user
          val nodes = extractedResult.nodes
          val links = extractedResult.links
          logger.info("posting graph for user: " + userName)

          if (userName.size == 0) BadRequest(Map("message" -> "Unable to parse JSON"))
          else
          {
              try
              {
                  graphDB.postUserGraph(userName, nodes, links, cxn)
              }
              catch
              {
                  case e: RuntimeException => 
                  {
                      println(e.toString)
                      InternalServerError(Map("message" -> "There was a problem posting results to the triplestore."))
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

  post("/createNewUser")
  {
      logger.info("Received a new user request")
      try 
      { 
          val userInput = request.body
          val extractedResult = parse(userInput).extract[UserName]
          val userName = extractedResult.user
          val pw = extractedResult.password

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
                      println(e.toString)
                      InternalServerError(Map("message" -> "There was a problem retrieving results from the triplestore."))
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

  get("/getAllWikipediaArticles")
  {
      logger.info("Received a get request")
      try
      {
          val res = graphDB.getAllWikipediaArticles(wpCxn)
          res
      }
      catch
      {
          case e: RuntimeException => 
          {
              println(e.toString)
              InternalServerError(Map("message" -> "There was a problem retrieving results from the triplestore."))
          }
      }
  }
}
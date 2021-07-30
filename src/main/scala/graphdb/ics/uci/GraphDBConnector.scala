package edu.upenn.turbo

import java.io.File
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.query.TupleQueryResult
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.query.BooleanQuery
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.OpenRDFException
import org.eclipse.rdf4j.model.Statement
import scala.collection.mutable.ArrayBuffer

import scala.collection.mutable.HashMap
import org.eclipse.rdf4j.query.QueryEvaluationException

import java.nio.file.Path
import java.nio.file.Paths
import org.slf4j.LoggerFactory

class GraphDBConnector 
{
    val logger = LoggerFactory.getLogger("turboAPIlogger")

    def getUserGraph(userName: String, password: String, cxn: RepositoryConnection): String =
    {
        val safeUser = userName.replace(" ","").replace("<","").replace(">","")
        if (checkUserCredentials(safeUser, password, cxn))
        {
            val query = s"select * where { graph <http://sustainkg.org/$safeUser> { ?s ?p ?o . }}"
            val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
            val results = new ArrayBuffer[ArrayBuffer[String]]
            while (tupleQueryResult.hasNext())
            {
                val bindingset: BindingSet = tupleQueryResult.next()
                val thisResult = ArrayBuffer(bindingset.getValue("s").toString, bindingset.getValue("p").toString, bindingset.getValue("o").toString)
                results += thisResult
            }
            sparqlResToJson(results, safeUser)
        }
        else "Login failed"
    }

    def postUserGraph(userName: String, nodes: Array[Object], links: Array[Object], cxn: RepositoryConnection)
    {
        val safeUser = userName.replace(" ","").replace("<","").replace(">","")
        deleteUserGraph(safeUser, cxn)
        val rdf = jsonToRdf(nodes, links)
        val userGraph = "http://sustainkg.org/" + safeUser
        val query = s"insert data { graph <$userGraph> { $rdf }}"
        cxn.begin()
        val tupleUpdate = cxn.prepareUpdate(QueryLanguage.SPARQL, query)
        tupleUpdate.execute()
        cxn.commit()
    }

    def deleteUserGraph(userName: String, cxn: RepositoryConnection)
    {
        val userGraph = "<http://sustainkg.org/" + userName + ">"
        val query = "CLEAR GRAPH " + userGraph
        cxn.begin()
        val tupleUpdate = cxn.prepareUpdate(QueryLanguage.SPARQL, query)
        tupleUpdate.execute()
        cxn.commit()
    }

    def checkUserCredentials(userName: String, password: String, cxn: RepositoryConnection): Boolean =
    {
        val userGraph = "<http://sustainkg.org/" + userName + ">"
        val checkUser = s"ASK {$userGraph <http://sustainkg.org/security> '$password' . }"
        val boolQueryResult: BooleanQuery = cxn.prepareBooleanQuery(QueryLanguage.SPARQL, checkUser)
        return boolQueryResult.evaluate()
    }

    def createNewUser(userName: String, password: String, cxn: RepositoryConnection): String =
    {
        val safeUser = userName.replace(" ","").replace("<","").replace(">","")
        val userGraph = "<http://sustainkg.org/" + safeUser + ">"
        val checkUser = s"ASK {$userGraph <http://sustainkg.org/security> ?pw . }"
        val boolQueryResult: BooleanQuery = cxn.prepareBooleanQuery(QueryLanguage.SPARQL, checkUser)
        if (boolQueryResult.evaluate()) return "User exists"
        else
        {
            val query = s"insert data { $userGraph <http://sustainkg.org/security> '$password' . }"
            cxn.begin()
            val tupleUpdate = cxn.prepareUpdate(QueryLanguage.SPARQL, query)
            tupleUpdate.execute()
            cxn.commit()
            "User created"
        }
    }

    def sparqlResToJson(res: ArrayBuffer[ArrayBuffer[String]], user: String): String =
    {
        var classString = ""
        var linkString = ""
        val classIdMap = new HashMap[String, Integer]
        var classCount = 0
        var linkCount = 0
        for (row <- res)
        {
            val classIndices = Array(0,2)
            for (i <- classIndices)
            {
                if (!classIdMap.contains(row(i)))
                {
                    classIdMap += row(i) -> classCount
                    val classSuffix = row(i).split("https://en.wikipedia.org/wiki/")(1)
                    classString += "{'type':'node','id':'"+classCount.toString+"','label':'Concept','properties':{'name':'"+classSuffix+"'}},\n"
                    classCount = classCount + 1
                }
            }
            val linkSuffix = row(1).split("http://sustainkg.org/")(1)
            val startId = classIdMap(row(0))
            val endId = classIdMap(row(2))
            linkString += "{'type':'link','id':'"+linkCount.toString+"','label':'"+linkSuffix+"','source':'"+startId+"', 'target':'"+endId+"','properties':{}},\n"
            linkCount = linkCount + 1
        }
        // remove last comma
        classString = classString.patch(classString.lastIndexOf(','), "", 1)
        linkString = linkString.patch(linkString.lastIndexOf(','), "", 1)
        s"""{
            'user':'$user',
            'nodes':[$classString],
            'links':[$linkString]
            }""".replaceAll("'","\"")
    }

    def jsonToRdf(nodes: Array[Object], links: Array[Object]): String =
    {
        var rdf = ""
        val classIdMap = new HashMap[String, String]
        for (map <- nodes)
        {
            val asMap = map.asInstanceOf[Map[String,Object]]
            val propMap = asMap("properties").asInstanceOf[Map[String,String]]
            classIdMap += asMap("id").asInstanceOf[String] -> propMap("name").asInstanceOf[String]
        }
        for (map <- links)
        {
            val asMap = map.asInstanceOf[Map[String,Object]]
            val linkLabel = asMap("label").asInstanceOf[String]
            val startNode = asMap("source").asInstanceOf[String]
            val endNode = asMap("target").asInstanceOf[String]
            rdf += "<https://en.wikipedia.org/wiki/"+classIdMap(startNode)+"> <http://sustainkg.org/"+linkLabel+"> <https://en.wikipedia.org/wiki/"+classIdMap(endNode)+"> . \n"
        }
        rdf
    }

    def getAllWikipediaArticles(cxn: RepositoryConnection): String =
    {
        val query = s"select * where { ?s ?p ?o . }"
        val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
        var results = """{
                          "articles": [""" 
        while (tupleQueryResult.hasNext())
        {
            val thisResult: String = tupleQueryResult.next().getValue("s").toString
            results += s"""{"article":"$thisResult"},\n"""
        }
        results = results.patch(results.lastIndexOf(','), "", 1)
        results += "\n]\n}"
        results
    }
}
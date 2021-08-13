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
        val safeUser = userName.replace(" ","_").replace("<","").replace(">","")
        val loginResult = checkUserCredentials(safeUser, password, cxn)
        if (loginResult  == "Login Successful")
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
        else loginResult
    }

    def getCollectiveGraph(cxn: RepositoryConnection): String =
    {
        val query = s"select ?s ?p ?o where { graph ?g { ?s ?p ?o . }}"
        val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
        val results = new ArrayBuffer[ArrayBuffer[String]]
        while (tupleQueryResult.hasNext())
        {
            val bindingset: BindingSet = tupleQueryResult.next()
            val thisResult = ArrayBuffer(bindingset.getValue("s").toString, bindingset.getValue("p").toString, bindingset.getValue("o").toString)
            results += thisResult
        }
        sparqlResToJson(results, "all_users")
    }

    def postUserGraph(userName: String, nodes: Array[Object], links: Array[Object], cxn: RepositoryConnection)
    {
        val safeUser = userName.replace(" ","_").replace("<","").replace(">","")
        deleteUserGraph(safeUser, cxn)
        val rdf = jsonToRdf(nodes, links)
        val userGraph = "http://sustainkg.org/" + safeUser
        val query = s"insert data { graph <$userGraph> { $rdf }}"
        //cxn.begin()
        val tupleUpdate = cxn.prepareUpdate(QueryLanguage.SPARQL, query)
        tupleUpdate.execute()
        //cxn.commit()
    }

    def deleteUserGraph(userName: String, cxn: RepositoryConnection)
    {
        val userGraph = "<http://sustainkg.org/" + userName + ">"
        val query = "CLEAR GRAPH " + userGraph
        //cxn.begin()
        val tupleUpdate = cxn.prepareUpdate(QueryLanguage.SPARQL, query)
        tupleUpdate.execute()
        //cxn.commit()
    }

    def checkUserCredentials(userName: String, password: String, cxn: RepositoryConnection): String =
    {
        val userGraph = "<http://sustainkg.org/" + userName + ">"
        val checkUser = s"ASK {$userGraph <http://sustainkg.org/security> ?pass . }"
        val boolQueryResult: BooleanQuery = cxn.prepareBooleanQuery(QueryLanguage.SPARQL, checkUser)
        if (!boolQueryResult.evaluate()) return "Wrong Username"
        else
        {
            val checkPassword = s"ASK {$userGraph <http://sustainkg.org/security> '$password' . }"
            val boolQueryResult2: BooleanQuery = cxn.prepareBooleanQuery(QueryLanguage.SPARQL, checkPassword)
            if (!boolQueryResult2.evaluate()) return "Wrong Password"
            else return "Login Successful"
        }
    }

    def createNewUser(userName: String, password: String, cxn: RepositoryConnection): String =
    {
        val safeUser = userName.replace(" ","_").replace("<","").replace(">","")
        val userGraph = "<http://sustainkg.org/" + safeUser + ">"
        val checkUser = s"ASK {$userGraph <http://sustainkg.org/security> ?pw . }"
        val boolQueryResult: BooleanQuery = cxn.prepareBooleanQuery(QueryLanguage.SPARQL, checkUser)
        if (boolQueryResult.evaluate()) return "User exists"
        else
        {
            val query = s"insert data { $userGraph <http://sustainkg.org/security> '$password' . }"
            //cxn.begin()
            val tupleUpdate = cxn.prepareUpdate(QueryLanguage.SPARQL, query)
            tupleUpdate.execute()
            //cxn.commit()
            "User created"
        }
    }

    def getAllConcepts(cxn: RepositoryConnection): String =
    {
        val query = """select distinct ?s where { 
                            { graph ?g {?s ?p ?o . }}
                            UNION
                            { graph ?g {?o ?p ?s .}}
                        }"""
        val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
        val results = new ArrayBuffer[String]
        while (tupleQueryResult.hasNext())
        {
            val bindingset: BindingSet = tupleQueryResult.next()
            val thisResult = bindingset.getValue("s").toString
            results += thisResult
        }
        var replaceString = ""
        var str = """{
                "nodes": [{replace}]
                }"""
        for (result <- results)
        {
            var tempRes = addIllegalCharacters(result.replaceAll("https://en.wikipedia.org/wiki/","")).replaceAll("_", " ")
            replaceString += "\"" + tempRes + "\","
        }
        // remove last comma
        replaceString = replaceString.patch(replaceString.lastIndexOf(','), "", 1)
        str.replaceAll("\\{replace\\}",replaceString)
    }

    def getAllNodeConnections(node: String, cxn: RepositoryConnection): String =
    {
        val formattedNode = "https://en.wikipedia.org/wiki/"+node.replaceAll(" ","_")
        val query = s"""select distinct ?s ?p ?o where
                        {
                            {
                                values ?s {<$formattedNode>}
                                graph ?g {?s ?p ?o}
                            }
                            UNION
                            {
                                values ?o {<$formattedNode>}
                                graph ?g {?s ?p ?o}
                            }
                        }"""
        val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
        val results = new ArrayBuffer[ArrayBuffer[String]]
        while (tupleQueryResult.hasNext())
        {
            val bindingset: BindingSet = tupleQueryResult.next()
            val thisResult = ArrayBuffer(bindingset.getValue("s").toString, bindingset.getValue("p").toString, bindingset.getValue("o").toString)
            results += thisResult
        }
        sparqlResToJson(results, "all_users")
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
                    val classSuffix = addIllegalCharacters(row(i).split("https://en.wikipedia.org/wiki/")(1)).replaceAll("_", " ")
                    classString += "{'type':'node','id':'"+classCount.toString+"','label':'Concept','properties':{'name':'"+classSuffix+"'}},\n"
                    classCount = classCount + 1
                }
            }
            val linkSuffix = row(1).split("http://sustainkg.org/")(1).replaceAll("_", " ")
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
            rdf += "<https://en.wikipedia.org/wiki/"+removeIllegalCharacters(classIdMap(startNode).replace(" ", "_")) +
                "> <http://sustainkg.org/"+linkLabel.replace(" ", "_") +
                "> <https://en.wikipedia.org/wiki/"+removeIllegalCharacters(classIdMap(endNode).replace(" ", "_"))+"> . \n"
        }
        rdf
    }

    def removeIllegalCharacters(replace: String): String =
    {
        var newString = replace.replaceAll("%","__percent__")
        newString = newString.replaceAll("\"","__double_quote__")
        newString = newString.replaceAll("\\^","__carrot__")
        newString = newString.replaceAll("\\\\","__backslash__")
        newString.replaceAll("`","__dash__")
    }

    def addIllegalCharacters(replace: String): String =
    {
        var newString = replace.replaceAll("__percent__","%")
        newString = newString.replaceAll("__double_quote__","\\\\\"")
        newString = newString.replaceAll("__carrot__","\\^")
        newString = newString.replaceAll("__backslash__","\\\\\\\\")
        newString.replaceAll("__dash__","`")
    }
}
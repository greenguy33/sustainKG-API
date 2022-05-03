package org.scalatra.example.atmosphere

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
import scala.io.Source
import java.util.UUID

class GraphDBConnector 
{
    val logger = LoggerFactory.getLogger("turboAPIlogger")
    val groupMap = getGroupMap()

    /*def getUserGraph(userName: String, password: String, cxn: RepositoryConnection): String =
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
    }*/

    def getUserGraphNoPassword(userName: String, cxn: RepositoryConnection): String =
    {
        var safeUser = userName.replace(" ","_").replace("<","").replace(">","")
        // map to group account
        if (groupMap.contains(safeUser)) safeUser = groupMap(safeUser)
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
        var safeUser = userName.replace(" ","_").replace("<","").replace(">","")
        // map to group account
        if (groupMap.contains(safeUser)) safeUser = groupMap(safeUser)
        cxn.begin()
        try
        {
            deleteUserGraph(safeUser, cxn)
            val rdf = jsonToRdf(nodes, links)
            val userGraph = "http://sustainkg.org/" + safeUser
            val query = s"insert data { graph <$userGraph> { $rdf }}"
            val tupleUpdate = cxn.prepareUpdate(QueryLanguage.SPARQL, query)
            tupleUpdate.execute()
            cxn.commit()
        }
        catch
        {
            case e: Throwable => 
            {
                cxn.rollback()
                throw new Throwable(e)
            }
        }
    }

    def postMoveNode(user:String,node:String,xpos:String,ypos:String,cxn: RepositoryConnection)
    {
        var safeUser = user.replace(" ","_").replace("<","").replace(">","")
        val safeNode = removeIllegalCharacters(node)
        // map to group account
        if (groupMap.contains(safeUser)) safeUser = groupMap(safeUser)
        cxn.begin()
        try
        {
            val query = 
                s"""DELETE{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeNode><http://sustainkg.org/x_coord>?xpos.
                            <https://en.wikipedia.org/wiki/$safeNode><http://sustainkg.org/y_coord>?ypos.
                        }}
                    INSERT{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeNode><http://sustainkg.org/x_coord><http://sustainkg.org/$xpos>.
                            <https://en.wikipedia.org/wiki/$safeNode><http://sustainkg.org/y_coord><http://sustainkg.org/$ypos>.
                        }}
                    WHERE{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeNode><http://sustainkg.org/x_coord>?xpos.
                            <https://en.wikipedia.org/wiki/$safeNode><http://sustainkg.org/y_coord>?ypos.
                        }}"""
            println(query)
            val tupleUpdate = cxn.prepareUpdate(QueryLanguage.SPARQL, query)
            tupleUpdate.execute()
            cxn.commit()
        }
        catch
        {
            case e: Throwable => 
            {
                cxn.rollback()
                throw new Throwable(e)
            }
        }
    }

    def postChangeNode(user:String,oldNode:String,newNode:String,cxn: RepositoryConnection)
    {
        var safeUser = user.replace(" ","_").replace("<","").replace(">","")
        val safeOldNode = removeIllegalCharacters(oldNode)
        val safeNewNode = removeIllegalCharacters(newNode)
        // map to group account
        if (groupMap.contains(safeUser)) safeUser = groupMap(safeUser)
        cxn.begin()
        try
        {
            val changeSubjects = 
                s"""DELETE{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeOldNode> ?p ?o.
                        }}
                    INSERT{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeNewNode> ?p ?o.
                        }}
                    WHERE{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeOldNode> ?p ?o.
                        }}"""
            val changeObjects = 
                s"""DELETE{GRAPH<http://sustainkg.org/$safeUser>{
                            ?s ?p <https://en.wikipedia.org/wiki/$safeOldNode>.
                        }}
                    INSERT{GRAPH<http://sustainkg.org/$safeUser>{
                            ?s ?p <https://en.wikipedia.org/wiki/$safeNewNode>.
                        }}
                    WHERE{GRAPH<http://sustainkg.org/$safeUser>{
                            ?s ?p <https://en.wikipedia.org/wiki/$safeOldNode>.
                        }}"""
            println(changeSubjects)
            println(changeObjects)
            cxn.prepareUpdate(QueryLanguage.SPARQL, changeSubjects).execute()
            cxn.prepareUpdate(QueryLanguage.SPARQL, changeObjects).execute()
            cxn.commit()
        }
        catch
        {
            case e: Throwable => 
            {
                cxn.rollback()
                throw new Throwable(e)
            }
        }
    }

    def postRemoveNode(user:String,node:String,cxn: RepositoryConnection)
    {
        // this currently leaves link residuals that need to be cleaned up.
        var safeUser = user.replace(" ","_").replace("<","").replace(">","")
        val safeNode = removeIllegalCharacters(node)
        // map to group account
        if (groupMap.contains(safeUser)) safeUser = groupMap(safeUser)
        cxn.begin()
        try
        {
            val removeSubjects = 
                s"""DELETE{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeNode> ?p ?o.
                            ?p ?p1 ?o2 .
                        }}
                    WHERE{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeNode> ?p ?o.
                            optional{?p ?p1 ?o2 .}
                        }}"""
            val removeObjects = 
                s"""DELETE{GRAPH<http://sustainkg.org/$safeUser>{
                            ?s ?p <https://en.wikipedia.org/wiki/$safeNode>.
                            ?p ?p1 ?o2 .
                        }}
                    WHERE{GRAPH<http://sustainkg.org/$safeUser>{
                            ?s ?p <https://en.wikipedia.org/wiki/$safeNode>.
                            optional{?p ?p1 ?o2 .}
                        }}"""
            println(removeSubjects)
            println(removeObjects)
            cxn.prepareUpdate(QueryLanguage.SPARQL, removeSubjects).execute()
            cxn.prepareUpdate(QueryLanguage.SPARQL, removeObjects).execute()
            cxn.commit()
        }
        catch
        {
            case e: Throwable => 
            {
                cxn.rollback()
                throw new Throwable(e)
            }
        }
    }

    def postRemoveLink(user:String,origin:String,target:String,label:String,cxn: RepositoryConnection)
    {
        var safeUser = user.replace(" ","_").replace("<","").replace(">","")
        val safeOrigin = removeIllegalCharacters(origin)
        val safeTarget = removeIllegalCharacters(target)
        val safeLabel = removeIllegalCharacters(label)
        // map to group account
        if (groupMap.contains(safeUser)) safeUser = groupMap(safeUser)
        cxn.begin()
        try
        {
            val removeLink = 
                s"""DELETE{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeOrigin> ?p <https://en.wikipedia.org/wiki/$safeTarget>.
                            ?p <http://sustainkg.org/label> <http://sustainkg.org/$safeLabel>.
                            ?p ?p1 ?o .
                        }}
                    WHERE{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeOrigin> ?p <https://en.wikipedia.org/wiki/$safeTarget>.
                            ?p <http://sustainkg.org/label> <http://sustainkg.org/$safeLabel>.
                            ?p ?p1 ?o .
                        }}"""
            println(removeLink)
            cxn.prepareUpdate(QueryLanguage.SPARQL, removeLink).execute()
            cxn.commit()
        }
        catch
        {
            case e: Throwable => 
            {
                cxn.rollback()
                throw new Throwable(e)
            }
        }
    }

    def postChangeLink(user:String,origin:String,target:String,oldLabel:String,newLabel:String,citation:String,cxn: RepositoryConnection)
    {
        var safeUser = user.replace(" ","_").replace("<","").replace(">","")
        val safeOrigin = removeIllegalCharacters(origin)
        val safeTarget = removeIllegalCharacters(target)
        val safeOldLabel = removeIllegalCharacters(oldLabel)
        val safeNewLabel = removeIllegalCharacters(newLabel)
        var citationRdf = ""
        if (citation != "")
        {
            citationRdf = s"<http://sustainkg.org/$safeNewLabel><http://sustainkg.org/citation><http://sustainkg.org/$citation>."
        }
        // map to group account
        if (groupMap.contains(safeUser)) safeUser = groupMap(safeUser)
        cxn.begin()
        try
        {
            val changeLink = 
                s"""DELETE{GRAPH<http://sustainkg.org/$safeUser>{
                            ?p <http://sustainkg.org/label> <http://sustainkg.org/$safeOldLabel>.
                        }}
                    INSERT{GRAPH<http://sustainkg.org/$safeUser>{
                            ?p <http://sustainkg.org/label> <http://sustainkg.org/$safeNewLabel>.
                            $citationRdf
                        }}
                    WHERE{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeOrigin> ?p <https://en.wikipedia.org/wiki/$safeTarget>.
                            ?p <http://sustainkg.org/label> <http://sustainkg.org/$safeOldLabel>.
                        }}"""
            println(changeLink)
            cxn.prepareUpdate(QueryLanguage.SPARQL, changeLink).execute()
            cxn.commit()
        }
        catch
        {
            case e: Throwable => 
            {
                cxn.rollback()
                throw new Throwable(e)
            }
        }
    }

    def postAddLink(user:String,origin:String,target:String,label:String,citation:String,cxn: RepositoryConnection)
    {
        var safeUser = user.replace(" ","_").replace("<","").replace(">","")
        val safeOrigin = removeIllegalCharacters(origin)
        val safeTarget = removeIllegalCharacters(target)
        val safeLabel = removeIllegalCharacters(label)
        val uniqueId = UUID.randomUUID().toString.replace("-","")
        // map to group account
        if (groupMap.contains(safeUser)) safeUser = groupMap(safeUser)
        var citationRdf = ""
        if (citation != "")
        {
            citationRdf = s"<http://sustainkg.org/$uniqueId><http://sustainkg.org/citation><http://sustainkg.org/$citation>."
        }
        cxn.begin()
        try
        {
            val addLink = 
                // needs to manage citation
                s"""INSERT DATA{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeOrigin><http://sustainkg.org/$uniqueId><https://en.wikipedia.org/wiki/$safeTarget>.
                            <http://sustainkg.org/$uniqueId><http://sustainkg.org/label><http://sustainkg.org/$safeLabel>.
                            $citationRdf
                        }}"""
            println(addLink)
            cxn.prepareUpdate(QueryLanguage.SPARQL, addLink).execute()
            cxn.commit()
        }
        catch
        {
            case e: Throwable => 
            {
                cxn.rollback()
                throw new Throwable(e)
            }
        }
    }

    def postAddNode(user:String,node:String,xpos:String,ypos:String,cxn: RepositoryConnection)
    {
        var safeUser = user.replace(" ","_").replace("<","").replace(">","")
        val safeNode = removeIllegalCharacters(node)
        // map to group account
        if (groupMap.contains(safeUser)) safeUser = groupMap(safeUser)
        cxn.begin()
        try
        {
            val addLink = 
                s"""INSERT DATA{GRAPH<http://sustainkg.org/$safeUser>{
                            <https://en.wikipedia.org/wiki/$safeNode><http://sustainkg.org/x_coord><http://sustainkg.org/$xpos>.
                            <https://en.wikipedia.org/wiki/$safeNode><http://sustainkg.org/y_coord><http://sustainkg.org/$ypos>.
                        }}"""
            println(addLink)
            cxn.prepareUpdate(QueryLanguage.SPARQL, addLink).execute()
            cxn.commit()
        }
        catch
        {
            case e: Throwable => 
            {
                cxn.rollback()
                throw new Throwable(e)
            }
        }
    }

    def deleteUserGraph(userName: String, cxn: RepositoryConnection)
    {
        val userGraph = "<http://sustainkg.org/" + userName + ">"
        val query = "CLEAR GRAPH " + userGraph
        val tupleUpdate = cxn.prepareUpdate(QueryLanguage.SPARQL, query)
        tupleUpdate.execute()
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
            val tupleUpdate = cxn.prepareUpdate(QueryLanguage.SPARQL, query)
            tupleUpdate.execute()
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
        for (result <- results)
        {
            var tempRes = addIllegalCharacters(result.replaceAll("https://en.wikipedia.org/wiki/","")).replaceAll("_", " ")
            replaceString += "\"" + tempRes + "\","
        }
        // remove last comma
        replaceString = replaceString.patch(replaceString.lastIndexOf(','), "", 1)
        """{"nodes": ["""+replaceString+"""]}"""
    }

    def getAllNodeConnections(node: String, cxn: RepositoryConnection): String =
    {
        val formattedNode = "https://en.wikipedia.org/wiki/"+removeIllegalCharacters(node).replaceAll(" ","_")
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

    def getGraphStatistics(cxn: RepositoryConnection): String =
    {
        val query = s"""select ?g (count(distinct ?s) as ?nc) ((count(?p)/2) as ?lc) where
                        {
                            graph ?g
                            {
                                {
                                    ?s ?p ?o
                                }
                                UNION
                                {
                                    ?o ?p ?s
                                }
                            }
                        }
                        group by ?g"""
        val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
        val results = new ArrayBuffer[ArrayBuffer[String]]
        while (tupleQueryResult.hasNext())
        {
            val bindingset: BindingSet = tupleQueryResult.next()
            val thisResult = ArrayBuffer(bindingset.getValue("g").toString, bindingset.getValue("nc").toString, bindingset.getValue("lc").toString)
            results += thisResult
        }
        var res = "["
        for (row <- results)
        {
            val name = row(0).replaceAll("http://sustainkg.org/","")
            val nc = row(1).replaceAll("\\^\\^<http://www.w3.org/2001/XMLSchema#integer>","")
            val lc = row(2).replaceAll("\\^\\^<http://www.w3.org/2001/XMLSchema#decimal>","")
            res += s"""{\"name\":\"$name\", \"nodes\":$nc, \"links\":$lc},"""
        }
        res.patch(res.lastIndexOf(','), "", 1) + "]"
    }

    def sparqlResToJson(res: ArrayBuffer[ArrayBuffer[String]], user: String): String =
    {
        var classString = ""
        var linkString = ""
        val classIdMap = new HashMap[String, Integer]
        val linkPropMap = new HashMap[String, HashMap[String, String]]
        val nodePropMap = new HashMap[String, HashMap[String, String]]
        var classCount = 0
        var linkCount = 0
        for (row <- res)
        {
            // if it's a link
            if (!row(0).startsWith("https://en.wikipedia.org/wiki/"))
            {
                if (linkPropMap.contains(row(0)))
                {
                    linkPropMap(row(0)) += row(1) -> row(2)
                }
                else
                {
                    linkPropMap += row(0) -> HashMap(row(1) -> row(2))
                }
            }
            // if it's a node
            else
            {
                if (row(1) == "http://sustainkg.org/x_coord" || row(1) == "http://sustainkg.org/y_coord")
                {
                    val keyval = addIllegalCharacters(row(0).replace("https://en.wikipedia.org/wiki/","")).replaceAll("_", " ")
                    if (nodePropMap.contains(keyval))
                    {
                        nodePropMap(keyval) += row(1) -> row(2).replace("http://sustainkg.org/","")
                    }
                    else
                    {
                        nodePropMap += keyval -> HashMap(row(1) -> row(2).replace("http://sustainkg.org/",""))
                    }
                }
            }
        }
        // for ((k,v) <- nodePropMap)
        // {
        //     print(k)
        //     for ((k1,v1) <- v)
        //     {
        //         print(k1 + " " + v1)
        //     }
        // }
        for (row <- res)
        {
            // process nodes
            if (row(0).startsWith("https://en.wikipedia.org/wiki/") && !row(2).startsWith("https://en.wikipedia.org/wiki/"))
            {
                if (!classIdMap.contains(row(0)))
                {
                    classIdMap += row(0) -> classCount
                    val classSuffix = addIllegalCharacters(row(0).replace("https://en.wikipedia.org/wiki/","")).replaceAll("_", " ")
                    classString += "{\"type\":\"node\",\"id\":\""+classCount.toString+"\",\"label\":\"Concept\",\"properties\":{\"name\":\""+classSuffix+"\"},\n" +
                                    "\"x\":\""+nodePropMap(classSuffix)("http://sustainkg.org/x_coord") + "\", \"y\":\""+nodePropMap(classSuffix)("http://sustainkg.org/y_coord") + "\"},"
                    classCount = classCount + 1
                }
            }
            // process links
            else if (row(0).startsWith("https://en.wikipedia.org/wiki/") && row(2).startsWith("https://en.wikipedia.org/wiki/"))
            {
                var citation = ""
                var linkSuffix = ""
                val linkProps = linkPropMap(row(1))
                var moreProps = ""
                for ((k,v) <- linkProps)
                {
                    if (k == "http://sustainkg.org/label")
                    {
                        linkSuffix = v.split("http://sustainkg.org/")(1).replaceAll("_", " ")
                    }
                    else
                    {
                        moreProps += "\""+k.split("http://sustainkg.org/")(1)+"\":\""+v.substring(21)+"\","
                    }
                }
                if (moreProps != "")
                {
                    moreProps = ", "+moreProps.patch(moreProps.lastIndexOf(','), "", 1)
                }
                assert (linkSuffix != "", "No link label found for link with index " + linkCount.toString)
                val startId = classIdMap(row(0))
                val endId = classIdMap(row(2))
                linkString += "{\"type\":\"link\",\"id\":\""+linkCount.toString+"\",\"label\":\""+linkSuffix+"\",\"source\":\""+startId+"\", \"target\":\""+endId+"\""+moreProps+"},\n"
                linkCount = linkCount + 1
            }
        }
        // remove last comma
        classString = classString.patch(classString.lastIndexOf(','), "", 1)
        linkString = linkString.patch(linkString.lastIndexOf(','), "", 1)
        s"""{
            "user":"$user",
            "nodes":[$classString],
            "links":[$linkString]
            }"""
    }

    def removeIllegalCharacters(replace: String): String =
    {
        var newString = replace.replaceAll("%","__percent__")
        newString = newString.replaceAll("\"","__double_quote__")
        newString = newString.replaceAll("\\^","__carrot__")
        newString = newString.replaceAll("\\\\","__backslash__")
        newString = newString.replaceAll(" ", "_")
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

    def getGroupMap(): HashMap[String, String] =
    {
        val map = new HashMap[String, String]
        val filename = "student_groups.csv"
        for (line <- Source.fromFile(filename).getLines) 
        {
            if (line != "")
            {
                val split = line.split(",")
                val netId = split(0)
                val group = split(1)
                map += netId -> group
            }
        }
        map
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
            val x_coord = asMap("x").asInstanceOf[String]
            val y_coord = asMap("y").asInstanceOf[String]
            rdf += "<https://en.wikipedia.org/wiki/"+removeIllegalCharacters(propMap("name").asInstanceOf[String].replace(" ", "_")) +
            "> <http://sustainkg.org/x_coord> <http://sustainkg.org/" + x_coord + "> . \n" +
            "<https://en.wikipedia.org/wiki/"+removeIllegalCharacters(propMap("name").asInstanceOf[String].replace(" ", "_")) +
            "> <http://sustainkg.org/y_coord> <http://sustainkg.org/" + y_coord + "> . \n"
        }
        for (map <- links)
        {
            val uniqueId = UUID.randomUUID().toString.replace("-","")
            val asMap = map.asInstanceOf[Map[String,Object]]
            val linkLabel = asMap("label").asInstanceOf[String]
            val startNode = asMap("source").asInstanceOf[String]
            val endNode = asMap("target").asInstanceOf[String]
            var citation = ""
            if (asMap.contains("citation"))
            {
                citation = asMap("citation").asInstanceOf[String]
            }
            rdf += "<https://en.wikipedia.org/wiki/"+removeIllegalCharacters(classIdMap(startNode).replace(" ", "_")) +
                "> <http://sustainkg.org/"+uniqueId +"> <https://en.wikipedia.org/wiki/"+removeIllegalCharacters(classIdMap(endNode).replace(" ", "_"))+"> . \n"+
                "<http://sustainkg.org/"+uniqueId+"> <http://sustainkg.org/label> <http://sustainkg.org/"+linkLabel.replace(" ", "_")+"> . \n"
            if (citation != "")
            {
                rdf += "<http://sustainkg.org/"+uniqueId+"> <http://sustainkg.org/citation> <http://sustainkg.org/"+citation+"> . \n"
            }
        }
        rdf
    }
}
package edu.upenn.turbo

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager

object GraphDbConnection
{
	var dbCxn: RepositoryConnection = null
    var dbRepo: Repository = null
    var dbRepoManager: RemoteRepositoryManager = null

	def setDbConnection(dbCxn: RepositoryConnection)
    {
        this.dbCxn = dbCxn
    }
    
    def getDbConnection(): RepositoryConnection = dbCxn
    
    def setDbRepo(dbRepo: Repository)
    {
        this.dbRepo = dbRepo
    }
    
    def getDbRepo(): Repository = dbRepo
    
    def setDbRepoManager(dbRepoManager: RemoteRepositoryManager)
    {
        this.dbRepoManager = dbRepoManager
    }
    
    def getDbRepoManager(): RemoteRepositoryManager = dbRepoManager
}
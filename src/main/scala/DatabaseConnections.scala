package org.scalatra.example.atmosphere

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

    def getNewDbConnection(): RepositoryConnection =
    {
        this.dbRepo.getConnection()
    }
    
    def setDbRepo(dbRepo: Repository)
    {
        this.dbRepo = dbRepo
    }
    
    def getDbRepo(): Repository = dbRepo
    
    def setDbRepoManager(dbRepoManager: RemoteRepositoryManager)
    {
        this.dbRepoManager = dbRepoManager
    }
    
    def getDbRepoManager(): RemoteRepositoryManager = wp_dbRepoManager

    var wp_dbCxn: RepositoryConnection = null
    var wp_dbRepo: Repository = null
    var wp_dbRepoManager: RemoteRepositoryManager = null

    def setWpDbConnection(wp_dbCxn: RepositoryConnection)
    {
        this.wp_dbCxn = wp_dbCxn
    }
    
    def getWpDbConnection(): RepositoryConnection = wp_dbCxn
    
    def setWpDbRepo(wp_dbRepo: Repository)
    {
        this.wp_dbRepo = wp_dbRepo
    }
    
    def getWpDbRepo(): Repository = wp_dbRepo
    
    def setWpDbRepoManager(wp_dbRepoManager: RemoteRepositoryManager)
    {
        this.wp_dbRepoManager = wp_dbRepoManager
    }
    
    def getWpDbRepoManager(): RemoteRepositoryManager = wp_dbRepoManager
}
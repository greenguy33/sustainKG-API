import edu.upenn.turbo._
import org.scalatra._
import javax.servlet.ServletContext

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import java.io.File

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager

class ScalatraBootstrap extends LifeCycle with DashboardProperties {

  override def destroy(context: ServletContext) 
  {
      val dbRm = GraphDbConnection.getDbRepoManager()
      val dbR = GraphDbConnection.getDbRepo()
      val dbCxn = GraphDbConnection.getDbConnection()

      dbCxn.close()
      dbR.shutDown()
      dbRm.shutDown()

      /*val wp_dbRm = GraphDbConnection.getWpDbRepoManager()
      val wp_dbR = GraphDbConnection.getWpDbRepo()
      val wp_dbCxn = GraphDbConnection.getWpDbConnection()

      wp_dbCxn.close()
      wp_dbR.shutDown()
      wp_dbRm.shutDown()*/
  }

  override def init(context: ServletContext) {

    println("connecting to graph db...")
    val serviceUrl = getFromProperties("serviceURL")
    println("serviceURL: "+ serviceUrl)
    val username = getFromProperties("username")
    println("username: " + username)
    val password = getFromProperties("password")
    val repoName = getFromProperties("repoName")
    println("repository: "+ repoName)
    val dbRepoManager = new RemoteRepositoryManager(serviceUrl)
    println("initialized repository manager")
    dbRepoManager.setUsernameAndPassword(username, password)
    println("set username and password")
    dbRepoManager.initialize()
    println("authentication successful")
    val dbRepo = dbRepoManager.getRepository(repoName)
    println("initialized repository")
    val dbCxn = dbRepo.getConnection()
    println("initializing connection")

    /*val wp_dbRepoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
    wp_dbRepoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
    wp_dbRepoManager.initialize()
    val wp_dbRepo = wp_dbRepoManager.getRepository(getFromProperties("wikiRepoName"))
    val wp_dbCxn = wp_dbRepo.getConnection()*/

    GraphDbConnection.setDbRepoManager(dbRepoManager)
    GraphDbConnection.setDbRepo(dbRepo)
    GraphDbConnection.setDbConnection(dbCxn)

    /*GraphDbConnection.setWpDbRepoManager(wp_dbRepoManager)
    GraphDbConnection.setWpDbRepo(wp_dbRepo)
    GraphDbConnection.setWpDbConnection(wp_dbCxn)*/

    println("established connection to repository "+ repoName)
    context.mount(new DashboardServlet, "/*")
  	println("""

                                                                                                                               
                                                                                                                               

 .----------------.  .----------------.  .----------------.  .----------------.  .----------------.                                         
| .--------------. || .--------------. || .--------------. || .--------------. || .--------------. |                                        
| |  ________    | || |      __      | || |    _______   | || |  ____  ____  | || |              | |                                        
| | |_   ___ `.  | || |     /  \     | || |   /  ___  |  | || | |_   ||   _| | || |              | |                                        
| |   | |   `. \ | || |    / /\ \    | || |  |  (__ \_|  | || |   | |__| |   | || |    ______    | |                                        
| |   | |    | | | || |   / ____ \   | || |   '.___`-.   | || |   |  __  |   | || |   |______|   | |                                        
| |  _| |___.' / | || | _/ /    \ \_ | || |  |`\____) |  | || |  _| |  | |_  | || |              | |                                        
| | |________.'  | || ||____|  |____|| || |  |_______.'  | || | |____||____| | || |              | |                                        
| |              | || |              | || |              | || |              | || |              | |                                        
| '--------------' || '--------------' || '--------------' || '--------------' || '--------------' |                                        
 '----------------'  '----------------'  '----------------'  '----------------'  '----------------'                                         
 .----------------.  .----------------.  .----------------.  .----------------.  .----------------.                                         
| .--------------. || .--------------. || .--------------. || .--------------. || .--------------. |                                        
| |   ______     | || |     ____     | || |      __      | || |  _______     | || |  ________    | |                                        
| |  |_   _ \    | || |   .'    `.   | || |     /  \     | || | |_   __ \    | || | |_   ___ `.  | |                                        
| |    | |_) |   | || |  /  .--.  \  | || |    / /\ \    | || |   | |__) |   | || |   | |   `. \ | |                                        
| |    |  __'.   | || |  | |    | |  | || |   / ____ \   | || |   |  __ /    | || |   | |    | | | |                                        
| |   _| |__) |  | || |  \  `--'  /  | || | _/ /    \ \_ | || |  _| |  \ \_  | || |  _| |___.' / | |                                        
| |  |_______/   | || |   `.____.'   | || ||____|  |____|| || | |____| |___| | || | |________.'  | |                                        
| |              | || |              | || |              | || |              | || |              | |                                        
| '--------------' || '--------------' || '--------------' || '--------------' || '--------------' |                                        
 '----------------'  '----------------'  '----------------'  '----------------'  '----------------'                                         
 .----------------.  .----------------.  .----------------.                                                                                 
| .--------------. || .--------------. || .--------------. |                                                                                
| |      __      | || |   ______     | || |     _____    | |                                                                                
| |     /  \     | || |  |_   __ \   | || |    |_   _|   | |                                                                                
| |    / /\ \    | || |    | |__) |  | || |      | |     | |                                                                                
| |   / ____ \   | || |    |  ___/   | || |      | |     | |                                                                                
| | _/ /    \ \_ | || |   _| |_      | || |     _| |_    | |                                                                                
| ||____|  |____|| || |  |_____|     | || |    |_____|   | |                                                                                
| |              | || |              | || |              | |                                                                                
| '--------------' || '--------------' || '--------------' |                                                                                
 '----------------'  '----------------'  '----------------'                                                                                 
 .----------------.  .----------------.  .----------------.  .----------------.  .----------------.  .----------------.                     
| .--------------. || .--------------. || .--------------. || .--------------. || .--------------. || .--------------. |                    
| |     ____     | || |              | || |     ____     | || |              | || |    ______    | || |              | |                    
| |   .'    '.   | || |              | || |   .'    '.   | || |              | || |   / ____ `.  | || |      _       | |                    
| |  |  .--.  |  | || |              | || |  |  .--.  |  | || |              | || |   `'  __) |  | || |     (_)      | |                    
| |  | |    | |  | || |              | || |  | |    | |  | || |              | || |   _  |__ '.  | || |      _       | |                    
| |  |  `--'  |  | || |      _       | || |  |  `--'  |  | || |      _       | || |  | \____) |  | || |     (_)      | |                    
| |   '.____.'   | || |     (_)      | || |   '.____.'   | || |     (_)      | || |   \______.'  | || |              | |                    
| |              | || |              | || |              | || |              | || |              | || |              | |                    
| '--------------' || '--------------' || '--------------' || '--------------' || '--------------' || '--------------' |                    
 '----------------'  '----------------'  '----------------'  '----------------'  '----------------'  '----------------'                     
 .----------------.  .----------------.  .----------------.  .----------------.                                                             
| .--------------. || .--------------. || .--------------. || .--------------. |                                                            
| |     ______   | || |      __      | || |   _____      | || |     _____    | |                                                            
| |   .' ___  |  | || |     /  \     | || |  |_   _|     | || |    |_   _|   | |                                                            
| |  / .'   \_|  | || |    / /\ \    | || |    | |       | || |      | |     | |                                                            
| |  | |         | || |   / ____ \   | || |    | |   _   | || |      | |     | |                                                            
| |  \ `.___.'\  | || | _/ /    \ \_ | || |   _| |__/ |  | || |     _| |_    | |                                                            
| |   `._____.'  | || ||____|  |____|| || |  |________|  | || |    |_____|   | |                                                            
| |              | || |              | || |              | || |              | |                                                            
| '--------------' || '--------------' || '--------------' || '--------------' |                                                            
 '----------------'  '----------------'  '----------------'  '----------------'                                                             
 .----------------.  .----------------.  .----------------.  .----------------.  .----------------.  .----------------.  .-----------------.
| .--------------. || .--------------. || .--------------. || .--------------. || .--------------. || .--------------. || .--------------. |
| |  _________   | || |  ________    | || |     _____    | || |  _________   | || |     _____    | || |     ____     | || | ____  _____  | |
| | |_   ___  |  | || | |_   ___ `.  | || |    |_   _|   | || | |  _   _  |  | || |    |_   _|   | || |   .'    `.   | || ||_   \|_   _| | |
| |   | |_  \_|  | || |   | |   `. \ | || |      | |     | || | |_/ | | \_|  | || |      | |     | || |  /  .--.  \  | || |  |   \ | |   | |
| |   |  _|  _   | || |   | |    | | | || |      | |     | || |     | |      | || |      | |     | || |  | |    | |  | || |  | |\ \| |   | |
| |  _| |___/ |  | || |  _| |___.' / | || |     _| |_    | || |    _| |_     | || |     _| |_    | || |  \  `--'  /  | || | _| |_\   |_  | |
| | |_________|  | || | |________.'  | || |    |_____|   | || |   |_____|    | || |    |_____|   | || |   `.____.'   | || ||_____|\____| | |
| |              | || |              | || |              | || |              | || |              | || |              | || |              | |
| '--------------' || '--------------' || '--------------' || '--------------' || '--------------' || '--------------' || '--------------' |
 '----------------'  '----------------'  '----------------'  '----------------'  '----------------'  '----------------'  '----------------'                                                                                                                    
                                                                                                                                                                     
""")
  }
}

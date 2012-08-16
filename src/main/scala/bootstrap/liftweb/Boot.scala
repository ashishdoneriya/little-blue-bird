package bootstrap.liftweb
import net.liftweb.db.StandardDBVendor
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Mailer._
import net.liftweb.mapper.MapperRules
import net.liftweb.util.Props
import com.lbb.util.Emailer
import net.liftweb.mapper.Schemifier
import com.lbb.util.LbbLogger
import net.liftweb.http.LiftRules
import net.liftweb.http.S
import net.liftweb.common.Box
import net.liftweb.mapper.DB
import com.lbb.entity.Circle
import com.lbb.entity.Gift
import net.liftweb.http.Req
import net.liftweb.db.DefaultConnectionIdentifier
import com.lbb.entity.CircleParticipant
import com.lbb.entity.Recipient
import com.lbb.RestService
import scala.xml.NodeSeq
import com.lbb.entity.User
import net.liftweb.common.Empty
import net.liftweb.common.Full
import net.liftweb.http.DocType
import com.lbb.entity.Reminder

class Boot extends LbbLogger {
  def boot {
    // TODO need real db pool
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor = 
	new StandardDBVendor(Props.get("db.driver") openOr "com.mysql.jdbc.Driver", //"org.h2.Driver", //"com.mysql.jdbc.Driver",
			     Props.get("db.url") openOr 
			     "jdbc:mysql://localhost:3307/bdunklau", //"jdbc:h2:~/test", //"jdbc:mysql://localhost:3306/littlebluebird",
			     Box(Props.get("db.user") openOr "test"), Box(Props.get("db.pass") openOr "test"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
      // see https://groups.google.com/forum/?fromgroups#!topic/liftweb/mIeiFDNFoDE
      // see http://comments.gmane.org/gmane.comp.web.lift/50754
      S.addAround(DB.buildLoanWrapper())
    }
    
    // to make the container serve a collection of files...
    LiftRules.passNotFoundToChain = true
    
    // to make the container serve a collection of files...
    LiftRules.liftRequest.append{
        case Req("app" :: _, _, _) => false
     } 
    
  
    // set DocType to HTML5
    LiftRules.docType.default.set((r: Req) => r match {
      case _ if S.skipDocType => Empty
      case _ if S.getDocType._1 => S.getDocType._2
      case _ => Full(DocType.html5)
    })
    
    MapperRules.formatFormElement = (name:NodeSeq, form:NodeSeq) =>
      <xml:group><div>{form}</div></xml:group> 
        
    Emailer.config

    // Use Lift's Mapper ORM to populate the database
    // you don't need to use Mapper to use Lift... use
    // any ORM you want
    Schemifier.schemify(true, Schemifier.infoF _, User)
    Schemifier.schemify(true, Schemifier.infoF _, Circle)
    Schemifier.schemify(true, Schemifier.infoF _, CircleParticipant)
    Schemifier.schemify(true, Schemifier.infoF _, Gift)
    Schemifier.schemify(true, Schemifier.infoF _, Recipient)
    Schemifier.schemify(true, Schemifier.infoF _, Reminder)
    
    // Read the reminder table and create executors for all the reminders
    Reminder.boot
    
    LiftRules.dispatch.append(RestService)
    
    Props.mode match {
      case Props.RunModes.Test => debug("we are in Test mode")
      case Props.RunModes.Development => debug("we are in Development mode")
      case Props.RunModes.Pilot => debug("we are in Pilot mode")
      case Props.RunModes.Production => debug("we are in Production mode")
      case Props.RunModes.Staging => debug("we are in Staging mode")
      case _ => debug("unknown run mode: "+Props.mode)                                        
    }
  }
}
package com.lbb
import java.util.Date
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import com.lbb.entity.Circle
import com.lbb.entity.Reminder
import com.lbb.entity.User
import com.lbb.util.ReminderUtil
import net.liftweb.common.Box
import net.liftweb.db.DefaultConnectionIdentifier
import net.liftweb.db.StandardDBVendor
import net.liftweb.mapper.DB
import net.liftweb.mapper.Schemifier
import net.liftweb.util.Props
import net.liftweb.common.Full
import com.lbb.entity.CircleParticipant
import net.liftweb.mapper.By

@RunWith(classOf[JUnitRunner])
class ReminderUtilTest extends FunSuite with AssertionsForJUnit {

  def initDb = {
    // this stuff goes in Boot.scala
    val vendor = 
	new StandardDBVendor(Props.get("db.driver") openOr "com.mysql.jdbc.Driver",
			     Props.get("db.url") openOr 
			     "jdbc:mysql://localhost:3307/bdunklau", //"jdbc:h2:~/test", //"jdbc:mysql://localhost:3306/littlebluebird",
			     Box(Props.get("db.user") openOr "test"), Box(Props.get("db.pass") openOr "test"))

    DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    
    Schemifier.schemify(true, Schemifier.infoF _, Circle)
  }

  test("determine remind date") {
    val daysprior = 7
    
    val from = new DateTime(2012,12,25,0,0,0,0)
    val act = ReminderUtil.calc(from, daysprior)
    val exp = new DateTime(2012,12,18,0,0,0,0)
    assert(act === exp)
  }
  
  test("create reminders") {
    initDb
        
    Reminder.findAll.foreach(_.delete_!)
    assert(Reminder.findAll.size===0)
        
    Circle.findAll.foreach(_.delete_!)
    assert(Circle.findAll.size===0)
        
    User.findAll.foreach(_.delete_!)
    assert(User.findAll.size===0)
    
    val brent = UserTest.createBrent
    val tamie = UserTest.createTamie
    val kiera = UserTest.createKiera
    val truman = UserTest.createTruman
    val jett = UserTest.createJett
    val nextXmas = CircleTest.nextXmas.add(List(brent, tamie, kiera, truman, jett), brent)
    
    val d1 = new DateTime(2012,12,22,0,0,0,0)
    val d2 = new DateTime(2012,12,18,0,0,0,0)
    val d3 = new DateTime(2012,12,11,0,0,0,0)
    val d4 = new DateTime(2012,11,25,0,0,0,0)
    val dates = List(d1,d2,d3,d4)
    // order names alphabetically to compare with actuals more easily
    val exp = Map(brent -> dates, jett -> dates, kiera -> dates, tamie -> dates, truman -> dates)
    val exprem = for(kv <- exp) yield {
      for(date <- kv._2) yield {
        Reminder.create.viewer(kv._1).circle(nextXmas).remind_date(new Date(date.getMillis()))
      }
    }
    
    val expectedreminders = exprem.toList.flatten
    
    ////////////////////////////////////////////////////////////////////
    // THIS IS WHAT WE'RE TESTING
    val actual = ReminderUtil.createReminders(nextXmas)
    
    println("ReminderUtilTest: expectedreminders.size="+expectedreminders.size+"  actual.size="+actual.size)
    assert(expectedreminders.size === actual.size)
    
    for(ex <- expectedreminders) {
      //println("actual.contains("+ex+"): "+actual.contains(ex))
      assert(actual.contains(ex) === true)
    }
    
    // now save the reminders to the db and see if we can query for them
    //actual foreach {_.save} // reminders are created when the date is set and when participants are added.
    // We don't actually call Reminder.save anymore
    
    // make sure we can query by circle...
    val circlerems = nextXmas.reminders
    println("ReminderUtilTest: expectedreminders.size="+expectedreminders.size+"  circlerems.size="+circlerems.size)
    assert(expectedreminders.size === circlerems.size)
    circlerems foreach {
      cr => {
        //println("expectedreminders.contains("+cr+"): "+expectedreminders.contains(cr))
        assert(expectedreminders.contains(cr)===true)
      }
    }
    
    // make sure we can query by name...
    println("ReminderUtilTest: query by name")
    val brentrems = brent.reminders
    val expbrent = expectedreminders.filter(ex => ex.viewer.obj.map(_.first.is)=="Brent")
    assert(brentrems.size === expbrent.size)
    brentrems foreach {br => assert(expbrent.contains(br)===true)}
    
    // make sure we can query by name/circle...
    println("ReminderUtilTest: query by name/circle")
    val brentxmasrems = Reminder.findByNameAndEvent(brent.id, nextXmas.id)
    assert(brentxmasrems.size === expbrent.size)
    brentxmasrems foreach {br => assert(expbrent.contains(br)===true)}
    
    // make sure we can query by name/circle/date...
    println("ReminderUtilTest: query by name/circle/date")
    val date1 = new Date(d1.getMillis)
    val single = Reminder.findByNameEventAndDate(brent.id, nextXmas.id, date1)
    assert(single.size===1)
    assert(single.head.viewer.obj.map(_.first.is) === "Brent")
    assert(single.head.circle.obj.map(_.name.is) === "Christmas 2012")
    assert(single.head.remind_date.is.getTime() === date1.getTime())
    
    
    
    ////////////////////////////////////////////////////////////////////
    // Now change the date of the event and make sure the reminders all got changed
    val newdate = new Date(new DateTime(2012,12,24,0,0,0,0).getMillis)
    
    ////////////////////////////////////////////////////////////////////
    // THIS IS WHAT WE'RE TESTING
    nextXmas.date(newdate)
    
    val newreminders = nextXmas.reminders
    
    // new expected reminders...
    val newd1 = new DateTime(2012,12,21,0,0,0,0)
    val newd2 = new DateTime(2012,12,17,0,0,0,0)
    val newd3 = new DateTime(2012,12,10,0,0,0,0)
    val newd4 = new DateTime(2012,11,24,0,0,0,0)
    val newdates = List(newd1,newd2,newd3,newd4)
    // order names alphabetically to compare with actuals more easily
    val newexp = Map(brent -> newdates, jett -> newdates, kiera -> newdates, tamie -> newdates, truman -> newdates)
    val newexprem = for(kv <- newexp) yield {
      for(date <- kv._2) yield {
        Reminder.create.viewer(kv._1).circle(nextXmas).remind_date(new Date(date.getMillis()))
      }
    }
    val newexpectedreminders = newexprem.toList.flatten
    println("ReminderUtilTest:  newexpectedreminders.size = "+newexpectedreminders.size)
    println("ReminderUtilTest:  newreminders.size = "+newreminders.size)
    
    assert(newexpectedreminders.size === newreminders.size)
    
    newexpectedreminders.foreach(r => println("newexpectedreminder: "+r))
    
    newreminders.foreach(r => {
      println("newexpectedreminders.contains(r): "+newexpectedreminders.contains(r)+" where r = "+r)
      assert(newexpectedreminders.contains(r))
    })
  }	
  
  /**
   * We're going to start off with a circle that has some people in it.
   * Then we're going to add Bill and make sure that reminders are created
   * for Bill
   */
  test("add participant - check reminders") {
    initDb
        
    Reminder.findAll.foreach(_.delete_!)
    assert(Reminder.findAll.size===0)
        
    Circle.findAll.foreach(_.delete_!)
    assert(Circle.findAll.size===0)
        
    User.findAll.foreach(_.delete_!)
    assert(User.findAll.size===0)
    
    val bill = UserTest.createBill
    val brent = UserTest.createBrent
    val tamie = UserTest.createTamie
    val kiera = UserTest.createKiera
    val truman = UserTest.createTruman
    val jett = UserTest.createJett
    // start off with these participants
    val nextXmas = CircleTest.nextXmas.add(List(brent, tamie, kiera, truman, jett), brent)
    
    val d1 = new DateTime(2012,12,22,0,0,0,0)
    val d2 = new DateTime(2012,12,18,0,0,0,0)
    val d3 = new DateTime(2012,12,11,0,0,0,0)
    val d4 = new DateTime(2012,11,25,0,0,0,0)
    val dates = List(d1,d2,d3,d4)
    // order names alphabetically to compare with actuals more easily
    val exp = Map(bill -> dates, brent -> dates, jett -> dates, kiera -> dates, tamie -> dates, truman -> dates)
    val exprem = for(kv <- exp) yield {
      for(date <- kv._2) yield {
        Reminder.create.viewer(kv._1).circle(nextXmas).remind_date(new Date(date.getMillis()))
      }
    }
    
    val expectedreminders = exprem.toList.flatten
    
    // Now add Bill the same way we do in RestService.insertParticipant
    val cp = CircleParticipant.create.circle(nextXmas).person(bill).inviter(brent).participationLevel("Receiver");
    
    ////////////////////////////////////////////////////////////////////
    // THIS IS WHAT WE'RE TESTING
    cp.save // this call should generate reminders for bill
    
    val actualreminders = nextXmas.reminders

    assert(expectedreminders.size === actualreminders.size)
    
    actualreminders.foreach(a => assert(expectedreminders.contains(a) === true))
    
  }
  
  /**
   * Start with a typical event and remove a participant
   * Make sure the reminders for that person have been deleted
   */
  test("remove participant - check reminders") {
    initDb
        
    Reminder.findAll.foreach(_.delete_!)
    assert(Reminder.findAll.size===0)
        
    Circle.findAll.foreach(_.delete_!)
    assert(Circle.findAll.size===0)
        
    User.findAll.foreach(_.delete_!)
    assert(User.findAll.size===0)
    
    val brent = UserTest.createBrent
    val tamie = UserTest.createTamie
    val kiera = UserTest.createKiera
    val truman = UserTest.createTruman
    val jett = UserTest.createJett
    // start off with these participants
    val nextXmas = CircleTest.nextXmas.add(List(brent, tamie, kiera, truman, jett), brent)
    
    val d1 = new DateTime(2012,12,22,0,0,0,0)
    val d2 = new DateTime(2012,12,18,0,0,0,0)
    val d3 = new DateTime(2012,12,11,0,0,0,0)
    val d4 = new DateTime(2012,11,25,0,0,0,0)
    val dates = List(d1,d2,d3,d4)
    // order names alphabetically to compare with actuals more easily
    val exp = Map(brent -> dates, jett -> dates, kiera -> dates, tamie -> dates, truman -> dates)
    val exprem = for(kv <- exp) yield {
      for(date <- kv._2) yield {
        Reminder.create.viewer(kv._1).circle(nextXmas).remind_date(new Date(date.getMillis()))
      }
    }
    
    val expectedreminders = exprem.toList.flatten
    val beforecount = expectedreminders.size
    val aftercount = beforecount - dates.size
    
    // Now let's remove a participant: brent
    val cps = CircleParticipant.findAll(By(CircleParticipant.circle, nextXmas.id), By(CircleParticipant.person, brent.id))
    
    ////////////////////////////////////////////////////////////////////
    // THIS IS WHAT WE'RE TESTING
    cps.foreach(_.delete_!)
    
//    make sure that ReminderUtil.scheduledExecutors has the right executors
//    removed from the map
    
    val actualreminders = nextXmas.reminders
    assert(actualreminders.size === aftercount)
    
    // make sure we still have the correct reminders
    actualreminders.foreach(a => assert(expectedreminders.contains(a)))
    
    // make sure the ones we expect to be gone are really gone
    val brentsreminders = expectedreminders.filter(rem => rem.viewer.obj.first.equals("Brent"))
    brentsreminders.foreach(b => {
      val same = actualreminders.contains(b)
      println("actualreminders.contains(b)"+same+" => "+b)
      assert(!actualreminders.contains(b))
    })
  }
  
  test("reminder executor") {
    initDb
        
    Reminder.findAll.foreach(_.delete_!)
    assert(Reminder.findAll.size===0)
        
    Circle.findAll.foreach(_.delete_!)
    assert(Circle.findAll.size===0)
        
    User.findAll.foreach(_.delete_!)
    assert(User.findAll.size===0)
    
    val brent = UserTest.createBrent
    val nextXmas = CircleTest.nextXmas
    
    val reminder = Reminder.create.viewer(brent).circle(nextXmas).remind_date(new Date(new Date().getTime()+5000))
    
    val delay = 2
    val runnable = new Runnable {def run = println("runnable run")}
    ReminderUtil.schedule(delay, runnable)
    Thread.sleep(delay*1000 + 1000)
    
    // how do you test this?  there's no getters
    //val reminderExecutor = ReminderUtil.createReminderExecutor(reminder)

    
    
  }
  
  
}
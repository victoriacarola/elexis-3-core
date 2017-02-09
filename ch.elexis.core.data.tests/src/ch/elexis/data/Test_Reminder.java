package ch.elexis.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.events.ElexisEvent;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.model.issue.ProcessStatus;
import ch.elexis.core.model.issue.Visibility;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.JdbcLinkException;
import ch.rgw.tools.TimeTool;

public class Test_Reminder extends AbstractPersistentObjectTest {
	private static JdbcLink link;
	
	private static Anwender anwender;
	private static Patient patient;
	
	@BeforeClass
	public static void init(){
		link = initDB();
		
		User user = User.load("Administrator");
		anwender = new Anwender("Name", "Vorname", (String) null, "w");
		user.setAssignedContact(anwender);
		// set user and Mandant in system
		ElexisEventDispatcher.getInstance()
			.fire(new ElexisEvent(user, User.class, ElexisEvent.EVENT_SELECTED));
		Mandant m = new Mandant("Mandant", "Erwin", "26.07.1979", "m");
		patient = new Patient("Mia", "Krank", "22041982", "w");
		CoreHub.setMandant(m);
	}
	
	@AfterClass
	public static void tearDown(){
		try {
			if (link == null || !link.isAlive())
				return;
			link.exec("DROP ALL OBJECTS");
			link.disconnect();
		} catch (JdbcLinkException je) {
			// just tell what happened and resume
			// exception is allowed for tests which get rid of the connection on their own
			// for example testConnect(), ...
			je.printStackTrace();
		}
	}
	
	@Test
	public void testAddRemoveResponsible() throws InterruptedException{
		Reminder reminder = new Reminder(null, new TimeTool().toString(TimeTool.DATE_GER),
			Visibility.ALWAYS, "", "TestMessage");
		long lastUpdate = reminder.getLastUpdate();
		assertNotSame(0L, reminder.getLastUpdate());
		Thread.sleep(2);
		reminder.addResponsible(anwender);
		assertTrue(reminder.getLastUpdate() > lastUpdate);
		assertEquals(1, reminder.getResponsibles().size());
		lastUpdate = reminder.getLastUpdate();
		Thread.sleep(2);
		reminder.removeResponsible(anwender);
		assertTrue(reminder.getLastUpdate() > lastUpdate);
		assertEquals(0, reminder.getResponsibles().size());
		reminder.delete();
	}
	
	@Test
	public void testFindOpenRemindersResponsibleFor(){
		Reminder reminderClosed = new Reminder(null, new TimeTool().toString(TimeTool.DATE_GER),
			Visibility.ALWAYS, "", "TestMessage");
		reminderClosed.addResponsible(CoreHub.actUser);
		reminderClosed.set(Reminder.FLD_STATUS,
			Integer.toString(ProcessStatus.CLOSED.numericValue()));
		
		Reminder reminder = new Reminder(null, new TimeTool().toString(TimeTool.DATE_GER),
			Visibility.ALWAYS, "", "TestMessage");
		reminder.addResponsible(CoreHub.actUser);
		assertEquals(1,
			Reminder.findOpenRemindersResponsibleFor(CoreHub.actUser, false, null, false).size());
		assertEquals(1,
			Reminder.findOpenRemindersResponsibleFor(CoreHub.actUser, false, null, false).size());
		
		Reminder patientSpecificReminder = new Reminder(patient,
			new TimeTool().toString(TimeTool.DATE_GER), Visibility.ALWAYS, "", "TestMessage");
		patientSpecificReminder.addResponsible(CoreHub.actUser);
		assertEquals(2,
			Reminder.findOpenRemindersResponsibleFor(CoreHub.actUser, false, null, false).size());
		assertEquals(1,
			Reminder.findOpenRemindersResponsibleFor(null, false, patient, false).size());
		
		Reminder popupReminder = new Reminder(patient, new TimeTool().toString(TimeTool.DATE_GER),
			Visibility.POPUP_ON_PATIENT_SELECTION, "", "TestMessage");
		popupReminder.addResponsible(CoreHub.actUser);
		assertEquals(3,
			Reminder.findOpenRemindersResponsibleFor(CoreHub.actUser, false, null, false).size());
		assertEquals(1,
			Reminder.findOpenRemindersResponsibleFor(CoreHub.actUser, false, null, true).size());
		assertEquals(1,
			Reminder.findOpenRemindersResponsibleFor(CoreHub.actUser, false, patient, true).size());
		
		reminderClosed.delete();
		reminder.delete();
		patientSpecificReminder.delete();
		popupReminder.delete();
	}
}

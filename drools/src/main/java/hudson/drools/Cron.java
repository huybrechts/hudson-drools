package hudson.drools;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.PeriodicWork;
import hudson.triggers.TimerTrigger;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Runs every minute to check {@link TimerTrigger} and schedules build.
 */
@Extension
public class Cron extends PeriodicWork {
	private final Calendar cal = new GregorianCalendar();

	public long getRecurrencePeriod() {
		return MIN;
	}

	public void doRun() {
		while (new Date().getTime() - cal.getTimeInMillis() > 1000) {
			try {
				checkTriggers(cal);
			} catch (Throwable e) {
				e.printStackTrace();
			}

			cal.add(Calendar.MINUTE, 1);
		}
	}

	public static void checkTriggers(final Calendar cal) {
		Hudson inst = Hudson.getInstance();

		for (DroolsProject p : inst.getAllItems(DroolsProject.class)) {
			if (p.getTabs() != null) {
				if (p.getTabs().check(cal)) {
					p.scheduleBuild(new TimerTrigger.TimerTriggerCause());
				}
			}
		}
	}
}

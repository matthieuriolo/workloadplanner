# workloadplanner
Java tool for creating events based on vacancies and worktimes. The tool will create a new ics file for each matched event and output a new ics file which contains events based on your vacancies, preferred working hours and the date of the matched event.

This tool can be very useful if you need to calculate mandatory preperation time or afterwork hours for your university/classes.



## the config file

You need to provide in the config file a URL to an existing ics file. Define your vacancies in the worktime node. You can give to each vacancy a priority (smaller first). Define a set of Regex patterns which matches against the events given in the downloaded ics file.

```
<?xml version="1.0" encoding="UTF-8"?>
<calendar name="CALENDAR_NAME">
	<schedules>
		 <url>URL_TO_ICS</url>
		 <file>FILE_TO_ICS</file>
	</schedules>
	
	<vacancies>
		<!-- indexed by 1 = Sunday -->
		<time day="1" from="08:00" to="12:00" priority="1" />
		<time day="1" from="13:00" to="18:00" priority="1"/>


		<time day="2" from="19:00" to="22:00" priority="2" />
	</vacancies>


	<assignments>
		<assignment pattern="REGEX_PATTERN_FOR_EVENTS_IN_DOWNLOADED_ICS" travelhours="2">
			<task name="EVENTS_BEFORE" type="before" hours="6" />
			<task name="EVENTS_AFTER" type="after" hours="1" />
		</assignment>
	</assignments>
</calendar>

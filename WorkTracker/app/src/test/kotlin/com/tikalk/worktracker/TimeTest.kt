package com.tikalk.worktracker;

import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.split
import com.tikalk.worktracker.time.hourOfDay
import com.tikalk.worktracker.time.millis
import com.tikalk.worktracker.time.minute
import com.tikalk.worktracker.time.second
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Time tests.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TimeTest {
    @Test
    fun splitRecords() {
        val user = User("name")
        val record = TimeRecord(user, Project.EMPTY.copy(), ProjectTask.EMPTY.copy(), null, null)
        record.project.id = 1
        record.task.id = 1
        var splits: List<TimeRecord>

        splits = record.split()
        assertNotNull(splits)
        assertTrue(splits.isEmpty())

        val start = Calendar.getInstance()
        val finish = Calendar.getInstance()

        // No finish.
        record.start = start
        splits = record.split()
        assertNotNull(splits)
        assertEquals(0, splits.size)

        // Finish too soon.
        record.finish = finish
        splits = record.split()
        assertNotNull(splits)
        assertEquals(0, splits.size)

        // Finish 1 hours later.
        finish.add(Calendar.HOUR_OF_DAY, 1)
        record.finish = finish
        splits = record.split()
        assertNotNull(splits)
        assertEquals(1, splits.size)

        // Finish 1 day later.
        finish.add(Calendar.DAY_OF_MONTH, 1)
        record.finish = finish
        splits = record.split()
        assertNotNull(splits)
        assertEquals(2, splits.size)
        var rec1 = splits[0]
        assertNotNull(rec1)
        assertNotNull(rec1.start)
        assertNotNull(rec1.finish)
        var start1 = start
        var finish1 = start1.clone() as Calendar
        finish1.hourOfDay = 23
        finish1.minute = 59
        finish1.second = 59
        finish1.millis = 999
        assertEquals(start1, rec1.start)
        assertEquals(finish1, rec1.finish)
        var rec2 = splits[1]
        assertNotNull(rec2)
        assertNotNull(rec2.start)
        assertNotNull(rec2.finish)
        var start2 = start.clone() as Calendar
        start2.add(Calendar.DAY_OF_MONTH, 1)
        start2.hourOfDay = 0
        start2.minute = 0
        start2.second = 0
        start2.millis = 0
        var finish2 = finish
        assertEquals(start2, rec2.start)
        assertEquals(finish2, rec2.finish)

        // Finish 2 days later.
        finish.add(Calendar.DAY_OF_MONTH, 1)
        record.finish = finish
        splits = record.split()
        assertNotNull(splits)
        assertEquals(3, splits.size)
        rec1 = splits[0]
        assertNotNull(rec1)
        assertNotNull(rec1.start)
        assertNotNull(rec1.finish)
        start1 = record.start
        finish1 = start.clone() as Calendar
        finish1.hourOfDay = 23
        finish1.minute = 59
        finish1.second = 59
        finish1.millis = 999
        assertEquals(start1, rec1.start)
        assertEquals(finish1, rec1.finish)
        rec2 = splits[1]
        assertNotNull(rec2)
        assertNotNull(rec2.start)
        assertNotNull(rec2.finish)
        start2 = finish1.clone() as Calendar
        start2.add(Calendar.DAY_OF_MONTH, 1)
        start2.hourOfDay = 0
        start2.minute = 0
        start2.second = 0
        start2.millis = 0
        finish2 = start2.clone() as Calendar
        finish2.hourOfDay = 23
        finish2.minute = 59
        finish2.second = 59
        finish2.millis = 999
        assertEquals(start2, rec2.start)
        assertEquals(finish2, rec2.finish)
        var rec3 = splits[2]
        assertNotNull(rec3)
        assertNotNull(rec3.start)
        assertNotNull(rec3.finish)
        var start3 = finish2.clone() as Calendar
        start3.add(Calendar.DAY_OF_MONTH, 1)
        start3.hourOfDay = 0
        start3.minute = 0
        start3.second = 0
        start3.millis = 0
        var finish3 = record.finish
        assertEquals(start3, rec3.start)
        assertEquals(finish3, rec3.finish)
    }
}

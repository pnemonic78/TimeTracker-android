package com.tikalk.worktracker

import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.split
import com.tikalk.worktracker.time.copy
import com.tikalk.worktracker.time.hourOfDay
import com.tikalk.worktracker.time.millis
import com.tikalk.worktracker.time.setToEndOfDay
import com.tikalk.worktracker.time.setToStartOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Time tests.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TimeTest {
    @Test
    fun splitRecords() {
        val record = TimeRecord(TikalEntity.ID_NONE, Project.EMPTY.copy(), ProjectTask.EMPTY.copy(), null, null)
        record.project.id = 1
        record.project.name = "Project"
        record.task.id = 1
        record.task.name = "Task"
        var splits: List<TimeRecord>

        splits = record.split()
        assertNotNull(splits)
        assertTrue(splits.isEmpty())

        val start = Calendar.getInstance()
        start.hourOfDay = 12
        val finish = start.copy()

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
        var finish1 = start1.copy()
        finish1.setToEndOfDay()
        finish1.millis = 0
        assertEquals(start1, rec1.start)
        assertEquals(finish1, rec1.finish)
        var rec2 = splits[1]
        assertNotNull(rec2)
        assertNotNull(rec2.start)
        assertNotNull(rec2.finish)
        var start2 = start.copy()
        start2.add(Calendar.DAY_OF_MONTH, 1)
        start2.setToStartOfDay()
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
        start1 = record.start!!
        finish1 = start.copy()
        finish1.setToEndOfDay()
        finish1.millis = 0
        assertEquals(start1, rec1.start)
        assertEquals(finish1, rec1.finish)
        rec2 = splits[1]
        assertNotNull(rec2)
        assertNotNull(rec2.start)
        assertNotNull(rec2.finish)
        start2 = finish1.copy()
        start2.add(Calendar.DAY_OF_MONTH, 1)
        start2.setToStartOfDay()
        finish2 = start2.copy()
        finish2.setToEndOfDay()
        finish2.millis = 0
        assertEquals(start2, rec2.start)
        assertEquals(finish2, rec2.finish)
        val rec3 = splits[2]
        assertNotNull(rec3)
        assertNotNull(rec3.start)
        assertNotNull(rec3.finish)
        val start3 = finish2.copy()
        start3.add(Calendar.DAY_OF_MONTH, 1)
        start3.setToStartOfDay()
        val finish3 = record.finish
        assertEquals(start3, rec3.start)
        assertEquals(finish3, rec3.finish)
    }
}

package com.tikalk.worktracker

import android.text.format.DateUtils
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.split
import com.tikalk.worktracker.time.copy
import com.tikalk.worktracker.time.dayOfMonth
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
        val record = TimeRecord.EMPTY.copy()
        record.project.id = 1
        record.project.name = "Project"
        record.task.id = 1
        record.task.name = "Task"
        record.date.dayOfMonth = 1

        var splits: List<TimeRecord>

        splits = record.split()
        assertNotNull(splits)
        assertTrue(splits.isEmpty())

        val start = Calendar.getInstance()
        start.hourOfDay = 12
        start.dayOfMonth = record.date.dayOfMonth
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
        var rec0 = splits[0]
        assertNotNull(rec0)
        assertNotNull(rec0.start)
        assertNotNull(rec0.finish)
        assertEquals(record.date.get(Calendar.DAY_OF_YEAR), rec0.date.get(Calendar.DAY_OF_YEAR))
        var start1 = start
        start1.millis = 0
        var finish1 = start1.copy()
        finish1.setToEndOfDay()
        finish1.millis = 0
        assertEquals(start1.timeInMillis, rec0.startTime)
        assertEquals(finish1.timeInMillis, rec0.finishTime)
        var rec1 = splits[1]
        assertNotNull(rec1)
        assertNotNull(rec1.start)
        assertNotNull(rec1.finish)
        assertEquals(record.date.get(Calendar.DAY_OF_YEAR) + 1, rec1.date.get(Calendar.DAY_OF_YEAR))
        var start2 = start.copy()
        start2.add(Calendar.DAY_OF_MONTH, 1)
        start2.setToStartOfDay()
        var finish2 = finish
        assertEquals(start2.timeInMillis, rec1.startTime)
        assertEquals(finish2.timeInMillis, rec1.finishTime)

        // Finish 2 days later.
        finish.add(Calendar.DAY_OF_MONTH, 1)
        record.finish = finish
        splits = record.split()
        assertNotNull(splits)
        assertEquals(3, splits.size)
        rec0 = splits[0]
        assertNotNull(rec0)
        assertNotNull(rec0.start)
        assertNotNull(rec0.finish)
        start1 = record.start!!
        finish1 = start.copy()
        finish1.setToEndOfDay()
        finish1.millis = 0
        assertEquals(start1.timeInMillis, rec0.startTime)
        assertEquals(finish1.timeInMillis, rec0.finishTime)
        assertEquals(record.date.get(Calendar.DAY_OF_YEAR), rec0.date.get(Calendar.DAY_OF_YEAR))
        rec1 = splits[1]
        assertNotNull(rec1)
        assertNotNull(rec1.start)
        assertNotNull(rec1.finish)
        start2 = finish1.copy()
        start2.add(Calendar.DAY_OF_MONTH, 1)
        start2.setToStartOfDay()
        finish2 = start2.copy()
        finish2.setToEndOfDay()
        finish2.millis = 0
        assertEquals(start2.timeInMillis, rec1.startTime)
        assertEquals(finish2.timeInMillis, rec1.finishTime)
        assertEquals(record.date.get(Calendar.DAY_OF_YEAR) + 1, rec1.date.get(Calendar.DAY_OF_YEAR))
        val rec2 = splits[2]
        assertNotNull(rec2)
        assertNotNull(rec2.start)
        assertNotNull(rec2.finish)
        val start3 = finish2.copy()
        start3.add(Calendar.DAY_OF_MONTH, 1)
        start3.setToStartOfDay()
        val finish3 = record.finish
        assertEquals(start3, rec2.start)
        assertEquals(finish3, rec2.finish)
        assertEquals(record.date.get(Calendar.DAY_OF_YEAR) + 2, rec2.date.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun seconds() {
        val record = TimeRecord.EMPTY.copy()
        val now1 = System.currentTimeMillis()
        val seconds1 = now1 / DateUtils.SECOND_IN_MILLIS
        val millis1 = seconds1 * DateUtils.SECOND_IN_MILLIS

        record.startTime = now1
        assertEquals(millis1, record.startTime)

        val now2 = System.currentTimeMillis()
        val seconds2 = now2 / DateUtils.SECOND_IN_MILLIS
        val millis2 = seconds2 * DateUtils.SECOND_IN_MILLIS

        record.finishTime = now2
        assertEquals(millis2, record.finishTime)

        val duration = now2 - now1
        val seconds3 = duration / DateUtils.SECOND_IN_MILLIS
        val millis3 = seconds3 * DateUtils.SECOND_IN_MILLIS
        assertEquals(millis3, record.finishTime - record.startTime)
    }
}

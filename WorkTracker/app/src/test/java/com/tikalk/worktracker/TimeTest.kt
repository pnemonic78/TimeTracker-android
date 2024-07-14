package com.tikalk.worktracker

import android.text.format.DateUtils
import com.tikalk.time.copy
import com.tikalk.time.dayOfMonth
import com.tikalk.time.dayOfYear
import com.tikalk.time.hourOfDay
import com.tikalk.time.millis
import com.tikalk.time.setToEndOfDay
import com.tikalk.time.setToStartOfDay
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.split
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Time tests.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TimeTest {

    private fun createRecord(): TimeRecord {
        return TimeRecord.EMPTY.copy().apply {
            project.id = 1
            project.name = PROJECT_NAME
            task.id = 1
            task.name = TASK_NAME
            date.dayOfMonth = 1
        }
    }

    @Test
    fun splitRecords_StartFinish_Empty() {
        val record = createRecord()

        val splits: List<TimeRecord> = record.split()
        assertNotNull(splits)
        assertTrue(splits.isEmpty())
    }

    /** No finish. */
    @Test
    fun splitRecords_StartFinish_None() {
        val record = createRecord()

        val start = Calendar.getInstance()
        start.hourOfDay = 12
        start.dayOfMonth = record.date.dayOfMonth
        record.start = start

        val splits = record.split()
        assertNotNull(splits)
        assertEquals(0, splits.size)
    }

    /** Finish too soon. */
    @Test
    fun splitRecords_StartFinish_Soon() {
        val record = createRecord()

        val start = Calendar.getInstance()
        start.hourOfDay = 12
        start.dayOfMonth = record.date.dayOfMonth
        record.start = start

        val finish = start.copy()
        record.finish = finish
        val splits = record.split()
        assertNotNull(splits)
        assertEquals(0, splits.size)
    }

    /** Finish 1 hour later. */
    @Test
    fun splitRecords_StartFinish_Hour() {
        val record = createRecord()

        val start = Calendar.getInstance()
        start.hourOfDay = 12
        start.dayOfMonth = record.date.dayOfMonth
        record.start = start

        val finish = start.copy()
        finish.add(Calendar.HOUR_OF_DAY, 1)
        record.finish = finish
        val splits = record.split()
        assertNotNull(splits)
        assertEquals(1, splits.size)
    }

    /** Finish 1 day later. */
    @Test
    fun splitRecords_StartFinish_Day() {
        val record = createRecord()

        val start = Calendar.getInstance()
        start.hourOfDay = 12
        start.dayOfMonth = record.date.dayOfMonth
        record.start = start

        val finish = start.copy()
        finish.add(Calendar.DAY_OF_MONTH, 1)
        record.finish = finish
        val splits = record.split()
        assertNotNull(splits)
        assertEquals(2, splits.size)
        val rec0 = splits[0]
        assertNotNull(rec0)
        assertNotNull(rec0.start)
        assertNotNull(rec0.finish)
        assertEquals(record.date.dayOfYear, rec0.date.dayOfYear)
        val start1 = start
        start1.millis = 0
        val finish1 = start1.copy()
        finish1.setToEndOfDay()
        finish1.millis = 0
        assertEquals(start1.timeInMillis, rec0.startTime)
        assertEquals(finish1.timeInMillis, rec0.finishTime)
        val rec1 = splits[1]
        assertNotNull(rec1)
        assertNotNull(rec1.start)
        assertNotNull(rec1.finish)
        assertEquals(record.date.dayOfYear + 1, rec1.date.dayOfYear)
        val start2 = start.copy()
        start2.add(Calendar.DAY_OF_MONTH, 1)
        start2.setToStartOfDay()
        val finish2 = finish
        assertEquals(start2.timeInMillis, rec1.startTime)
        assertEquals(finish2.timeInMillis, rec1.finishTime)
    }

    /** Finish 2 days later. */
    @Test
    fun splitRecords_StartFinish_Days() {
        val record = createRecord()

        val start = Calendar.getInstance()
        start.hourOfDay = 12
        start.dayOfMonth = record.date.dayOfMonth
        record.start = start

        val finish = start.copy()
        finish.add(Calendar.DAY_OF_MONTH, 2)
        record.finish = finish
        val splits = record.split()
        assertNotNull(splits)
        assertEquals(3, splits.size)
        val rec0 = splits[0]
        assertNotNull(rec0)
        assertNotNull(rec0.start)
        assertNotNull(rec0.finish)
        val start1 = record.start!!
        val finish1 = start.copy()
        finish1.setToEndOfDay()
        finish1.millis = 0
        assertEquals(start1.timeInMillis, rec0.startTime)
        assertEquals(finish1.timeInMillis, rec0.finishTime)
        assertEquals(record.date.dayOfYear, rec0.date.dayOfYear)
        val rec1 = splits[1]
        assertNotNull(rec1)
        assertNotNull(rec1.start)
        assertNotNull(rec1.finish)
        val start2 = finish1.copy()
        start2.add(Calendar.DAY_OF_MONTH, 1)
        start2.setToStartOfDay()
        val finish2 = start2.copy()
        finish2.setToEndOfDay()
        finish2.millis = 0
        assertEquals(start2.timeInMillis, rec1.startTime)
        assertEquals(finish2.timeInMillis, rec1.finishTime)
        assertEquals(record.date.dayOfYear + 1, rec1.date.dayOfYear)
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
        assertEquals(record.date.dayOfYear + 2, rec2.date.dayOfYear)
    }

    /** No duration. */
    @Test
    fun splitRecords_Duration_None() {
        val record = createRecord()

        record.duration = 0L
        val splits = record.split()
        assertNotNull(splits)
        assertEquals(0, splits.size)
    }

    /** Duration too small. */
    @Test
    fun splitRecords_Duration_Soon() {
        val record = createRecord()

        record.duration = DateUtils.MINUTE_IN_MILLIS
        val splits = record.split()
        assertNotNull(splits)
        assertEquals(0, splits.size)
    }

    /** Duration 1 hour. */
    @Test
    fun splitRecords_Duration_Hour() {
        val record = createRecord()

        record.duration = DateUtils.HOUR_IN_MILLIS
        val splits = record.split()
        assertNotNull(splits)
        assertEquals(1, splits.size)
        val rec0 = splits[0]
        assertNotNull(rec0)
        assertNull(rec0.start)
        assertNull(rec0.finish)
        assertEquals(DateUtils.HOUR_IN_MILLIS, rec0.duration)
    }

    /** Duration 1 day. */
    @Test
    fun splitRecords_Duration_Day() {
        val record = createRecord()

        record.duration = DateUtils.DAY_IN_MILLIS
        val splits = record.split()
        assertNotNull(splits)
        assertEquals(1, splits.size)
        val rec0 = splits[0]
        assertNotNull(rec0)
        assertNull(rec0.start)
        assertNull(rec0.finish)
        assertEquals(record.date.dayOfYear, rec0.date.dayOfYear)
        assertEquals(DateUtils.DAY_IN_MILLIS, rec0.duration)
    }

    /** Duration 2 days. */
    @Test
    fun splitRecords_Duration_Days() {
        val record = createRecord()

        record.duration = 2 * DateUtils.DAY_IN_MILLIS
        val splits = record.split()
        assertNotNull(splits)
        assertEquals(2, splits.size)
        val rec0 = splits[0]
        assertNotNull(rec0)
        assertNull(rec0.start)
        assertNull(rec0.finish)
        assertEquals(record.date.dayOfYear, rec0.date.dayOfYear)
        assertEquals(DateUtils.DAY_IN_MILLIS, rec0.duration)
        val rec1 = splits[1]
        assertNotNull(rec1)
        assertNull(rec1.start)
        assertNull(rec1.finish)
        assertEquals(record.date.dayOfYear + 1, rec1.date.dayOfYear)
        assertEquals(DateUtils.DAY_IN_MILLIS, rec1.duration)
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

    companion object {
        private const val PROJECT_NAME = "Project"
        private const val TASK_NAME = "Task"
    }
}

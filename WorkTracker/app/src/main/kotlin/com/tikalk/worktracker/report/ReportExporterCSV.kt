/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2019, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * • Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * • Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * • Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.tikalk.worktracker.report

import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import com.opencsv.CSVWriterBuilder
import com.opencsv.ICSVWriter
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.formatSystemDate
import com.tikalk.worktracker.time.formatSystemTime
import io.reactivex.SingleObserver
import java.io.File
import java.io.FileWriter
import java.io.Writer
import java.util.*
import kotlin.collections.ArrayList

/**
 * Write the list of records as a CSV file.
 */
class ReportExporterCSV(context: Context, records: List<TimeRecord>, filter: ReportFilter) : ReportExporter(context, records, filter) {

    override fun createRunner(context: Context, records: List<TimeRecord>, filter: ReportFilter, observer: SingleObserver<in Uri>): ReportExporterRunner {
        return ReportExporterCSVRunner(context, records, filter, observer)
    }

    private class ReportExporterCSVRunner(context: Context, records: List<TimeRecord>, filter: ReportFilter, observer: SingleObserver<in Uri>) : ReportExporterRunner(context, records, filter, observer) {
        override fun writeContents(context: Context, records: List<TimeRecord>, filter: ReportFilter, folder: File, filenamePrefix: String): File {
            val showProjectField = filter.showProjectField
            val showTaskField = filter.showTaskField
            val showStartField = filter.showStartField
            val showFinishField = filter.showFinishField
            val showDurationField = filter.showDurationField
            val showNotesField = filter.showNotesField
            val showCostField = filter.showCostField

            val file = File(folder, filenamePrefix + EXTENSION)
            val writer: Writer = FileWriter(file)
            out = writer
            val csvWriter: ICSVWriter = CSVWriterBuilder(writer).build()

            val headerRecord = ArrayList<String>()
            headerRecord.add(context.getString(R.string.date_header))
            if (showProjectField) {
                headerRecord.add(context.getString(R.string.project_header))
            }
            if (showTaskField) {
                headerRecord.add(context.getString(R.string.task_header))
            }
            if (showStartField) {
                headerRecord.add(context.getString(R.string.start_header))
            }
            if (showFinishField) {
                headerRecord.add(context.getString(R.string.finish_header))
            }
            if (showDurationField) {
                headerRecord.add(context.getString(R.string.duration_header))
            }
            if (showNotesField) {
                headerRecord.add(context.getString(R.string.note_header))
            }
            if (showCostField) {
                headerRecord.add(context.getString(R.string.cost_header))
            }
            csvWriter.writeNext(headerRecord.toTypedArray())

            val row = ArrayList<String>()
            for (record in records) {
                row.clear()
                row.add(formatSystemDate(record.start))
                if (showProjectField) {
                    row.add(record.project.name)
                }
                if (showTaskField) {
                    row.add(record.task.name)
                }
                if (showStartField) {
                    row.add(formatSystemTime(record.start))
                }
                if (showFinishField) {
                    row.add(formatSystemTime(record.finish))
                }
                if (showDurationField) {
                    val durationMs = record.finishTime - record.startTime
                    val durationHs = durationMs.toDouble() / DateUtils.HOUR_IN_MILLIS
                    row.add(String.format(Locale.US, "%.2f", durationHs))
                }
                if (showNotesField) {
                    row.add(record.note)
                }
                if (showCostField) {
                    row.add(String.format(Locale.US, "%.2f", record.cost))
                }
                csvWriter.writeNext(row.toTypedArray())
            }

            csvWriter.close()
            writer.close()
            return file
        }
    }

    companion object {
        const val MIME_TYPE = "application/csv"
        const val EXTENSION = ".csv"
    }
}
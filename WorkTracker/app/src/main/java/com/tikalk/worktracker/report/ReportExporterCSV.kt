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
import com.tikalk.worktracker.model.time.ReportTotals
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.formatSystemDate
import com.tikalk.worktracker.time.formatSystemTime
import kotlinx.coroutines.flow.FlowCollector
import java.io.File
import java.io.FileWriter
import java.io.Writer
import java.util.Locale

/**
 * Write the list of records as a Comma-separated Values (CSV) file.
 */
class ReportExporterCSV(
    context: Context,
    records: List<TimeRecord>,
    filter: ReportFilter,
    totals: ReportTotals
) : ReportExporter(context, records, filter, totals) {

    override val mimeType: String
        get() = MIME_TYPE

    override fun createRunner(
        context: Context,
        records: List<TimeRecord>,
        filter: ReportFilter,
        totals: ReportTotals,
        collector: FlowCollector<Uri>
    ): ReportExporterRunner {
        return ReportExporterCSVRunner(context, records, filter, totals, collector)
    }

    private class ReportExporterCSVRunner(
        context: Context,
        records: List<TimeRecord>,
        filter: ReportFilter,
        totals: ReportTotals,
        collector: FlowCollector<Uri>
    ) : ReportExporterRunner(context, records, filter, totals, collector) {
        override suspend fun writeContents(
            context: Context,
            records: List<TimeRecord>,
            filter: ReportFilter,
            totals: ReportTotals,
            folder: File,
            filenamePrefix: String
        ): File {
            val showProjectField = filter.showProjectField
            val showTaskField = filter.showTaskField
            val showStartField = filter.showStartField
            val showFinishField = filter.showFinishField
            val showDurationField = filter.showDurationField
            val showNoteField = filter.showNoteField
            val showCostField = filter.showCostField
            val showLocationField = filter.showLocationField

            val file = File(folder, filenamePrefix + EXTENSION)
            val writer: Writer = FileWriter(file)
            val csvWriter: ICSVWriter = CSVWriterBuilder(writer).build()

            val headerRecord = ArrayList<String>()
            headerRecord.add(context.getString(R.string.date_header))
            if (showProjectField) {
                headerRecord.add(context.getString(R.string.project_header))
            }
            if (showTaskField) {
                headerRecord.add(context.getString(R.string.task_header))
            }
            if (showLocationField) {
                headerRecord.add(context.getString(R.string.location_header))
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
            if (showNoteField) {
                headerRecord.add(context.getString(R.string.note_header))
            }
            if (showCostField) {
                headerRecord.add(context.getString(R.string.cost_header))
            }
            csvWriter.writeNext(headerRecord.toTypedArray())

            val row = ArrayList<String>()
            for (record in records) {
                row.clear()
                row.add(formatSystemDate(record.date)!!)
                if (showProjectField) {
                    row.add(record.project.name)
                }
                if (showTaskField) {
                    row.add(record.task.name)
                }
                if (showLocationField) {
                    val text = record.location.toLocationItem(context).label
                    row.add(text)
                }
                if (showStartField) {
                    row.add(formatSystemTime(record.start).orEmpty())
                }
                if (showFinishField) {
                    row.add(formatSystemTime(record.finish).orEmpty())
                }
                if (showDurationField) {
                    val durationMs = record.duration
                    val durationHs = durationMs.toDouble() / DateUtils.HOUR_IN_MILLIS
                    row.add(String.format(Locale.US, "%.2f", durationHs))
                }
                if (showNoteField) {
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
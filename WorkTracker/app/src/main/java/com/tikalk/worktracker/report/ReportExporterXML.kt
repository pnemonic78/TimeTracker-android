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
import android.util.Xml
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportTotals
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.formatSystemDate
import com.tikalk.worktracker.time.formatSystemTime
import kotlinx.coroutines.flow.FlowCollector
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileWriter
import java.io.Writer
import java.util.Locale

/**
 * Write the list of records as an Extensible Markup Language (XML) file.
 */
class ReportExporterXML(
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
        return ReportExporterXMLRunner(context, records, filter, totals, collector)
    }

    private class ReportExporterXMLRunner(
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
            val showProjectField = filter.isProjectFieldVisible
            val showTaskField = filter.isTaskFieldVisible
            val showStartField = filter.isStartFieldVisible
            val showFinishField = filter.isFinishFieldVisible
            val showDurationField = filter.isDurationFieldVisible
            val showNoteField = filter.isNoteFieldVisible
            val showCostField = filter.isCostFieldVisible

            val file = File(folder, filenamePrefix + EXTENSION)
            val writer: Writer = FileWriter(file)
            val xmlWriter: XmlSerializer = Xml.newSerializer()

            val ns: String? = null
            xmlWriter.setOutput(writer)
            xmlWriter.setFeature(FEATURE_INDENT, true)
            xmlWriter.startDocument(null, true)
            xmlWriter.startTag(ns, "rows")

            for (record in records) {
                xmlWriter.startTag(ns, "row")
                xmlWriter.startTag(ns, "date")
                xmlWriter.text(formatSystemDate(record.date))
                xmlWriter.endTag(ns, "date")
                if (showProjectField) {
                    xmlWriter.startTag(ns, "project")
                    xmlWriter.text(record.project.name)
                    xmlWriter.endTag(ns, "project")
                }
                if (showTaskField) {
                    xmlWriter.startTag(ns, "task")
                    xmlWriter.text(record.task.name)
                    xmlWriter.endTag(ns, "task")
                }
                if (showStartField) {
                    xmlWriter.startTag(ns, "start")
                    xmlWriter.text(formatSystemTime(record.start).orEmpty())
                    xmlWriter.endTag(ns, "start")
                }
                if (showFinishField) {
                    xmlWriter.startTag(ns, "finish")
                    xmlWriter.text(formatSystemTime(record.finish).orEmpty())
                    xmlWriter.endTag(ns, "finish")
                }
                if (showDurationField) {
                    val durationMs = record.duration
                    val durationHs = durationMs.toDouble() / DateUtils.HOUR_IN_MILLIS
                    xmlWriter.startTag(ns, "duration")
                    xmlWriter.text(String.format(Locale.US, "%.2f", durationHs))
                    xmlWriter.endTag(ns, "duration")
                }
                if (showNoteField) {
                    xmlWriter.startTag(ns, "note")
                    xmlWriter.text(record.note)
                    xmlWriter.endTag(ns, "note")
                }
                if (showCostField) {
                    xmlWriter.startTag(ns, "cost")
                    xmlWriter.text(String.format(Locale.US, "%.2f", record.cost))
                    xmlWriter.endTag(ns, "cost")
                }
                xmlWriter.endTag(ns, "row")
            }

            xmlWriter.endTag(ns, "rows")
            xmlWriter.endDocument()
            writer.close()

            return file
        }
    }

    companion object {
        const val MIME_TYPE = "application/xml"
        const val EXTENSION = ".xml"

        private const val FEATURE_INDENT = "http://xmlpull.org/v1/doc/features.html#indent-output"
    }
}
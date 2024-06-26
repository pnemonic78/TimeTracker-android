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
import com.tikalk.util.TikalFormatter
import com.tikalk.util.isEven
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportTotals
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.formatCurrency
import com.tikalk.worktracker.time.formatElapsedTime
import com.tikalk.worktracker.time.formatSystemDate
import com.tikalk.worktracker.time.formatSystemTime
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.html.Entities
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.consumers.DelayedConsumer
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.stream.HTMLStreamBuilder
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.html.unsafe
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.Writer
import java.util.Locale

/**
 * Write the list of records as an Hypertext Markup Language (HTML) file.
 */
class ReportExporterHTML(
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
        return ReportExporterHTMLRunner(context, records, filter, totals, collector)
    }

    private class ReportExporterHTMLRunner(
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

            val titleText = context.getString(
                R.string.reports_header,
                formatSystemDate(filter.start),
                formatSystemDate(filter.finish)
            )

            val cssStream = context.assets.open("default.css")
            val cssReader = InputStreamReader(cssStream)
            val css = cssReader.readText()

            val timeFormatter = TikalFormatter()
            val currencyFormatter = TikalFormatter()

            val consumer = DelayedConsumer(
                HTMLStreamBuilder(
                    writer,
                    prettyPrint = true,
                    xhtmlCompatible = true
                )
            ).html {
                head {
                    title(titleText)
                    style(MIME_TYPE_CSS) {
                        unsafe {
                            +"\n"
                            +css
                        }
                    }
                }
                body {
                    table {
                        attributes["border"] = "0"
                        attributes["cellpadding"] = "5"
                        attributes["cellspacing"] = "0"
                        attributes["width"] = "100%"

                        tr {
                            td("sectionHeader") {
                                div("pageTitle") {
                                    +titleText
                                }
                            }
                        }
                    }

                    br

                    table(classes = "x-scrollable-table") {
                        attributes["border"] = "0"
                        attributes["cellpadding"] = "3"
                        attributes["cellspacing"] = "1"
                        attributes["width"] = "100%"

                        tr {
                            th {
                                +context.getString(R.string.date_header)
                            }
                            if (showProjectField) {
                                th {
                                    +context.getString(R.string.project_header)
                                }
                            }
                            if (showTaskField) {
                                th {
                                    +context.getString(R.string.task_header)
                                }
                            }
                            if (showStartField) {
                                th {
                                    +context.getString(R.string.start_header)
                                }
                            }
                            if (showFinishField) {
                                th {
                                    +context.getString(R.string.finish_header)
                                }
                            }
                            if (showDurationField) {
                                th {
                                    +context.getString(R.string.duration_header)
                                }
                            }
                            if (showNoteField) {
                                th {
                                    +context.getString(R.string.note_header)
                                }
                            }
                            if (showCostField) {
                                th {
                                    +context.getString(R.string.cost_header)
                                }
                            }
                        }

                        for (i in records.indices) {
                            val record = records[i]

                            tr(if (i.isEven()) "rowReportItem" else "rowReportItemAlt") {
                                td("date-cell") {
                                    +formatSystemDate(record.date)!!
                                }
                                if (showProjectField) {
                                    td("text-cell") {
                                        +record.project.name
                                    }
                                }
                                if (showTaskField) {
                                    td("text-cell") {
                                        +record.task.name
                                    }
                                }
                                if (showStartField) {
                                    td("time-cell") {
                                        +formatSystemTime(record.start).orEmpty()
                                    }
                                }
                                if (showFinishField) {
                                    td("time-cell") {
                                        +formatSystemTime(record.finish).orEmpty()
                                    }
                                }
                                if (showDurationField) {
                                    val durationMs = record.duration
                                    formatElapsedTime(context, timeFormatter, durationMs)
                                    td("time-cell") {
                                        +timeFormatter.toString()
                                    }
                                }
                                if (showNoteField) {
                                    td("text-cell") {
                                        +record.note
                                    }
                                }
                                if (showCostField) {
                                    td("money-value-cell") {
                                        +String.format(Locale.US, "%.2f", record.cost)
                                    }
                                }
                            }
                        }

                        tr {
                            td {
                                +Entities.nbsp
                            }
                        }

                        tr {
                            td("invoice-label") {
                                +context.getString(R.string.total)
                            }
                            if (showProjectField) {
                                td {}
                            }
                            if (showTaskField) {
                                td {}
                            }
                            if (showStartField) {
                                td {}
                            }
                            if (showFinishField) {
                                td {}
                            }
                            if (showDurationField) {
                                td("time-cell subtotal-cell") {
                                    +formatElapsedTime(context, timeFormatter, totals.duration)
                                }
                            }
                            if (showNoteField) {
                                td {}
                            }
                            if (showCostField) {
                                td("money-value-cell subtotal-cell") {
                                    +formatCurrency(currencyFormatter, totals.cost)
                                }
                            }
                        }
                    }
                }
            }

            consumer.close()

            return file
        }
    }

    companion object {
        const val MIME_TYPE = "text/html"
        const val MIME_TYPE_CSS = "text/css"
        const val EXTENSION = ".html"
    }
}
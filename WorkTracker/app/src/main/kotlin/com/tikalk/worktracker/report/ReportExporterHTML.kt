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
import com.tikalk.util.isEven
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportTotals
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.formatCurrency
import com.tikalk.worktracker.time.formatElapsedTime
import com.tikalk.worktracker.time.formatSystemDate
import com.tikalk.worktracker.time.formatSystemTime
import io.reactivex.SingleObserver
import kotlinx.html.*
import kotlinx.html.consumers.DelayedConsumer
import kotlinx.html.stream.HTMLStreamBuilder
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.Writer
import java.util.*

/**
 * Write the list of records as an Hypertext Markup Language (HTML) file.
 */
class ReportExporterHTML(context: Context, records: List<TimeRecord>, filter: ReportFilter, totals: ReportTotals) : ReportExporter(context, records, filter, totals) {

    override fun createRunner(context: Context, records: List<TimeRecord>, filter: ReportFilter, totals: ReportTotals, observer: SingleObserver<in Uri>): ReportExporterRunner {
        return ReportExporterHTMLRunner(context, records, filter, totals, observer)
    }

    private class ReportExporterHTMLRunner(context: Context, records: List<TimeRecord>, filter: ReportFilter, totals: ReportTotals, observer: SingleObserver<in Uri>) : ReportExporterRunner(context, records, filter, totals, observer) {
        override fun writeContents(context: Context, records: List<TimeRecord>, filter: ReportFilter, totals: ReportTotals, folder: File, filenamePrefix: String): File {
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
            out = writer

            val titleText = context.getString(R.string.reports_header, formatSystemDate(filter.start), formatSystemDate(filter.finish))

            val cssStream = context.assets.open("default.css")
            val cssReader = InputStreamReader(cssStream)
            val css = cssReader.readText()

            val timeBuffer = StringBuilder(20)
            val timeFormatter = Formatter(timeBuffer, Locale.getDefault())
            val currencyBuffer = StringBuilder(20)
            val currencyFormatter = Formatter(currencyBuffer, Locale.getDefault())

            val consumer = DelayedConsumer(HTMLStreamBuilder(writer, prettyPrint = true, xhtmlCompatible = true)).html {
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

                    table {
                        attributes["border"] = "0"
                        attributes["cellpadding"] = "3"
                        attributes["cellspacing"] = "1"
                        attributes["width"] = "100%"

                        tr {
                            td("tableHeader") {
                                +context.getString(R.string.date_header)
                            }
                            if (showProjectField) {
                                td("tableHeader") {
                                    +context.getString(R.string.project_header)
                                }
                            }
                            if (showTaskField) {
                                td("tableHeader") {
                                    +context.getString(R.string.task_header)
                                }
                            }
                            if (showLocationField) {
                                td("tableHeader") {
                                    +context.getString(R.string.location_header)
                                }
                            }
                            if (showStartField) {
                                td("tableHeaderCentered") {
                                    +context.getString(R.string.start_header)
                                }
                            }
                            if (showFinishField) {
                                td("tableHeaderCentered") {
                                    +context.getString(R.string.finish_header)
                                }
                            }
                            if (showDurationField) {
                                td("tableHeaderCentered") {
                                    +context.getString(R.string.duration_header)
                                }
                            }
                            if (showNoteField) {
                                td("tableHeader") {
                                    +context.getString(R.string.note_header)
                                }
                            }
                            if (showCostField) {
                                td("tableHeaderCentered") {
                                    +context.getString(R.string.cost_header)
                                }
                            }
                        }

                        for (i in records.indices) {
                            val record = records[i]

                            tr(if (i.isEven()) "rowReportItem" else "rowReportItemAlt") {
                                td("cellLeftAligned") {
                                    +formatSystemDate(record.start)
                                }
                                if (showProjectField) {
                                    td("cellLeftAligned") {
                                        +record.project.name
                                    }
                                }
                                if (showTaskField) {
                                    td("cellLeftAligned") {
                                        +record.task.name
                                    }
                                }
                                if (showLocationField) {
                                    td("cellLeftAligned") {
                                        +record.location.toLocationItem(context).label
                                    }
                                }
                                if (showStartField) {
                                    td("cellRightAligned") {
                                        +formatSystemTime(record.start)
                                    }
                                }
                                if (showFinishField) {
                                    td("cellRightAligned") {
                                        +formatSystemTime(record.finish)
                                    }
                                }
                                if (showDurationField) {
                                    timeBuffer.setLength(0)
                                    val durationMs = record.finishTime - record.startTime
                                    formatElapsedTime(context, timeFormatter, durationMs)
                                    td("cellRightAligned") {
                                        +timeFormatter.toString()
                                    }
                                }
                                if (showNoteField) {
                                    td("cellLeftAligned") {
                                        +record.note
                                    }
                                }
                                if (showCostField) {
                                    td("cellRightAligned") {
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

                        tr("rowReportSubtotal") {
                            td("cellLeftAlignedSubtotal") {
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
                                td("cellRightAlignedSubtotal") {
                                    timeBuffer.setLength(0)
                                    +formatElapsedTime(context, timeFormatter, totals.duration).toString()
                                }
                            }
                            if (showNoteField) {
                                td {}
                            }
                            if (showCostField) {
                                td("cellRightAlignedSubtotal") {
                                    timeBuffer.setLength(0)
                                    +formatCurrency(currencyFormatter, totals.cost).toString()
                                }
                            }
                        }
                    }
                }
            }

            consumer.close()
            out = null

            return file
        }
    }

    companion object {
        const val MIME_TYPE = "text/html"
        const val MIME_TYPE_CSS = "text/css"
        const val EXTENSION = ".html"
    }
}
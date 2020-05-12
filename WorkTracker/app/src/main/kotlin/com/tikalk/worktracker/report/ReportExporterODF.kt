/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2020, Tikal Knowledge, Ltd.
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
import com.tikalk.util.isEven
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportTotals
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.SYSTEM_DATE_PATTERN
import com.tikalk.worktracker.time.formatSystemDate
import io.reactivex.SingleObserver
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import org.odftoolkit.odfdom.doc.table.OdfTableCell
import org.odftoolkit.odfdom.dom.attribute.style.StyleDataStyleNameAttribute
import org.odftoolkit.odfdom.dom.element.table.TableTableCellElement
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily
import org.odftoolkit.odfdom.dom.style.props.OdfParagraphProperties
import org.odftoolkit.odfdom.dom.style.props.OdfTableCellProperties
import org.odftoolkit.odfdom.dom.style.props.OdfTextProperties
import org.odftoolkit.odfdom.incubator.doc.number.OdfNumberDateStyle
import org.odftoolkit.odfdom.type.Color
import java.io.File
import java.util.*

/**
 * Write the list of records as an OpenDocument Format (ODF) spreadsheet file.
 */
class ReportExporterODF(context: Context, records: List<TimeRecord>, filter: ReportFilter, totals: ReportTotals) : ReportExporter(context, records, filter, totals) {

    override fun createRunner(context: Context, records: List<TimeRecord>, filter: ReportFilter, totals: ReportTotals, observer: SingleObserver<in Uri>): ReportExporterRunner {
        return ReportExporterODFRunner(context, records, filter, totals, observer)
    }

    private class ReportExporterODFRunner(context: Context, records: List<TimeRecord>, filter: ReportFilter, totals: ReportTotals, observer: SingleObserver<in Uri>) : ReportExporterRunner(context, records, filter, totals, observer) {
        override fun writeContents(context: Context, records: List<TimeRecord>, filter: ReportFilter, totals: ReportTotals, folder: File, filenamePrefix: String): File {
            val showProjectField = filter.showProjectField
            val showTaskField = filter.showTaskField
            val showStartField = filter.showStartField
            val showFinishField = filter.showFinishField
            val showDurationField = filter.showDurationField
            val showNoteField = filter.showNoteField
            val showCostField = filter.showCostField

            val locale = Locale.getDefault()
            val currency = Currency.getInstance(locale)
            val currencyCode = currency.currencyCode

            val file = File(folder, filenamePrefix + EXTENSION)
            out = null

            val doc = OdfSpreadsheetDocument.newSpreadsheetDocument()
            val table = doc.tableList[0]

            var rowIndex = 0
            var columnIndex = 0
            var cell: OdfTableCell

            val titleText = context.getString(R.string.reports_header, formatSystemDate(filter.start), formatSystemDate(filter.finish))
            cell = table.getCellByPosition(columnIndex, rowIndex)
            cell.stringValue = titleText
            val cellTitle = cell

            rowIndex += 2
            columnIndex = 0

            cell = table.getCellByPosition(columnIndex++, rowIndex)
            cell.stringValue = context.getString(R.string.date_header)
            if (showProjectField) {
                cell = table.getCellByPosition(columnIndex++, rowIndex)
                cell.stringValue = context.getString(R.string.project_header)
            }
            if (showTaskField) {
                cell = table.getCellByPosition(columnIndex++, rowIndex)
                cell.stringValue = context.getString(R.string.task_header)
            }
            var cellHeaderStart: OdfTableCell? = null
            if (showStartField) {
                cell = table.getCellByPosition(columnIndex++, rowIndex)
                cell.stringValue = context.getString(R.string.start_header)
                cellHeaderStart = cell
            }
            var cellHeaderFinish: OdfTableCell? = null
            if (showFinishField) {
                cell = table.getCellByPosition(columnIndex++, rowIndex)
                cell.stringValue = context.getString(R.string.finish_header)
                cellHeaderFinish = cell
            }
            var cellHeaderDuration: OdfTableCell? = null
            if (showDurationField) {
                cell = table.getCellByPosition(columnIndex++, rowIndex)
                cell.stringValue = context.getString(R.string.duration_header)
                cellHeaderDuration = cell
            }
            if (showNoteField) {
                cell = table.getCellByPosition(columnIndex++, rowIndex)
                cell.stringValue = context.getString(R.string.note_header)
            }
            if (showCostField) {
                cell = table.getCellByPosition(columnIndex++, rowIndex)
                cell.stringValue = context.getString(R.string.cost_header)
            }
            val rowIndexHeader = rowIndex
            val columnCount = columnIndex
            val rowIndexRecords = rowIndex + 1

            for (record in records) {
                rowIndex++
                columnIndex = 0

                cell = table.getCellByPosition(columnIndex++, rowIndex)
                cell.dateValue = record.start
                if (showProjectField) {
                    cell = table.getCellByPosition(columnIndex++, rowIndex)
                    cell.stringValue = record.project.name
                }
                if (showTaskField) {
                    cell = table.getCellByPosition(columnIndex++, rowIndex)
                    cell.stringValue = record.task.name
                }
                if (showStartField) {
                    cell = table.getCellByPosition(columnIndex++, rowIndex)
                    cell.timeValue = record.start
                }
                if (showFinishField) {
                    cell = table.getCellByPosition(columnIndex++, rowIndex)
                    cell.timeValue = record.finish
                }
                if (showDurationField) {
                    val durationMs = record.finishTime - record.startTime
                    val durationHs = durationMs.toDouble() / DateUtils.HOUR_IN_MILLIS
                    cell = table.getCellByPosition(columnIndex++, rowIndex)
                    cell.doubleValue = durationHs
                }
                if (showNoteField) {
                    cell = table.getCellByPosition(columnIndex++, rowIndex)
                    cell.stringValue = record.note
                }
                if (showCostField) {
                    cell = table.getCellByPosition(columnIndex++, rowIndex)
                    cell.setCurrencyValue(record.cost, currencyCode)
                }
                assert(columnIndex == columnCount)
            }

            rowIndex += 2
            columnIndex = 0
            val rowIndexSubtotal = rowIndex
            cell = table.getCellByPosition(columnIndex++, rowIndex)
            cell.stringValue = context.getString(R.string.total)
            if (showProjectField) {
                columnIndex++
            }
            if (showTaskField) {
                columnIndex++
            }
            if (showStartField) {
                columnIndex++
            }
            if (showFinishField) {
                columnIndex++
            }
            if (showDurationField) {
                val durationMs = totals.duration
                val durationHs = durationMs.toDouble() / DateUtils.HOUR_IN_MILLIS
                cell = table.getCellByPosition(columnIndex++, rowIndex)
                cell.doubleValue = durationHs
            }
            if (showNoteField) {
                columnIndex++
            }
            if (showCostField) {
                cell = table.getCellByPosition(columnIndex++, rowIndex)
                cell.setCurrencyValue(totals.cost, currencyCode)
            }
            assert(columnIndex == columnCount)

            val documentStyles = doc.documentStyles

            val sectionHeaderStyleName = "sectionHeader"
            val sectionHeaderStyle = documentStyles.newStyle(sectionHeaderStyleName, OdfStyleFamily.TableCell)
            sectionHeaderStyle.setProperty(OdfTextProperties.FontWeight, "bold")
            sectionHeaderStyle.setProperty(OdfTextProperties.Color, Color.SILVER.toString())
            sectionHeaderStyle.setProperty(OdfTextProperties.FontSize, "12pt")
            sectionHeaderStyle.setProperty(OdfTableCellProperties.BorderBottom, "1pt solid ${Color.SILVER}")

            val tableHeaderStyleName = "tableHeader"
            val tableHeaderStyle = documentStyles.newStyle(tableHeaderStyleName, OdfStyleFamily.TableCell)
            tableHeaderStyle.setProperty(OdfTextProperties.FontWeight, "bold")

            val tableHeaderCenteredStyleName = "tableHeaderCentered"
            val tableHeaderCenteredStyle = documentStyles.newStyle(tableHeaderCenteredStyleName, OdfStyleFamily.TableCell)
            tableHeaderCenteredStyle.setProperty(OdfTextProperties.FontWeight, "bold")
            tableHeaderCenteredStyle.setProperty(OdfParagraphProperties.TextAlign, "center")

            val rowReportItemStyleName = "rowReportItem"
            val rowReportItemStyle = documentStyles.newStyle(rowReportItemStyleName, OdfStyleFamily.TableCell)
            rowReportItemStyle.setProperty(OdfTableCellProperties.BackgroundColor, "#f5f5f5")

            val dateStyleName = "reportItemDate"
            val dateStyle = OdfNumberDateStyle(doc.stylesDom, SYSTEM_DATE_PATTERN, dateStyleName)
            documentStyles.appendChild(dateStyle)

            val rowReportItemDateStyleName = "rowReportItemDate"
            val rowReportItemDateStyle = documentStyles.newStyle(rowReportItemDateStyleName, OdfStyleFamily.TableCell)
            rowReportItemDateStyle.setProperty(OdfTableCellProperties.BackgroundColor, "#f5f5f5")
            rowReportItemDateStyle.setOdfAttributeValue(StyleDataStyleNameAttribute.ATTRIBUTE_NAME, dateStyleName)

            val rowReportItemAltStyleName = "rowReportItemAlt"
            val rowReportItemAltStyle = documentStyles.newStyle(rowReportItemAltStyleName, OdfStyleFamily.TableCell)
            rowReportItemAltStyle.setProperty(OdfTableCellProperties.BackgroundColor, "#ffffff")

            val rowReportItemDateAltStyleName = "rowReportItemDateAlt"
            val rowReportItemDateAltStyle = documentStyles.newStyle(rowReportItemDateAltStyleName, OdfStyleFamily.TableCell)
            rowReportItemDateAltStyle.setProperty(OdfTableCellProperties.BackgroundColor, "#ffffff")
            rowReportItemDateAltStyle.setOdfAttributeValue(StyleDataStyleNameAttribute.ATTRIBUTE_NAME, dateStyleName)

            val rowReportSubtotalStyleName = "rowReportSubtotal"
            val rowReportSubtotalStyle = documentStyles.newStyle(rowReportSubtotalStyleName, OdfStyleFamily.TableCell)
            rowReportSubtotalStyle.setProperty(OdfTextProperties.FontWeight, "bold")
            rowReportSubtotalStyle.setProperty(OdfTableCellProperties.BackgroundColor, "#e0e0e0")

            val cellTitleElement = cellTitle.odfElement
            cellTitleElement.styleName = sectionHeaderStyleName
            if (cellTitleElement is TableTableCellElement) {
                cellTitleElement.tableNumberColumnsSpannedAttribute = columnCount
            }

            for (c in 0 until columnCount) {
                cell = table.getCellByPosition(c, rowIndexHeader)
                cell.odfElement.styleName = tableHeaderStyleName

                cell = table.getCellByPosition(c, rowIndexSubtotal)
                cell.odfElement.styleName = rowReportSubtotalStyleName
            }

            for (i in records.indices) {
                rowIndex = rowIndexRecords + i

                cell = table.getCellByPosition(0, rowIndex)
                if (i.isEven()) {
                    cell.odfElement.styleName = rowReportItemDateStyleName
                } else {
                    cell.odfElement.styleName = rowReportItemDateAltStyleName
                }

                for (c in 1 until columnCount) {
                    cell = table.getCellByPosition(c, rowIndex)
                    if (i.isEven()) {
                        cell.odfElement.styleName = rowReportItemStyleName
                    } else {
                        cell.odfElement.styleName = rowReportItemAltStyleName
                    }
                }
            }

            cellHeaderStart?.odfElement?.styleName = tableHeaderCenteredStyleName
            cellHeaderFinish?.odfElement?.styleName = tableHeaderCenteredStyleName
            cellHeaderDuration?.odfElement?.styleName = tableHeaderCenteredStyleName

            doc.save(file)
            doc.close()

            return file
        }
    }

    companion object {
        private val SPREADSHEET = OdfSpreadsheetDocument.OdfMediaType.SPREADSHEET
        val MIME_TYPE = SPREADSHEET.mediaTypeString
        val EXTENSION = "." + SPREADSHEET.suffix
    }
}
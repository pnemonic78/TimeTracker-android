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
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.TimeRecord
import io.reactivex.SingleObserver
import org.jsoup.nodes.Element
import java.io.File
import java.io.FileWriter
import java.io.Writer

/**
 * Write the list of records as an HTML file.
 */
class ReportExporterHTML(context: Context, val table: Element, filter: ReportFilter, folder: File) : ReportExporter(context, emptyList(), filter, folder) {

    override fun createRunner(context: Context, records: List<TimeRecord>, filter: ReportFilter, folder: File, observer: SingleObserver<in File>): ReportExporterRunner {
        return ReportExporterHTMLRunner(context, table, filter, folder, observer)
    }

    private class ReportExporterHTMLRunner(context: Context, val table: Element, filter: ReportFilter, folder: File, observer: SingleObserver<in File>) : ReportExporterRunner(context, emptyList(), filter, folder, observer) {
        override fun writeContents(context: Context, records: List<TimeRecord>, filter: ReportFilter, folder: File, filenamePrefix: String): File {
            val table = this.table
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

            val html = table.outerHtml()
            writer.write(html)

            writer.close()

            return file
        }
    }

    companion object {
        const val MIME_TYPE = "text/html"
        const val EXTENSION = ".html"
    }
}
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
import androidx.core.content.FileProvider
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportTotals
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.formatSystemDate
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Write the list of records to a file.
 */
@OptIn(FlowPreview::class)
abstract class ReportExporter(
    val context: Context,
    val records: List<TimeRecord>,
    val filter: ReportFilter,
    val totals: ReportTotals
) : AbstractFlow<Uri>() {

    @ExperimentalCoroutinesApi
    override suspend fun collectSafely(collector: FlowCollector<Uri>) {
        val runner = createRunner(context, records, filter, totals, collector)
        runner.run()
    }

    protected abstract fun createRunner(
        context: Context,
        records: List<TimeRecord>,
        filter: ReportFilter,
        totals: ReportTotals,
        collector: FlowCollector<Uri>
    ): ReportExporterRunner

    protected abstract class ReportExporterRunner(
        val context: Context,
        val records: List<TimeRecord>,
        val filter: ReportFilter,
        val totals: ReportTotals,
        val collector: FlowCollector<Uri>
    ) {

        suspend fun run() {
            val folder = File(context.filesDir, FOLDER_EXPORTS)
            folder.mkdirs()
            val filename =
                "report_${formatSystemDate(filter.start)}_${formatSystemDate(filter.finish)}"
            val file = writeContents(context, records, filter, totals, folder, filename)
            val fileUri: Uri? = FileProvider.getUriForFile(context, AUTHORITY, file)

            if (fileUri != null) {
                collector.emit(fileUri)
            } else {
                throw FileNotFoundException(file.toString())
            }
        }

        @Throws(IOException::class)
        protected abstract suspend fun writeContents(
            context: Context,
            records: List<TimeRecord>,
            filter: ReportFilter,
            totals: ReportTotals,
            folder: File,
            filenamePrefix: String
        ): File
    }

    abstract val mimeType: String

    companion object {
        private const val FOLDER_EXPORTS = "exports"
        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.files"
    }
}
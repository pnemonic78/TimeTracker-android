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
import com.github.reactivex.DefaultDisposable
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.formatSystemDate
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import java.io.File
import java.io.IOException
import java.io.Writer

abstract class ReportExporter(val context: Context, val records: List<TimeRecord>, val filter: ReportFilter, val folder: File) : Single<File>(), Disposable {

    private var runner: ReportExporterRunner? = null

    override fun subscribeActual(observer: SingleObserver<in File>) {
        val r = createRunner(context, records, filter, folder, observer)
        runner = r
        observer.onSubscribe(r)
        r.run()
    }

    protected abstract fun createRunner(context: Context, records: List<TimeRecord>, filter: ReportFilter, folder: File, observer: SingleObserver<in File>): ReportExporterRunner

    override fun isDisposed(): Boolean {
        return runner?.isDisposed ?: false
    }

    override fun dispose() {
        runner?.dispose()
    }

    fun cancel() {
        dispose()
    }

    fun isIdle(): Boolean = (runner == null) || !runner!!.running || isDisposed

    protected abstract class ReportExporterRunner(val context: Context, val records: List<TimeRecord>, val filter: ReportFilter, val folder: File, val observer: SingleObserver<in File>) : DefaultDisposable() {

        var running = false
            private set
        protected var out: Writer? = null

        override fun onDispose() {
            out?.close()
        }

        fun run() {
            running = true
            if (isDisposed) {
                running = false
                return
            }

            folder.mkdirs()
            val filename = "report_${formatSystemDate(filter.start)}_${formatSystemDate(filter.finish)}"
            val file = writeContents(context, records, filter, folder, filename)

            running = false
            if (!isDisposed) {
                observer.onSuccess(file)
            }
        }

        @Throws(IOException::class)
        protected abstract fun writeContents(context: Context, records: List<TimeRecord>, filter: ReportFilter, folder: File, filenamePrefix: String): File
    }
}
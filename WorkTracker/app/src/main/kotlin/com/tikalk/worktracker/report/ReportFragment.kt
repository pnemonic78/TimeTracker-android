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
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isNavDestination
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragment
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportPage
import com.tikalk.worktracker.model.time.ReportTotals
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.time.formatCurrency
import com.tikalk.worktracker.time.formatElapsedTime
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_report_list.*
import kotlinx.android.synthetic.main.report_totals.*
import timber.log.Timber
import java.util.*

class ReportFragment : InternetFragment(),
    LoginFragment.OnLoginListener {

    private val recordsData = MutableLiveData<List<TimeRecord>>()
    private val totalsData = MutableLiveData<ReportTotals>()
    private val filterData = MutableLiveData<ReportFilter>()
    private var listAdapter = ReportAdapter(ReportFilter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        recordsData.observe(this, Observer { records ->
            bindList(records)
        })
        totalsData.observe(this, Observer { totals ->
            bindTotals(totals)
        })
        filterData.observe(this, Observer { filter ->
            this.listAdapter = ReportAdapter(filter)
            list.adapter = listAdapter
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_report_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.adapter = listAdapter
    }

    @MainThread
    private fun bindList(records: List<TimeRecord>) {
        if (!isVisible) return
        listAdapter.submitList(records)
        if (records === recordsData.value) {
            listAdapter.notifyDataSetChanged()
        }
        if (records.isNotEmpty()) {
            listSwitcher.displayedChild = CHILD_LIST
        } else {
            listSwitcher.displayedChild = CHILD_EMPTY
        }
    }

    @MainThread
    private fun bindTotals(totals: ReportTotals) {
        val context = this.context ?: return
        val timeBuffer = StringBuilder(20)
        val timeFormatter = Formatter(timeBuffer, Locale.getDefault())
        val currencyBuffer = StringBuilder(20)
        val currencyFormatter = Formatter(currencyBuffer, Locale.getDefault())
        val filter = filterData.value

        if (filter?.showDurationField == true) {
            timeBuffer.setLength(0)
            durationTotalLabel.visibility = View.VISIBLE
            durationTotal.text = formatElapsedTime(context, timeFormatter, totals.duration).toString()
        } else {
            durationTotalLabel.visibility = View.INVISIBLE
            durationTotal.text = null
        }
        if (filter?.showCostField == true) {
            timeBuffer.setLength(0)
            costTotalLabel.visibility = View.VISIBLE
            costTotal.text = formatCurrency(currencyFormatter, totals.cost).toString()
        } else {
            costTotalLabel.visibility = View.INVISIBLE
            costTotal.text = null
        }
    }

    override fun authenticate(submit: Boolean) {
        Timber.i("authenticate submit=$submit currentDestination=${findNavController().currentDestination?.label}")
        if (!isNavDestination(R.id.loginFragment)) {
            val args = Bundle()
            parentFragmentManager.putFragment(args, LoginFragment.EXTRA_CALLER, this)
            args.putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
            findNavController().navigate(R.id.action_reportList_to_login, args)
        }
    }

    @MainThread
    fun run() {
        Timber.i("run first=$firstRun")

        var filter: ReportFilter? = null

        val args = arguments
        if (args != null) {
            if (args.containsKey(EXTRA_FILTER)) {
                val filterExtra = args.getParcelable<ReportFilter>(EXTRA_FILTER)
                if (filterExtra != null) {
                    filter = filterExtra
                }
            }
        }
        if (filter == null) {
            filter = filterData.value ?: ReportFilter()
        }

        dataSource.reportPage(filter, firstRun)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ page ->
                processPage(page)
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                handleError(err)
            })
            .addTo(disposables)
    }

    private fun processPage(page: ReportPage) {
        filterData.value = page.filter
        recordsData.value = page.records
        totalsData.value = page.totals
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    override fun onLoginSuccess(fragment: LoginFragment, login: String) {
        Timber.i("login success")
        fragment.dismissAllowingStateLoss()
        run()
    }

    override fun onLoginFailure(fragment: LoginFragment, login: String, reason: String) {
        Timber.e("login failure: $reason")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.report, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_export_csv -> {
                exportCSV(item)
                return true
            }
            R.id.menu_export_html -> {
                exportHTML(item)
                return true
            }
            R.id.menu_export_odf -> {
                exportODF(item)
                return true
            }
            R.id.menu_export_xml -> {
                exportXML(item)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun exportCSV(item: MenuItem? = null) {
        val context = this.context ?: return
        val records = recordsData.value ?: return
        val filter = filterData.value ?: return
        val totals = totalsData.value ?: return

        item?.isEnabled = false
        showProgress(true)

        ReportExporterCSV(context, records, filter, totals)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ uri ->
                Timber.i("Exported CSV to $uri")
                shareFile(context, uri, ReportExporterCSV.MIME_TYPE)
                showProgress(false)
                item?.isEnabled = true
            }, { err ->
                Timber.e(err, "Error exporting CSV: ${err.message}")
                showProgress(false)
                item?.isEnabled = true
            })
            .addTo(disposables)
    }

    private fun exportHTML(item: MenuItem? = null) {
        val context = this.context ?: return
        val records = recordsData.value ?: return
        val filter = filterData.value ?: return
        val totals = totalsData.value ?: return

        item?.isEnabled = false
        showProgress(true)

        ReportExporterHTML(context, records, filter, totals)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ file ->
                Timber.i("Exported HTML to $file")
                shareFile(context, file, ReportExporterHTML.MIME_TYPE)
                showProgress(false)
                item?.isEnabled = true
            }, { err ->
                Timber.e(err, "Error exporting HTML: ${err.message}")
                showProgress(false)
                item?.isEnabled = true
            })
            .addTo(disposables)
    }

    private fun exportODF(item: MenuItem? = null) {
        val context = this.context ?: return
        val records = recordsData.value ?: return
        val filter = filterData.value ?: return
        val totals = totalsData.value ?: return

        item?.isEnabled = false
        showProgress(true)

        ReportExporterODF(context, records, filter, totals)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ file ->
                Timber.i("Exported ODF to $file")
                shareFile(context, file, ReportExporterODF.MIME_TYPE)
                showProgress(false)
                item?.isEnabled = true
            }, { err ->
                Timber.e(err, "Error exporting ODF: ${err.message}")
                showProgress(false)
                alert(err)
                item?.isEnabled = true
            })
            .addTo(disposables)
    }

    private fun exportXML(item: MenuItem? = null) {
        val context = this.context ?: return
        val records = recordsData.value ?: return
        val filter = filterData.value ?: return
        val totals = totalsData.value ?: return

        item?.isEnabled = false
        showProgress(true)

        ReportExporterXML(context, records, filter, totals)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ file ->
                Timber.i("Exported XML to $file")
                shareFile(context, file, ReportExporterXML.MIME_TYPE)
                showProgress(false)
                item?.isEnabled = true
            }, { err ->
                Timber.e(err, "Error exporting XML: ${err.message}")
                showProgress(false)
                item?.isEnabled = true
            })
            .addTo(disposables)
    }

    private fun shareFile(context: Context, fileUri: Uri, mimeType: String? = null) {
        val activity = this.activity ?: return
        val intent = ShareCompat.IntentBuilder.from(activity)
            .addStream(fileUri)
            .setType(mimeType ?: context.contentResolver.getType(fileUri))
            .intent
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Validate that the device can open your File!
        val pm = context.packageManager
        if (intent.resolveActivity(pm) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(context, fileUri.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun alert(err: Throwable?) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.error_title)
            .setIcon(R.drawable.ic_dialog)
            .setMessage(R.string.error_export)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    companion object {
        const val EXTRA_CALLER = TrackerFragment.EXTRA_CALLER
        const val EXTRA_FILTER = "filter"

        private const val CHILD_LIST = 0
        private const val CHILD_EMPTY = 1
    }
}
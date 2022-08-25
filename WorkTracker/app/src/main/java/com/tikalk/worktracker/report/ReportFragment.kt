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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isNavDestination
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.databinding.FragmentReportListBinding
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportPage
import com.tikalk.worktracker.model.time.ReportTotals
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.time.formatCurrency
import com.tikalk.worktracker.time.formatElapsedTime
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.util.Formatter
import java.util.Locale

class ReportFragment : InternetFragment() {

    private var _binding: FragmentReportListBinding? = null
    private val binding get() = _binding!!
    private val bindingTotals get() = binding.totals

    private val recordsData = MutableLiveData<List<TimeRecord>>()
    private val totalsData = MutableLiveData<ReportTotals>()
    private val filterData = MutableLiveData<ReportFilter>()
    private var listAdapter = ReportAdapter(ReportFilter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        recordsData.observe(this) { records ->
            bindList(records)
        }
        totalsData.observe(this) { totals ->
            bindTotals(totals)
        }
        filterData.observe(this) { filter ->
            this.listAdapter = ReportAdapter(filter)
            binding.list.adapter = listAdapter
        }
        delegate.login.observe(this) { (_, reason) ->
            if (reason == null) {
                Timber.i("login success")
                run()
            } else {
                Timber.e("login failure: $reason")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.adapter = listAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @MainThread
    private fun bindList(records: List<TimeRecord>) {
        if (!isVisible) return
        listAdapter.submitList(records)
        if (records === recordsData.value) {
            listAdapter.notifyDataSetChanged()
        }
        if (records.isNotEmpty()) {
            binding.listSwitcher.displayedChild = CHILD_LIST
        } else {
            binding.listSwitcher.displayedChild = CHILD_EMPTY
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
            timeBuffer.clear()
            bindingTotals.durationTotalLabel.visibility = View.VISIBLE
            bindingTotals.durationTotal.text =
                formatElapsedTime(context, timeFormatter, totals.duration).toString()
        } else {
            bindingTotals.durationTotalLabel.visibility = View.INVISIBLE
            bindingTotals.durationTotal.text = null
        }
        if (filter?.showCostField == true) {
            timeBuffer.clear()
            bindingTotals.costTotalLabel.visibility = View.VISIBLE
            bindingTotals.costTotal.text = formatCurrency(currencyFormatter, totals.cost).toString()
        } else {
            bindingTotals.costTotalLabel.visibility = View.INVISIBLE
            bindingTotals.costTotal.text = null
        }
    }

    override fun authenticate(submit: Boolean) {
        val navController = findNavController()
        Timber.i("authenticate submit=$submit currentDestination=${navController.currentDestination?.label}")
        if (!isNavDestination(R.id.loginFragment)) {
            Bundle().apply {
                putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
                navController.navigate(R.id.action_reportList_to_login, this)
            }
        }
    }

    @MainThread
    fun run() {
        Timber.i("run first=$firstRun")

        var filter: ReportFilter? = null

        val args = arguments
        if (args != null) {
            if (args.containsKey(EXTRA_FILTER)) {
                val filterExtra = args.getParcelable<ReportFilter?>(EXTRA_FILTER)
                if (filterExtra != null) {
                    filter = filterExtra
                }
            }
        }
        if (filter == null) {
            filter = filterData.value ?: ReportFilter()
        }

        delegate.dataSource.reportPage(filter, firstRun)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.report, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_export_csv -> {
                exportCSV(item, false)
                return true
            }
            R.id.menu_export_html -> {
                exportHTML(item, false)
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
            R.id.menu_preview_csv -> {
                exportCSV(item, preview = true)
                return true
            }
            R.id.menu_preview_html -> {
                exportHTML(item, preview = true)
                return true
            }
            R.id.menu_preview_odf -> {
                exportODF(item, preview = true)
                return true
            }
            R.id.menu_preview_xml -> {
                exportXML(item, preview = true)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun export(
        context: Context,
        item: MenuItem? = null,
        exporter: ReportExporter,
        preview: Boolean = false
    ) {
        item?.isEnabled = false
        showProgress(true)

        exporter
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ uri ->
                Timber.i("Exported to $uri")
                if (preview) {
                    previewFile(context, uri, exporter.mimeType)
                } else {
                    shareFile(context, uri, exporter.mimeType)
                }
                showProgress(false)
                item?.isEnabled = true
            }, { err ->
                Timber.e(err, "Error exporting: ${err.message}")
                showProgress(false)
                item?.isEnabled = true
            })
            .addTo(disposables)
    }

    private fun exportCSV(item: MenuItem? = null, preview: Boolean = false) {
        val context = this.context ?: return
        val records = recordsData.value ?: return
        val filter = filterData.value ?: return
        val totals = totalsData.value ?: return

        export(
            context,
            item,
            ReportExporterCSV(context, records, filter, totals),
            preview
        )
    }

    private fun exportHTML(item: MenuItem? = null, preview: Boolean = false) {
        val context = this.context ?: return
        val records = recordsData.value ?: return
        val filter = filterData.value ?: return
        val totals = totalsData.value ?: return

        export(
            context,
            item,
            ReportExporterHTML(context, records, filter, totals),
            preview
        )
    }

    private fun exportODF(item: MenuItem? = null, preview: Boolean = false) {
        val context = this.context ?: return
        val records = recordsData.value ?: return
        val filter = filterData.value ?: return
        val totals = totalsData.value ?: return

        export(
            context,
            item,
            ReportExporterODF(context, records, filter, totals),
            preview
        )
    }

    private fun exportXML(item: MenuItem? = null, preview: Boolean = false) {
        val context = this.context ?: return
        val records = recordsData.value ?: return
        val filter = filterData.value ?: return
        val totals = totalsData.value ?: return

        export(
            context,
            item,
            ReportExporterXML(context, records, filter, totals),
            preview
        )
    }

    private fun shareFile(context: Context, fileUri: Uri, mimeType: String? = null) {
        val intent = ShareCompat.IntentBuilder(context)
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

    private fun showError(error: Throwable) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.error_title)
            .setIcon(R.drawable.ic_dialog)
            .setMessage(R.string.error_export)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun previewFile(context: Context, fileUri: Uri, mimeType: String? = null) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(fileUri, mimeType ?: context.contentResolver.getType(fileUri))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Validate that the device can open your File!
        val pm = context.packageManager
        if (intent.resolveActivity(pm) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(context, fileUri.toString(), Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_FILTER = "filter"

        private const val CHILD_LIST = 0
        private const val CHILD_EMPTY = 1
    }
}
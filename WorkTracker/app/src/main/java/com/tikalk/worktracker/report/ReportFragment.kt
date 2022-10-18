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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
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
        requireActivity().addMenuProvider(this, this, Lifecycle.State.RESUMED)
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
                val filterExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    args.getParcelable(EXTRA_FILTER, ReportFilter::class.java)
                } else {
                    args.getParcelable(EXTRA_FILTER)
                }
                if (filterExtra != null) {
                    filter = filterExtra
                }
            }
        }
        if (filter == null) {
            filter = filterData.value ?: ReportFilter()
        }

        lifecycleScope.launch {
            try {
                delegate.dataSource.reportPage(filter, firstRun)
                    .flowOn(Dispatchers.IO)
                    .collect { page ->
                        processPage(page)
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error loading page: ${e.message}")
                handleError(e)
            }
        }
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

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.report, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_export_csv -> {
                exportCSV(menuItem, false)
                return true
            }
            R.id.menu_export_html -> {
                exportHTML(menuItem, false)
                return true
            }
            R.id.menu_export_odf -> {
                exportODF(menuItem)
                return true
            }
            R.id.menu_export_xml -> {
                exportXML(menuItem)
                return true
            }
            R.id.menu_preview_csv -> {
                exportCSV(menuItem, preview = true)
                return true
            }
            R.id.menu_preview_html -> {
                exportHTML(menuItem, preview = true)
                return true
            }
            R.id.menu_preview_odf -> {
                exportODF(menuItem, preview = true)
                return true
            }
            R.id.menu_preview_xml -> {
                exportXML(menuItem, preview = true)
                return true
            }
        }
        return super.onMenuItemSelected(menuItem)
    }

    private fun export(
        context: Context,
        menuItem: MenuItem,
        exporter: ReportExporter,
        preview: Boolean = false
    ) {
        menuItem.isEnabled = false
        showProgress(true)

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                exporter
                    .flowOn(Dispatchers.IO)
                    .collect { uri ->
                        Timber.i("Exported to $uri")
                        menuItem.isEnabled = true
                        if (preview) {
                            previewFile(context, menuItem, uri, exporter.mimeType)
                        } else {
                            shareFile(context, menuItem, uri, exporter.mimeType)
                        }
                        showProgress(false)
                    }
            } catch (err: Exception) {
                Timber.e(err, "Error exporting: ${err.message}")
                showProgress(false)
                menuItem.isEnabled = true
            }
        }
    }

    private fun exportCSV(menuItem: MenuItem, preview: Boolean = false) {
        val context = this.context ?: return
        val records = recordsData.value ?: return
        val filter = filterData.value ?: return
        val totals = totalsData.value ?: return

        export(
            context,
            menuItem,
            ReportExporterCSV(context, records, filter, totals),
            preview
        )
    }

    private fun exportHTML(menuItem: MenuItem, preview: Boolean = false) {
        val context = this.context ?: return
        val records = recordsData.value ?: return
        val filter = filterData.value ?: return
        val totals = totalsData.value ?: return

        export(
            context,
            menuItem,
            ReportExporterHTML(context, records, filter, totals),
            preview
        )
    }

    private fun exportODF(menuItem: MenuItem, preview: Boolean = false) {
        val context = this.context ?: return
        val records = recordsData.value ?: return
        val filter = filterData.value ?: return
        val totals = totalsData.value ?: return

        export(
            context,
            menuItem,
            ReportExporterODF(context, records, filter, totals),
            preview
        )
    }

    private fun exportXML(menuItem: MenuItem, preview: Boolean = false) {
        val context = this.context ?: return
        val records = recordsData.value ?: return
        val filter = filterData.value ?: return
        val totals = totalsData.value ?: return

        export(
            context,
            menuItem,
            ReportExporterXML(context, records, filter, totals),
            preview
        )
    }

    private fun shareFile(
        context: Context,
        menuItem: MenuItem,
        fileUri: Uri,
        mimeType: String? = null
    ) {
        val intent = ShareCompat.IntentBuilder(context)
            .addStream(fileUri)
            .setType(mimeType ?: context.contentResolver.getType(fileUri))
            .intent
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            menuItem.isEnabled = false
            showError(e)
        }
    }

    private fun showError(error: Throwable) {
        Timber.e(error)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.error_title)
            .setIcon(R.drawable.ic_dialog)
            .setMessage(R.string.error_export)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun previewFile(
        context: Context,
        menuItem: MenuItem,
        fileUri: Uri,
        mimeType: String? = null
    ) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(fileUri, mimeType ?: context.contentResolver.getType(fileUri))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            menuItem.isEnabled = false
            showError(e)
        }
    }

    companion object {
        const val EXTRA_FILTER = "filter"

        private const val CHILD_LIST = 0
        private const val CHILD_EMPTY = 1
    }
}
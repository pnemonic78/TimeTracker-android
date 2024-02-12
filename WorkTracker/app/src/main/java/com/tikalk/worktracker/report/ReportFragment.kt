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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.core.app.ShareCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isDestination
import com.tikalk.compose.TikalTheme
import com.tikalk.core.databinding.FragmentComposeBinding
import com.tikalk.util.getParcelableCompat
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportPage
import com.tikalk.worktracker.model.time.ReportTotals
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.time.TimeEditFragment
import com.tikalk.worktracker.time.TimeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber

class ReportFragment : InternetFragment() {

    override val viewModel by viewModels<ReportViewModel>()
    private val timeViewModel by activityViewModels<TimeViewModel>()

    private var _binding: FragmentComposeBinding? = null
    private val binding get() = _binding!!

    private val recordsData = MutableStateFlow<List<TimeRecord>>(emptyList())
    private val totalsData = MutableStateFlow(ReportTotals())
    private val filterData = MutableStateFlow(ReportFilter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.onEdit.collect { record ->
                if (record != null) {
                    viewModel.clearEvents()
                    editRecord(record)
                }
            }
        }
        lifecycleScope.launch {
            timeViewModel.edited.collect { data ->
                if (data != null) onRecordEditSubmitted(data.record)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComposeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.composeView.setContent {
            TikalTheme {
                ReportResults(
                    itemsFlow = recordsData,
                    filterFlow = filterData,
                    totalsFlow = totalsData,
                    onClick = ::onRecordClick
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun authenticate(submit: Boolean) {
        val navController = findNavController()
        Timber.i("authenticate submit=$submit currentDestination=${navController.currentDestination?.label}")
        if (!navController.isDestination(R.id.loginFragment)) {
            Bundle().apply {
                putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
                navController.navigate(R.id.action_reportList_to_login, this)
            }
        }
    }

    @MainThread
    override fun run() {
        Timber.i("run first=$firstRun")

        var filter: ReportFilter? = null

        val args = arguments
        if (args != null) {
            if (args.containsKey(EXTRA_FILTER)) {
                val filterExtra = args.getParcelableCompat<ReportFilter>(EXTRA_FILTER)
                if (filterExtra != null) {
                    filter = filterExtra
                }
            }
        }
        if (filter == null) {
            filter = filterData.value
        }

        lifecycleScope.launch {
            try {
                viewModel.reportPage(filter, firstRun)
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

    private suspend fun processPage(page: ReportPage) {
        filterData.emit(page.filter)
        recordsData.emit(page.records)
        totalsData.emit(page.totals)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        val parentView = view?.parent as? View
        if (parentView?.parent == null) return
        if (parentView.id != R.id.nav_host_report) {
            menu.clear()
        }
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

    @MainThread
    private suspend fun export(
        context: Context,
        menuItem: MenuItem,
        exporter: ReportExporter,
        preview: Boolean = false
    ) {
        menuItem.isEnabled = false
        showProgress(true)

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

    private fun exportCSV(menuItem: MenuItem, preview: Boolean = false) {
        val context = this.context ?: return
        val records = recordsData.value
        val filter = filterData.value
        val totals = totalsData.value

        lifecycleScope.launch {
            export(
                context,
                menuItem,
                ReportExporterCSV(context, records, filter, totals),
                preview
            )
        }
    }

    private fun exportHTML(menuItem: MenuItem, preview: Boolean = false) {
        val context = this.context ?: return
        val records = recordsData.value
        val filter = filterData.value
        val totals = totalsData.value

        lifecycleScope.launch {
            export(
                context,
                menuItem,
                ReportExporterHTML(context, records, filter, totals),
                preview
            )
        }
    }

    private fun exportODF(menuItem: MenuItem, preview: Boolean = false) {
        val context = this.context ?: return
        val records = recordsData.value
        val filter = filterData.value
        val totals = totalsData.value

        lifecycleScope.launch {
            export(
                context,
                menuItem,
                ReportExporterODF(context, records, filter, totals),
                preview
            )
        }
    }

    private fun exportXML(menuItem: MenuItem, preview: Boolean = false) {
        val context = this.context ?: return
        val records = recordsData.value
        val filter = filterData.value
        val totals = totalsData.value

        lifecycleScope.launch {
            export(
                context,
                menuItem,
                ReportExporterXML(context, records, filter, totals),
                preview
            )
        }
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
            Timber.e(e)
            menuItem.isEnabled = false
            showError(R.string.error_export)
        }
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
            Timber.e(e)
            menuItem.isEnabled = false
            showError(R.string.error_export)
        }
    }

    private fun onRecordClick(record: TimeRecord) {
        viewModel.maybeEditRecord(lifecycleScope, record)
    }

    private fun editRecord(record: TimeRecord) {
        val navController = findNavController()
        Timber.i("editRecord record=$record currentDestination=${navController.currentDestination?.label}")
        if (!navController.isDestination(R.id.reportFragment)) return
        Bundle().apply {
            putLong(TimeEditFragment.EXTRA_DATE, record.dateTime)
            putLong(TimeEditFragment.EXTRA_DURATION, record.duration)
            putLong(TimeEditFragment.EXTRA_FINISH_TIME, record.finishTime)
            putLong(TimeEditFragment.EXTRA_PROJECT_ID, record.project.id)
            putLong(TimeEditFragment.EXTRA_RECORD_ID, record.id)
            putLong(TimeEditFragment.EXTRA_START_TIME, record.startTime)
            putLong(TimeEditFragment.EXTRA_TASK_ID, record.task.id)
            navController.navigate(R.id.action_reportList_to_timeEdit, this)
        }
    }

    private fun onRecordEditSubmitted(record: TimeRecord) {
        Timber.i("record submitted: $record")
        if (record.id == TikalEntity.ID_NONE) return
        // Remove the "edit form" and update the list.
        val navController = findNavController()
        navController.popBackStack()
        delegate.markFirst()
        run()
    }

    companion object {
        const val EXTRA_FILTER = "filter"
    }
}
package com.example.nightbrate

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AdminUserManagementActivity : AppCompatActivity() {

    private lateinit var list: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var err: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var tvCount: TextView
    private lateinit var etSearch: TextInputEditText
    private lateinit var btnFilters: MaterialButton
    private lateinit var panelFilters: View
    private lateinit var spinnerRole: Spinner
    private lateinit var spinnerStatus: Spinner

    private val debounceHandler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null

    private var suppressSpinner = true
    private var filtersVisible = false

    private val roleValues by lazy { resources.getStringArray(R.array.um_role_filter_values) }
    private val statusValues by lazy { resources.getStringArray(R.array.um_status_filter_values) }
    private var usersLoadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_user_management)
        AdminBottomBarHelper.bind(this, 1)

        list = findViewById(R.id.umgList)
        progress = findViewById(R.id.umgProgress)
        err = findViewById(R.id.tvUmgError)
        tvEmpty = findViewById(R.id.tvUmgEmpty)
        tvCount = findViewById(R.id.tvUmgCount)
        etSearch = findViewById(R.id.etUmgSearch)
        btnFilters = findViewById(R.id.btnUmgFilters)
        panelFilters = findViewById(R.id.panelUmgFilters)
        spinnerRole = findViewById(R.id.spinnerUmgRole)
        spinnerStatus = findViewById(R.id.spinnerUmgStatus)

        setupSpinners()
        setupSearchDebounce()
        btnFilters.setOnClickListener { toggleFilters() }
        updateFilterButtonStyle()

        refreshAll()
    }

    private fun setupSearchDebounce() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
                debounceRunnable = Runnable { refreshUsers() }
                debounceHandler.postDelayed(debounceRunnable!!, 350)
            }
        })
    }

    private fun setupSpinners() {
        ArrayAdapter.createFromResource(
            this,
            R.array.um_role_filter_labels,
            android.R.layout.simple_spinner_dropdown_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRole.adapter = adapter
        }
        ArrayAdapter.createFromResource(
            this,
            R.array.um_status_filter_labels,
            android.R.layout.simple_spinner_dropdown_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerStatus.adapter = adapter
        }

        val sel = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSpinner) return
                refreshUsers()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerRole.onItemSelectedListener = sel
        spinnerStatus.onItemSelectedListener = sel
        suppressSpinner = false
    }

    private fun toggleFilters() {
        filtersVisible = !filtersVisible
        panelFilters.visibility = if (filtersVisible) View.VISIBLE else View.GONE
        updateFilterButtonStyle()
    }

    private fun updateFilterButtonStyle() {
        val green = ContextCompat.getColor(this, R.color.brand)
        val white = ContextCompat.getColor(this, R.color.white)
        val inactiveBg = ContextCompat.getColor(this, R.color.um_filter_inactive_bg)
        val onInactive = ContextCompat.getColor(this, R.color.um_filter_inactive_text)
        if (filtersVisible) {
            btnFilters.backgroundTintList = ColorStateList.valueOf(green)
            btnFilters.setTextColor(white)
        } else {
            btnFilters.backgroundTintList = ColorStateList.valueOf(inactiveBg)
            btnFilters.setTextColor(onInactive)
        }
    }

    private fun refreshAll() {
        lifecycleScope.launch {
            refreshStats()
            refreshUsers()
        }
    }

    private suspend fun refreshStats() {
        try {
            val statsR = RetrofitClient.instance.getUserManagementStats()
            if (statsR.isSuccessful) {
                bindAllStats(statsR.body())
            }
        } catch (_: Exception) { }
    }

    private fun bindAllStats(s: UserManagementStatsResponse?) {
        val strong = ContextCompat.getColor(this, R.color.admin_strong)
        val amber = ContextCompat.getColor(this, R.color.um_stat_amber)
        val emerald = ContextCompat.getColor(this, R.color.um_stat_emerald)
        bindStatCell(R.id.cellStatTotal, "Toplam Kullanıcı", s?.totalUsers?.toString(), strong)
        bindStatCell(R.id.cellStatAdmin, "Admin", s?.admins?.toString(), amber)
        bindStatCell(R.id.cellStatDietitian, "Diyetisyen", s?.dietitians?.toString(), emerald)
        bindStatCell(R.id.cellStatClient, "Danışan", s?.clients?.toString(), emerald)
        bindStatCell(R.id.cellStatActive, "Aktif", s?.active?.toString(), emerald)
        bindStatCell(R.id.cellStatPending, "Bekleyen", s?.pending?.toString(), amber)
    }

    private fun bindStatCell(cellId: Int, label: String, value: String?, valueColor: Int) {
        val root = findViewById<View>(cellId)
        root.findViewById<TextView>(R.id.umStatLabel).text = label
        val tv = root.findViewById<TextView>(R.id.umStatValue)
        tv.text = value ?: "—"
        tv.setTextColor(valueColor)
    }

    private fun refreshUsers() {
        usersLoadJob?.cancel()
        usersLoadJob = lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            err.visibility = View.GONE
            tvEmpty.visibility = View.GONE
            try {
                val q = etSearch.text?.toString()?.trim().orEmpty()
                val rolePos = spinnerRole.selectedItemPosition.coerceAtLeast(0)
                val statusPos = spinnerStatus.selectedItemPosition.coerceAtLeast(0)
                val role = roleValues.getOrNull(rolePos)?.takeIf { it != "all" }
                val status = statusValues.getOrNull(statusPos)?.takeIf { it != "all" }

                val uR = RetrofitClient.instance.getUserManagementUsers(
                    q.ifEmpty { null },
                    role,
                    status
                )
                progress.visibility = View.GONE
                if (!uR.isSuccessful) {
                    err.text = readErrorMessage(uR)
                    err.visibility = View.VISIBLE
                    list.removeAllViews()
                    tvCount.text = ""
                    return@launch
                }
                val body = uR.body() ?: emptyList()
                list.removeAllViews()
                tvCount.text = if (body.isEmpty()) {
                    "0 kullanıcı bulundu"
                } else {
                    "${body.size} kullanıcı bulundu"
                }
                if (body.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                }
                for (item in body) {
                    addCard(item)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                progress.visibility = View.GONE
                err.text = e.message ?: "Bağlantı hatası"
                err.visibility = View.VISIBLE
                list.removeAllViews()
                tvCount.text = ""
            }
        }
    }

    private fun readErrorMessage(response: Response<*>): String {
        val raw = response.errorBody()?.string().orEmpty()
        return try {
            JSONObject(raw).optString("message").ifBlank { "HTTP ${response.code()}" }
        } catch (_: Exception) {
            if (raw.isNotBlank()) raw else "HTTP ${response.code()}"
        }
    }

    private fun addCard(u: AdminUserRowItem) {
        val v = layoutInflater.inflate(R.layout.item_admin_user_row, list, false)
        val initial = (u.initial?.take(1) ?: u.displayName?.take(1) ?: "?").uppercase()
        v.findViewById<TextView>(R.id.auAvatar).text = initial
        v.findViewById<TextView>(R.id.auName).text = u.displayName.orEmpty().ifBlank { "—" }
        v.findViewById<TextView>(R.id.auEmail).text = buildString {
            append("✉ ")
            append(u.email.orEmpty().ifBlank { "—" })
        }
        val phoneTv = v.findViewById<TextView>(R.id.auPhone)
        val phone = u.phone?.trim().orEmpty()
        if (phone.isNotEmpty() && phone != "—") {
            phoneTv.visibility = View.VISIBLE
            phoneTv.text = "☎ $phone"
        } else {
            phoneTv.visibility = View.GONE
        }
        applyRoleChip(v.findViewById(R.id.auRole), u.roleKey, u.role)
        applyStatusChip(v.findViewById(R.id.auStatus), u.statusKey, u.statusLabel)

        v.findViewById<TextView>(R.id.auCreated).text = buildString {
            append("Kayıt: ")
            append(formatDateTr(u.createdAt))
        }
        v.findViewById<TextView>(R.id.auLastActivity).text = buildString {
            append("Son aktivite: ")
            append(formatTimeAgoTr(u.lastActivityAt))
        }

        val id = u.id
        val logBtn = v.findViewById<MaterialButton>(R.id.auLog)
        logBtn.setOnClickListener {
            if (id.isNullOrBlank()) return@setOnClickListener
            showActivityLogDialog(u.displayName.orEmpty(), id)
        }

        val blockBtn = v.findViewById<MaterialButton>(R.id.auBlock)
        if (u.isSuspended == true) {
            blockBtn.text = "Askıyı kaldır"
            blockBtn.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.brand)
            )
            blockBtn.setTextColor(ContextCompat.getColor(this, R.color.white))
            blockBtn.strokeWidth = 0
            blockBtn.setOnClickListener {
                if (id.isNullOrBlank()) return@setOnClickListener
                MaterialAlertDialogBuilder(this)
                    .setMessage("Bu kullanıcının askısını kaldırmak istiyor musunuz?")
                    .setNegativeButton("İptal", null)
                    .setPositiveButton("Evet") { _, _ ->
                        lifecycleScope.launch {
                            try {
                                val r = RetrofitClient.instance.unsuspendUser(id)
                                if (r.isSuccessful) {
                                    Toast.makeText(this@AdminUserManagementActivity, "Askı kaldırıldı", Toast.LENGTH_SHORT).show()
                                    refreshAll()
                                } else {
                                    Toast.makeText(
                                        this@AdminUserManagementActivity,
                                        readErrorMessage(r),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@AdminUserManagementActivity,
                                    e.message ?: "Hata",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    .show()
            }
        } else {
            blockBtn.text = "Askıya al"
            blockBtn.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.um_suspend_btn)
            )
            blockBtn.setTextColor(ContextCompat.getColor(this, R.color.white))
            blockBtn.setOnClickListener {
                if (id.isNullOrBlank()) return@setOnClickListener
                showSuspendDialog(u)
            }
        }
        list.addView(v)
    }

    private fun showSuspendDialog(u: AdminUserRowItem) {
        val id = u.id ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_suspend_user, null)
        val explain = dialogView.findViewById<TextView>(R.id.tvSuspendExplain)
        val raw = getString(
            R.string.um_suspend_explain,
            u.email.orEmpty().ifBlank { u.displayName.orEmpty() }
        )
        explain.text = HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val input = dialogView.findViewById<TextInputEditText>(R.id.etSuspendReason)
        val dlg = MaterialAlertDialogBuilder(this)
            .setTitle("Kullanıcıyı askıya al")
            .setView(dialogView)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Askıya al", null)
            .create()
        dlg.setOnShowListener {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val msg = input.text?.toString()?.trim().orEmpty()
                if (msg.isEmpty()) {
                    Toast.makeText(this, "Mesaj gerekli", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    try {
                        val r = RetrofitClient.instance.suspendUser(id, SetUserSuspensionRequest(msg))
                        if (r.isSuccessful) {
                            dlg.dismiss()
                            Toast.makeText(this@AdminUserManagementActivity, "Kullanıcı askıya alındı", Toast.LENGTH_SHORT).show()
                            refreshAll()
                        } else {
                            Toast.makeText(
                                this@AdminUserManagementActivity,
                                readErrorMessage(r),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@AdminUserManagementActivity,
                            e.message ?: "Hata",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        dlg.show()
    }

    private fun showActivityLogDialog(displayName: String, userId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_activity_logs, null)
        val title = dialogView.findViewById<TextView>(R.id.tvLogDialogTitle)
        val container = dialogView.findViewById<LinearLayout>(R.id.llUserLogs)
        val prog = dialogView.findViewById<ProgressBar>(R.id.progressUserLogs)
        title.text = getString(R.string.um_log_title, displayName.ifBlank { "—" })
        container.removeAllViews()

        val dlg = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.btnLogDialogClose).setOnClickListener { dlg.dismiss() }
        dlg.show()

        lifecycleScope.launch {
            prog.visibility = View.VISIBLE
            try {
                val r = RetrofitClient.instance.getUserActivityLogs(userId, 40)
                prog.visibility = View.GONE
                if (!r.isSuccessful) {
                    Toast.makeText(this@AdminUserManagementActivity, readErrorMessage(r), Toast.LENGTH_LONG).show()
                    dlg.dismiss()
                    return@launch
                }
                val items = r.body() ?: emptyList()
                if (items.isEmpty()) {
                    val tv = TextView(this@AdminUserManagementActivity).apply {
                        text = "Bu kullanıcı için kayıt yok."
                        setTextColor(ContextCompat.getColor(context, R.color.admin_muted))
                        textSize = 14f
                    }
                    container.addView(tv)
                    return@launch
                }
                for (a in items) {
                    val row = layoutInflater.inflate(R.layout.item_user_log_entry, container, false)
                    row.findViewById<TextView>(R.id.uleInitial).text =
                        (a.initial?.take(1) ?: "?").uppercase()
                    val meta = "${a.actorDisplayName.orEmpty()} · ${formatDateTimeTr(a.createdAt)}"
                    row.findViewById<TextView>(R.id.uleMeta).text = meta
                    row.findViewById<TextView>(R.id.uleDesc).text = a.description.orEmpty()
                    container.addView(row)
                }
            } catch (e: Exception) {
                prog.visibility = View.GONE
                Toast.makeText(this@AdminUserManagementActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show()
                dlg.dismiss()
            }
        }
    }

    private fun applyRoleChip(tv: TextView, roleKey: String?, roleLabel: String?) {
        tv.text = roleLabel ?: "—"
        if (roleKey == "admin") {
            tv.setBackgroundResource(R.drawable.um_chip_role_admin)
            tv.setTextColor(ContextCompat.getColor(this, R.color.um_chip_amber_text))
        } else {
            tv.setBackgroundResource(R.drawable.um_chip_role_default)
            tv.setTextColor(ContextCompat.getColor(this, R.color.um_chip_emerald_text))
        }
    }

    private fun applyStatusChip(tv: TextView, statusKey: String?, statusLabel: String?) {
        tv.text = statusLabel ?: "—"
        val bg = when (statusKey) {
            "suspended" -> R.drawable.um_chip_status_suspended
            "pending" -> R.drawable.um_chip_status_pending
            else -> R.drawable.um_chip_status_active
        }
        val col = when (statusKey) {
            "suspended" -> R.color.um_chip_red_text
            "pending" -> R.color.um_chip_amber_text
            else -> R.color.um_chip_emerald_text
        }
        tv.setBackgroundResource(bg)
        tv.setTextColor(ContextCompat.getColor(this, col))
    }

    private fun formatDateTr(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        val ms = parseIsoToMillis(iso) ?: return "—"
        return SimpleDateFormat("d.MM.yyyy", Locale("tr", "TR")).format(Date(ms))
    }

    private fun formatDateTimeTr(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        val ms = parseIsoToMillis(iso) ?: return "—"
        return SimpleDateFormat("d.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(ms))
    }

    private fun formatTimeAgoTr(createdAt: String?): String {
        val then = parseIsoToMillis(createdAt) ?: return "—"
        val diff = System.currentTimeMillis() - then
        if (diff < 0) return "Az önce"
        val s = diff / 1000
        if (s < 60) return "Az önce"
        val m = s / 60
        if (m < 60) return "$m dk önce"
        val h = m / 60
        if (h < 24) return "$h saat önce"
        val days = h / 24
        if (days < 7) return "$days gün önce"
        return SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(Date(then))
    }

    private fun parseIsoToMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val tries = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        )
        for (fmt in tries) {
            try {
                val d = fmt.parse(raw) ?: continue
                return d.time
            } catch (_: Exception) { }
        }
        return null
    }
}

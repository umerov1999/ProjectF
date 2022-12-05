package dev.ragnarok.fenrir.fragment.search.filteredit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.dialog.selectchairs.SelectChairsDialog
import dev.ragnarok.fenrir.dialog.selectcity.SelectCityDialog
import dev.ragnarok.fenrir.dialog.selectcountry.SelectCountryDialog
import dev.ragnarok.fenrir.dialog.selectfaculty.SelectFacultyDialog
import dev.ragnarok.fenrir.dialog.selectschoolclasses.SelectSchoolClassesDialog
import dev.ragnarok.fenrir.dialog.selectschools.SelectSchoolsDialog
import dev.ragnarok.fenrir.dialog.selectuniversity.SelectUniversityDialog
import dev.ragnarok.fenrir.fragment.search.filteredit.SearchOptionsAdapter.OptionClickListener
import dev.ragnarok.fenrir.fragment.search.options.*
import dev.ragnarok.fenrir.getParcelableArrayListCompat
import dev.ragnarok.fenrir.trimmedIsNullOrEmpty
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.InputTextDialog
import dev.ragnarok.fenrir.util.Utils.hasMarshmallow
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.DateTimePicker
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.*

class FilterEditFragment : BottomSheetDialogFragment(), OptionClickListener {
    private val mCompositeDisposable = CompositeDisposable()
    private val requestGPSPermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ) {
        createCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text)
    }
    private var mData: ArrayList<BaseOption>? = null
    private var mAdapter: SearchOptionsAdapter? = null
    private var mAccountId = 0
    private var mEmptyText: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAccountId = requireArguments().getInt(Extra.ACCOUNT_ID)
        mData = requireArguments().getParcelableArrayListCompat(Extra.LIST)
    }

    private fun resolveEmptyTextVisibility() {
        mEmptyText?.visibility =
            if (mData.isNullOrEmpty()) View.VISIBLE else View.GONE
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val root = View.inflate(requireActivity(), R.layout.sheet_filter_edirt, null)
        val toolbar: Toolbar = root.findViewById(R.id.toolbar)
        toolbar.setTitle(R.string.search_options)
        toolbar.setNavigationIcon(R.drawable.check)
        toolbar.setNavigationOnClickListener { onSaveClick() }
        mEmptyText = root.findViewById(R.id.empty_text)
        val mRecyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        val manager: RecyclerView.LayoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        mRecyclerView.layoutManager = manager
        mAdapter = SearchOptionsAdapter(mData ?: Collections.emptyList())
        mAdapter?.setOptionClickListener(this)
        mRecyclerView.adapter = mAdapter
        resolveEmptyTextVisibility()
        dialog.setContentView(root)
        parentFragmentManager.setFragmentResultListener(
            REQUEST_FILTER_OPTION,
            this
        ) { _: String?, result: Bundle ->
            val key = result.getInt(Extra.KEY)
            val id = if (result.containsKey(Extra.ID)) result.getInt(Extra.ID) else null
            val title = if (result.containsKey(Extra.TITLE)) result.getString(Extra.TITLE) else null
            mergeDatabaseOptionValue(key, if (id == null) null else DatabaseOption.Entry(id, title))
        }
    }

    private fun onSaveClick() {
        val data = Bundle()
        data.putParcelableArrayList(Extra.LIST, mData)
        parentFragmentManager.setFragmentResult(REQUEST_FILTER_EDIT, data)
        dismiss()
    }

    override fun onSpinnerOptionClick(spinnerOption: SpinnerOption) {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(spinnerOption.title)
            .setItems(spinnerOption.createAvailableNames(requireActivity())) { _: DialogInterface?, which: Int ->
                spinnerOption.value = spinnerOption.available[which]
                mAdapter?.notifyDataSetChanged()
            }
            .setNegativeButton(R.string.clear) { _: DialogInterface?, _: Int ->
                spinnerOption.value = null
                mAdapter?.notifyDataSetChanged()
            }
            .setPositiveButton(R.string.button_cancel, null)
            .show()
    }

    private fun mergeDatabaseOptionValue(key: Int, value: DatabaseOption.Entry?) {
        val hData = mData
        hData ?: return
        for (option in hData) {
            if (option.key == key && option is DatabaseOption) {
                option.value = value
                option.childDependencies?.let { resetChildDependents(*it) }
                mAdapter?.notifyDataSetChanged()
                break
            }
        }
    }

    internal fun mergeGPSOptionValue(value: SimpleGPSOption) {
        val hData = mData
        hData ?: return
        for (option in hData) {
            if (option.key == value.key && option is SimpleGPSOption) {
                option.lat_gps = value.lat_gps
                option.long_gps = value.long_gps
                mAdapter?.notifyDataSetChanged()
                break
            }
        }
    }

    internal fun mergeDateOptionValue(value: SimpleDateOption) {
        val hData = mData
        hData ?: return
        for (option in hData) {
            if (option.key == value.key && option is SimpleDateOption) {
                option.timeUnix = value.timeUnix
                mAdapter?.notifyDataSetChanged()
                break
            }
        }
    }

    private fun resetChildDependents(vararg children: Int) {
        val hData = mData
        hData ?: return
        var changed = false
        for (key in children) {
            for (option in hData) {
                if (option.key == key) {
                    option.reset()
                    changed = true
                }
            }
        }
        if (changed) {
            mAdapter?.notifyDataSetChanged()
        }
    }

    override fun onDatabaseOptionClick(databaseOption: DatabaseOption) {
        val dependency = findDependencyByKey(databaseOption.parentDependencyKey)
        when (databaseOption.type) {
            DatabaseOption.TYPE_COUNTRY -> {
                val selectCountryDialog = SelectCountryDialog()
                val args = Bundle()
                args.putInt(Extra.KEY, databaseOption.key)
                args.putInt(Extra.ACCOUNT_ID, mAccountId)
                selectCountryDialog.arguments = args
                selectCountryDialog.show(parentFragmentManager, "countries")
            }
            DatabaseOption.TYPE_CITY -> if (dependency is DatabaseOption && dependency.value != null) {
                val countryId = (dependency.value ?: return).id
                showCitiesDialog(databaseOption, countryId)
            } else {
                val message = getString(
                    R.string.please_select_option, getString(
                        (dependency ?: return).title
                    )
                )
                createCustomToast(requireActivity()).setDuration(Toast.LENGTH_SHORT)
                    .showToastInfo(message)
            }
            DatabaseOption.TYPE_UNIVERSITY -> if (dependency is DatabaseOption && dependency.value != null) {
                val countryId = (dependency.value ?: return).id
                showUniversitiesDialog(databaseOption, countryId)
            } else {
                val message = getString(
                    R.string.please_select_option, getString(
                        (dependency ?: return).title
                    )
                )
                createCustomToast(requireActivity()).setDuration(Toast.LENGTH_SHORT)
                    .showToastInfo(message)
            }
            DatabaseOption.TYPE_FACULTY -> if (dependency is DatabaseOption && dependency.value != null) {
                val universityId = (dependency.value ?: return).id
                showFacultiesDialog(databaseOption, universityId)
            } else {
                val message = getString(
                    R.string.please_select_option, getString(
                        (dependency ?: return).title
                    )
                )
                createCustomToast(requireActivity()).setDuration(Toast.LENGTH_SHORT)
                    .showToastInfo(message)
            }
            DatabaseOption.TYPE_CHAIR -> if (dependency is DatabaseOption && dependency.value != null) {
                val facultyId = (dependency.value ?: return).id
                showChairsDialog(databaseOption, facultyId)
            } else {
                val message = getString(
                    R.string.please_select_option, getString(
                        (dependency ?: return).title
                    )
                )
                createCustomToast(requireActivity()).setDuration(Toast.LENGTH_SHORT)
                    .showToastInfo(message)
            }
            DatabaseOption.TYPE_SCHOOL -> if (dependency is DatabaseOption && dependency.value != null) {
                val cityId = (dependency.value ?: return).id
                showSchoolsDialog(databaseOption, cityId)
            } else {
                val message = getString(
                    R.string.please_select_option, getString(
                        (dependency ?: return).title
                    )
                )
                createCustomToast(requireActivity()).setDuration(Toast.LENGTH_SHORT)
                    .showToastInfo(message)
            }
            DatabaseOption.TYPE_SCHOOL_CLASS -> if (dependency is DatabaseOption && dependency.value != null) {
                val countryId = (dependency.value ?: return).id
                showSchoolClassesDialog(databaseOption, countryId)
            } else {
                val message = getString(
                    R.string.please_select_option, getString(
                        (dependency ?: return).title
                    )
                )
                createCustomToast(requireActivity()).setDuration(Toast.LENGTH_SHORT)
                    .showToastInfo(message)
            }
        }
    }

    override fun onSimpleNumberOptionClick(option: SimpleNumberOption) {
        InputTextDialog.Builder(requireActivity())
            .setTitleRes(option.title)
            .setAllowEmpty(true)
            .setInputType(InputType.TYPE_CLASS_NUMBER)
            .setValue(if (option.value == null) null else option.value.toString())
            .setCallback(object : InputTextDialog.Callback {
                override fun onChanged(newValue: String?) {
                    option.value = getIntFromEditable(newValue)
                    mAdapter?.notifyDataSetChanged()
                }

                override fun onCanceled() {
                }
            })
            .show()
    }

    internal fun getIntFromEditable(line: String?): Int? {
        return if (line == null || line.trimmedIsNullOrEmpty()) {
            null
        } else try {
            Integer.valueOf(line)
        } catch (ignored: NumberFormatException) {
            null
        }
    }

    override fun onSimpleTextOptionClick(option: SimpleTextOption) {
        InputTextDialog.Builder(requireActivity())
            .setTitleRes(option.title)
            .setInputType(InputType.TYPE_CLASS_TEXT)
            .setValue(option.value)
            .setAllowEmpty(true)
            .setCallback(object : InputTextDialog.Callback {
                override fun onChanged(newValue: String?) {
                    option.value = newValue
                    mAdapter?.notifyDataSetChanged()
                }

                override fun onCanceled() {
                }
            })
            .show()
    }

    override fun onSimpleBooleanOptionChanged(option: SimpleBooleanOption) {}
    override fun onOptionCleared(option: BaseOption) {
        option.childDependencies?.let { resetChildDependents(*it) }
    }

    override fun onGPSOptionClick(gpsOption: SimpleGPSOption) {
        MyLocation().getLocation(requireActivity(), object : LocationResult {
            override fun gotLocation(location: Location?) {
                if (isAdded) {
                    val uiHandler = Handler(requireActivity().mainLooper)
                    uiHandler.post {
                        if (location != null) {
                            gpsOption.lat_gps = location.latitude
                            gpsOption.long_gps = location.longitude
                        }
                        mergeGPSOptionValue(gpsOption)
                    }
                }
            }
        })
    }

    override fun onDateOptionClick(dateOption: SimpleDateOption) {
        DateTimePicker.Builder(requireActivity())
            .setTime(if (dateOption.timeUnix == 0L) Calendar.getInstance().time.time / 1000 else dateOption.timeUnix)
            .setCallback(object : DateTimePicker.Callback {
                override fun onDateTimeSelected(unixtime: Long) {
                    dateOption.timeUnix = unixtime
                    mergeDateOptionValue(dateOption)
                }
            })
            .show()
    }

    private fun showCitiesDialog(databaseOption: DatabaseOption, cityId: Int) {
        val args = Bundle()
        args.putInt(Extra.KEY, databaseOption.key)
        val selectCityDialog = SelectCityDialog.newInstance(mAccountId, cityId, args)
        selectCityDialog.show(parentFragmentManager, "cities")
    }

    private fun showUniversitiesDialog(databaseOption: DatabaseOption, universityId: Int) {
        val args = Bundle()
        args.putInt(Extra.KEY, databaseOption.key)
        val dialog = SelectUniversityDialog.newInstance(mAccountId, universityId, args)
        dialog.show(parentFragmentManager, "universities")
    }

    private fun showSchoolsDialog(databaseOption: DatabaseOption, schoolsId: Int) {
        val args = Bundle()
        args.putInt(Extra.KEY, databaseOption.key)
        val dialog = SelectSchoolsDialog.newInstance(mAccountId, schoolsId, args)
        dialog.show(parentFragmentManager, "schools")
    }

    private fun showFacultiesDialog(databaseOption: DatabaseOption, facultiesId: Int) {
        val args = Bundle()
        args.putInt(Extra.KEY, databaseOption.key)
        val dialog = SelectFacultyDialog.newInstance(mAccountId, facultiesId, args)
        dialog.show(parentFragmentManager, "faculties")
    }

    private fun showChairsDialog(databaseOption: DatabaseOption, chairsId: Int) {
        val args = Bundle()
        args.putInt(Extra.KEY, databaseOption.key)
        val dialog = SelectChairsDialog.newInstance(mAccountId, chairsId, args)
        dialog.show(parentFragmentManager, "chairs")
    }

    private fun showSchoolClassesDialog(databaseOption: DatabaseOption, schoolClasses: Int) {
        val args = Bundle()
        args.putInt(Extra.KEY, databaseOption.key)
        val dialog = SelectSchoolClassesDialog.newInstance(mAccountId, schoolClasses, args)
        dialog.show(parentFragmentManager, "school-classes")
    }

    private fun findDependencyByKey(key: Int): BaseOption? {
        if (key == BaseOption.NO_DEPENDENCY) {
            return null
        }
        val hData = mData
        hData ?: return null
        for (baseOption in hData) {
            if (baseOption.key == key) {
                return baseOption
            }
        }
        return null
    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
    }

    interface LocationResult {
        fun gotLocation(location: Location?)
    }

    private inner class MyLocation {
        var timer1: Timer? = null
        var lm: LocationManager? = null
        var locationResult: LocationResult? = null
        val gpsLocation: LocationListener = object : LocationListener {
            @SuppressLint("MissingPermission")
            override fun onLocationChanged(location: Location) {
                timer1?.cancel()
                locationResult?.gotLocation(location)
                lm?.removeUpdates(this)
                lm?.removeUpdates(networkLocation)
            }

            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}

            @Deprecated("")
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            }
        }
        val networkLocation: LocationListener = object : LocationListener {
            @SuppressLint("MissingPermission")
            override fun onLocationChanged(location: Location) {
                timer1?.cancel()
                locationResult?.gotLocation(location)
                lm?.removeUpdates(this)
                lm?.removeUpdates(gpsLocation)
            }

            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}

            @Deprecated("")
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            }
        }
        var gps_enabled = false
        var network_enabled = false

        @SuppressLint("MissingPermission")
        fun getLocation(context: Context, result: LocationResult?): Boolean {
            if (hasMarshmallow() && ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestGPSPermission.launch()
                return false
            }
            // I use LocationResult callback class to pass location value from
            // MyLocation to user code.
            locationResult = result
            if (lm == null) lm = context
                .getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            lm?.let {
                // Exceptions will be thrown if the provider is not permitted.
                try {
                    gps_enabled = it.isProviderEnabled(LocationManager.GPS_PROVIDER)
                } catch (ignored: Exception) {
                }
                try {
                    network_enabled = it
                        .isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                } catch (ignored: Exception) {
                }

                // Don't start listeners if no provider is enabled.
                if (!gps_enabled && !network_enabled) return false
                if (gps_enabled) it.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0f,
                    gpsLocation
                )
                if (network_enabled) it.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0, 0f,
                    networkLocation
                )
                timer1 = Timer()
                timer1?.schedule(GetLastLocation(), 5000)
            }
            return true
        }

        inner class GetLastLocation : TimerTask() {
            @SuppressLint("MissingPermission")
            override fun run() {
                if (hasMarshmallow() && ContextCompat.checkSelfPermission(
                        requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                        requireActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestGPSPermission.launch()
                    return
                }
                lm?.removeUpdates(gpsLocation)
                lm?.removeUpdates(networkLocation)
                var net_loc: Location? = null
                var gps_loc: Location? = null
                if (gps_enabled) gps_loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (network_enabled) net_loc =
                    lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                // If there are both values, use the latest one.
                if (gps_loc != null && net_loc != null) {
                    if (gps_loc.time > net_loc.time) locationResult?.gotLocation(gps_loc) else locationResult?.gotLocation(
                        net_loc
                    )
                    return
                }
                if (gps_loc != null) {
                    locationResult?.gotLocation(gps_loc)
                    return
                }
                if (net_loc != null) {
                    locationResult?.gotLocation(net_loc)
                    return
                }
                locationResult?.gotLocation(null)
            }
        }
    }

    companion object {
        const val REQUEST_FILTER_EDIT = "request_filter_edit"
        const val REQUEST_FILTER_OPTION = "request_filter_option"
        fun newInstance(accountId: Int, options: ArrayList<BaseOption>): FilterEditFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putParcelableArrayList(Extra.LIST, options)
            val fragment = FilterEditFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
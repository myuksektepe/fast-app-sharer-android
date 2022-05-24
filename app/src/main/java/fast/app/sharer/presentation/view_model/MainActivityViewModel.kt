package fast.app.sharer.presentation.view_model

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fast.app.sharer.domain.model.InstalledAppModel
import fast.app.sharer.util.CustomComparator
import fast.app.sharer.util.ResultState
import fast.app.sharer.util.TAG
import fast.app.sharer.util.Util.isSystemPackage
import fast.app.sharer.util.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val application: Application
) : BaseViewModel() {

    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext
    private var appList = mutableListOf<InstalledAppModel>()

    private val _allApps = MutableLiveData<ResultState<MutableList<InstalledAppModel>>>()
    val allApps: LiveData<ResultState<MutableList<InstalledAppModel>>>
        get() = _allApps

    @SuppressLint("QueryPermissionsNeeded")
    fun fetchInstalledApps(showSystemApps: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _allApps.postValue(ResultState.LOADING())
            try {
                appList = getInstalledApps(showSystemApps)
                _allApps.postValue(ResultState.SUCCESS(appList))
            } catch (e: Exception) {
                _allApps.postValue(ResultState.FAIL(exception = e, errorMessage = e.message))
            }
        }
    }

    private fun getInstalledApps(showSystemApps: Boolean): MutableList<InstalledAppModel> {
        val listApps = mutableListOf<InstalledAppModel>()
        val pm = context.packageManager
        val apps = pm.getInstalledPackages(0)
        apps.forEachIndexed { index, packageInfo ->
            //val packageInfo: PackageInfo = context.packageManager.getPackageInfo(app.packageName, 0)
            val name = packageInfo.applicationInfo.loadLabel(context.packageManager).toString()
            val packageName = packageInfo.packageName
            val versionName = packageInfo.versionName
            val icon = packageInfo.applicationInfo.icon
            val iconDrawable = packageInfo.applicationInfo.loadIcon(context.packageManager)
            val isSystemApp = isSystemPackage(packageInfo)
            val sourceDir = packageInfo.applicationInfo.sourceDir
            val file = File(sourceDir)
            val size = file.length()

            val app = InstalledAppModel(name, packageName, versionName, icon, iconDrawable, isSystemApp, sourceDir, file, size)
            if (showSystemApps) {
                listApps.add(app)
                Log.i(TAG, app.name.toString())
            } else {
                if (!isSystemPackage(packageInfo)) {
                    listApps.add(app)
                    Log.i(TAG, app.name.toString())
                }
            }
        }
        Collections.sort(listApps, CustomComparator())
        return listApps
    }


}
package fast.app.sharer.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fast.app.sharer.R
import fast.app.sharer.adapter.AppsAdapter
import fast.app.sharer.model.InstalledAppModel
import fast.app.sharer.util.Constans
import fast.app.sharer.util.CustomComparator
import fast.app.sharer.util.Util
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    val PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.BLUETOOTH,
    )

    var rltLoading: RelativeLayout? = null
    var prgUploading: ProgressBar? = null
    var rltNoResult: RelativeLayout? = null
    var edtSearch: EditText? = null
    var rcyAppList: RecyclerView? = null
    var appsList: ArrayList<InstalledAppModel>? = null
    var appsAdapter: AppsAdapter? = null
    var menu: Menu? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        getWindow().getDecorView().setSystemUiVisibility(flags);
        val decorView = window.decorView
        decorView
            .setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    decorView.systemUiVisibility = flags
                }
            }
         */

        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.systemUiVisibility = uiOptions

        init()
        getInstalledApps()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun init() {
        appsList = ArrayList<InstalledAppModel>()
        rcyAppList = findViewById(R.id.rcyAppList)
        edtSearch = findViewById(R.id.edtSearch)
        rltNoResult = findViewById(R.id.rltNoResult)
        prgUploading = findViewById(R.id.prgUploading)
        rltLoading = findViewById(R.id.rltLoading)

        // Check Permission
        if (!Util().hasPermissions(this, *PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, Constans.PERMISSION_WRITE_REQUEST_CODE);
        }

        // Search
        edtSearch!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                appsAdapter!!.filter.filter(s.toString())
                Handler().postDelayed(
                    {
                        if (appsAdapter!!.list.size == 0) {
                            rcyAppList!!.visibility = View.GONE
                            rltNoResult!!.visibility = View.VISIBLE
                        } else {
                            rcyAppList!!.visibility = View.VISIBLE
                            rltNoResult!!.visibility = View.GONE
                        }
                    },
                    300
                )
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("QueryPermissionsNeeded", "WrongConstant")
    private fun getInstalledApps(showSystemApps: Boolean = false) {
        val uiScope = CoroutineScope(Dispatchers.Main)
        val bgDispatcher: CoroutineDispatcher = Dispatchers.IO
        uiScope.launch {

            // PreExecute
            rltLoading?.visibility = View.VISIBLE

            // DoInBackground
            withContext(bgDispatcher) {

                // Clean the App List
                appsList!!.clear()

                // Get installed apps with PackageManager
                val pm = packageManager
                val apps = pm.getInstalledPackages(0)
                for (app in apps) {
                    if (showSystemApps) {
                        addAppToList(app.applicationInfo)
                    } else {
                        if (!Util().isSystemPackage(app)) {
                            addAppToList(app.applicationInfo)
                        }
                    }
                }

            }

            // PostExecute
            setRecyclerView(appsList!!)
            rltLoading?.visibility = View.GONE
        }
    }

    private fun addAppToList(app: ApplicationInfo) {
        val packageInfo: PackageInfo = packageManager.getPackageInfo(app.packageName, 0)
        val name = app.loadLabel(packageManager).toString()
        val packageName = packageInfo.packageName
        val versionName = packageInfo.versionName
        val icon = packageInfo.applicationInfo.icon
        val iconDrawable = app.loadIcon(packageManager)
        val isSystemApp = Util().isSystemPackage(packageInfo)
        val sourceDir = packageInfo.applicationInfo.sourceDir
        val file = File(sourceDir)
        val size = file.length()

        Collections.sort(appsList, CustomComparator())

        appsList?.add(InstalledAppModel(name, packageName, versionName, icon, iconDrawable, isSystemApp, sourceDir, file, size))
        Log.i(Util.TAG, "App: ${packageInfo}")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setRecyclerView(_appsList: ArrayList<InstalledAppModel>) {
        if (_appsList.size > 0) {
            val mLayoutManager = GridLayoutManager(applicationContext, 1)
            mLayoutManager.reverseLayout = false
            rcyAppList!!.layoutManager = mLayoutManager

            appsAdapter = AppsAdapter(applicationContext, _appsList, 2)
            rcyAppList!!.adapter = appsAdapter

            appsAdapter!!.onItemClick = {
                Util().shareApp(this, it)
                //Util().uploadApkToYuklio(this, it)
            }
        } else {
            rltNoResult!!.visibility = View.VISIBLE
        }

        rltLoading!!.visibility = View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toogleSystemApps(boolean: Boolean) {
        if (boolean) {
            menu!!.setGroupVisible(R.id.menu_group_hide_system_apps, true)
            menu!!.setGroupVisible(R.id.menu_group_show_system_apps, false)
        } else {
            menu!!.setGroupVisible(R.id.menu_group_hide_system_apps, false)
            menu!!.setGroupVisible(R.id.menu_group_show_system_apps, true)
        }
        getInstalledApps(boolean)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Util.shareRequestCode -> {
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constans.PERMISSION_WRITE_REQUEST_CODE) {
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        val menuInflater: MenuInflater = getMenuInflater()
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_show_system_apps -> {
                toogleSystemApps(true)
            }
            R.id.menu_hide_system_apps -> {
                toogleSystemApps(false)
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Util().deleteRecursive(Util.appsFolder)
    }

    @SuppressLint("NewApi")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && hasFocus) {
            val decorView = window.decorView
            val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            decorView.systemUiVisibility = uiOptions
        }
    }
}
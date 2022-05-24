package fast.app.sharer.presentation.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import dagger.hilt.android.AndroidEntryPoint
import fast.app.sharer.R
import fast.app.sharer.databinding.ActivityMainBinding
import fast.app.sharer.domain.adapter.AppsAdapter
import fast.app.sharer.domain.model.InstalledAppModel
import fast.app.sharer.presentation.view_model.MainActivityViewModel
import fast.app.sharer.util.PERMISSIONS
import fast.app.sharer.util.ResultState
import fast.app.sharer.util.TAG
import fast.app.sharer.util.Util
import fast.app.sharer.util.Util.deleteRecursive
import fast.app.sharer.util.Util.shareApp
import fast.app.sharer.util.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity<MainActivityViewModel, ActivityMainBinding>() {

    override val layoutRes: Int = R.layout.activity_main
    override var viewLifecycleOwner: LifecycleOwner = this
    override val viewModel: MainActivityViewModel by viewModels()
    var appsAdapter = AppsAdapter(2)
    var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        viewModel.fetchInstalledApps(false)
    }

    private fun init() {
        // Check Permission
        //if (!checkPermissions()) requestPermissons()

        // Search
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.edtSearch.hasFocus()) {
                    appsAdapter.filter.filter(s.toString())
                    lifecycleScope.launch(Dispatchers.Main) {
                        delay(300)
                        if (appsAdapter.filteredList.size == 0) {
                            binding.rcyAppList.visibility = View.GONE
                            binding.rltNoResult.visibility = View.VISIBLE
                        } else {
                            binding.rcyAppList.visibility = View.VISIBLE
                            binding.rltNoResult.visibility = View.GONE
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setRecyclerView(appList: MutableList<InstalledAppModel>) {
        if (appList.size > 0) {
            appsAdapter.update(appList)
            binding.rcyAppList.apply {
                adapter = appsAdapter
                setHasFixedSize(false)
                layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
            }
            hideLoading()

            appsAdapter.onItemClick = {
                if (checkPermissions2()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            applicationContext.shareApp(it).collectLatest {
                                when (it) {
                                    is ResultState.LOADING -> {
                                        showLoading()
                                    }
                                    is ResultState.FAIL -> {
                                        hideLoading()
                                        showErrorDialog(message = it.exception?.message, buttonMessage = getString(R.string.try_again), callback = {})
                                    }
                                    is ResultState.SUCCESS -> {
                                        hideLoading()
                                        //this@MainActivity.startActivityForResult(it.data, Util.shareRequestCode)
                                        resultLauncher.launch(it.data)
                                    }
                                }
                            }
                        }
                    }
                    //uploadApkToYuklio(this, it)
                }
            }
        } else {
            binding.rltNoResult.visibility = View.VISIBLE
        }
        Log.i(TAG, "itemCount: ${appsAdapter.itemCount}")
    }

    private fun toogleSystemApps(boolean: Boolean) {
        if (boolean) {
            menu!!.setGroupVisible(R.id.menu_group_hide_system_apps, true)
            menu!!.setGroupVisible(R.id.menu_group_show_system_apps, false)
        } else {
            menu!!.setGroupVisible(R.id.menu_group_hide_system_apps, false)
            menu!!.setGroupVisible(R.id.menu_group_show_system_apps, true)
        }
        viewModel.fetchInstalledApps(boolean)
        binding.edtSearch.setText("")
        binding.edtSearch.clearFocus()
    }

    private fun checkPermissions2(): Boolean {
        val options: Permissions.Options = Permissions.Options()
            .setRationaleDialogTitle(getString(R.string.info))
            .setSettingsDialogTitle(getString(R.string.info))
            .setSettingsDialogMessage(getString(R.string.please_approve_permission_requests))
        val rationale = getString(R.string.please_approve_permission_requests)
        var result = false

        Permissions.check(this, PERMISSIONS, rationale, options, object : PermissionHandler() {
            override fun onGranted() {
                result = true
            }

            override fun onDenied(context: Context?, deniedPermissions: ArrayList<String?>?) {
                result = false
            }
        })
        return result
    }

    /*
    private fun checkPermissions(): Boolean {
        val isPermissionGranted = hasPermissions(this, *PERMISSIONS)
        Log.i(TAG, "isPermissionGranted: $isPermissionGranted")
        return isPermissionGranted
    }

    private fun requestPermissons() {
        try {
            requestPermissions(this@MainActivity, PERMISSIONS, PERMISSION_WRITE_REQUEST_CODE)
        } catch (e: Exception) {
            //showErrorDailog("Yetki Lazım", buttonMessage = "Ayarlara Git", callback = { this.openAppSetting() })
            val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
            val alertDialog: android.app.AlertDialog = builder
                .setCancelable(false)
                .setTitle("Yetki Lazım")
                .setMessage("Ayarlara Git")
                .setPositiveButton("Tamam") { dialog, which ->
                    this.openAppSetting()
                }
                .setNegativeButton("Hayır", null)
                .create()
            alertDialog.show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_WRITE_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.i(TAG, "Permissions are granted")
            } else {
                Log.i(TAG, "Permissions are NOT granted!!")
                //showErrorDailog(message = "Yetki lazım", buttonMessage = "Ayarlara Git", callback = {})
            }
        }
    }
     */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        val menuInflater: MenuInflater = getMenuInflater()
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

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

    override fun onResume() {
        super.onResume()
        binding.edtSearch.clearFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteRecursive(Util.appsFolder)
    }

    override fun observeViewModel() {
        viewModel.allApps.observe(viewLifecycleOwner) { resultState ->
            when (resultState) {
                is ResultState.SUCCESS -> {
                    binding.rltLoading.visibility = View.GONE
                    resultState.data?.let {
                        setRecyclerView(it)
                        checkPermissions2()
                    } ?: run {
                        showErrorDialog(
                            message = getString(R.string.list_is_empty),
                            buttonMessage = getString(R.string.try_again),
                            callback = { viewModel.fetchInstalledApps(false) }
                        )
                    }
                }
                is ResultState.FAIL -> {
                    binding.rltLoading.visibility = View.GONE
                    showErrorDialog(
                        message = resultState.errorMessage,
                        buttonMessage = getString(R.string.try_again),
                        callback = { viewModel.fetchInstalledApps(false) }
                    )
                }
                is ResultState.LOADING -> {
                    binding.rltLoading.visibility = View.VISIBLE
                }
            }
        }
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
        }
    }
}
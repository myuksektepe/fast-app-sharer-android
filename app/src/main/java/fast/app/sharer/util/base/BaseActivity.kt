package fast.app.sharer.util.base

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import fast.app.sharer.R


abstract class BaseActivity<T : BaseViewModel, B : ViewDataBinding> : AppCompatActivity() {

    abstract val layoutRes: Int
    abstract val viewModel: T
    abstract var viewLifecycleOwner: LifecycleOwner
    private var _binding: B? = null
    val binding get() = _binding!!

    open fun initBinding() {
        this._binding?.lifecycleOwner = this
        viewLifecycleOwner = this
    }

    abstract fun observeViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = DataBindingUtil.inflate(layoutInflater, layoutRes, null, false)
        setContentView(_binding!!.root)

        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            builder.detectFileUriExposure()
        }

        initBinding()
        observeViewModel()
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private val loadingAlertDialog by lazy {
        this.let {
            Dialog(it).apply {
                //requestWindowFeature(Window.FEATURE_NO_TITLE)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setContentView(R.layout.dialog_loading)
                setCancelable(false)
            }
        }
    }

    fun showLoading() = loadingAlertDialog.show()
    fun hideLoading() = loadingAlertDialog.hide()


    fun showErrorDialog(
        message: String?,
        buttonMessage: String?,
        callback: () -> Unit? = {}
    ) {
        this.let {
            val alertDialog = AlertDialog.Builder(this).create()
            alertDialog.apply {
                setTitle(getString(R.string.warning))
                setMessage(message)
                setButton(
                    AlertDialog.BUTTON_POSITIVE, buttonMessage
                ) { _, _ -> callback }

                show()
            }
            /*
            val btnPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val layoutParams = btnPositive.layoutParams as LinearLayout.LayoutParams
            layoutParams.weight = 10f
            btnPositive.layoutParams = layoutParams
             */
        }
    }
}
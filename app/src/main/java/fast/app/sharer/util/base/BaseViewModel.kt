package fast.app.sharer.util.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import fast.app.sharer.util.ResultState

abstract class BaseViewModel() : ViewModel() {

    val loadingErrorState = MutableLiveData<ResultState<Any?>>()

    fun handleTask(task: ResultState<Any?>, callback: () -> Unit = {}) {
        loadingErrorState.postValue(task)
        callback.invoke()
    }
}
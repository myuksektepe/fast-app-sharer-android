package fast.app.sharer.domain.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import fast.app.sharer.databinding.AppListItem2Binding
import fast.app.sharer.databinding.AppListItemBinding
import fast.app.sharer.domain.model.InstalledAppModel
import fast.app.sharer.util.Util.humanReadableByteCountBin

class AppsAdapter(
    private val model: Int
) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>(), Filterable {

    var onItemClick: ((InstalledAppModel) -> Unit)? = null
    var list: MutableList<InstalledAppModel> = mutableListOf()
    var filteredList: MutableList<InstalledAppModel> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        var binding: ViewDataBinding? = null
        if (model == 1) {
            binding = AppListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        } else if (model == 2) {
            binding = AppListItem2Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        }
        return AppViewHolder(binding!!)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) = holder.bindItems(filteredList[position])

    inner class AppViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(item: InstalledAppModel) {
            when (model) {
                1 -> {
                    binding as AppListItemBinding
                    binding.txtAppName.text = item.name
                    binding.imgAppIcon.setImageDrawable(item.iconDrawable)
                    binding.txtAppVersionName.text = "Version: ${item.versionName}"
                    binding.txtAppSize.text = "Boyut: ${humanReadableByteCountBin(item.size!!)}"
                }
                2 -> {
                    binding as AppListItem2Binding
                    binding.txtAppName.text = item.name
                    binding.imgAppIcon.setImageDrawable(item.iconDrawable)
                    binding.txtAppVersionName.text = "v${item.versionName}"
                    binding.txtAppSize.text = humanReadableByteCountBin(item.size!!)
                }
            }

            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    fun update(appList: MutableList<InstalledAppModel>) {
        list = appList
        filteredList = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = filteredList.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getFilter(): Filter = searchFilter

    private val searchFilter: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val charString = constraint?.toString() ?: ""
            if (charString.isEmpty()) {
                filteredList = list
            } else {
                val _filteredList = ArrayList<InstalledAppModel>()
                list.filter {
                    it.name!!.lowercase().trim().contains(
                        charString.lowercase().trim()
                    )
                }.forEach {
                    _filteredList.add(it)
                    //Log.i(TAG,"$charString için listelenen: ${it.name}")
                }
                filteredList = _filteredList
            }
            return FilterResults().apply { values = filteredList }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            filteredList = results!!.values as MutableList<InstalledAppModel>
            notifyDataSetChanged()
        }

    }
}
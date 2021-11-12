package fast.app.sharer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fast.app.sharer.R
import fast.app.sharer.model.InstalledAppModel
import fast.app.sharer.util.Util

class AppsAdapter(
    private val context: Context,
    var list: ArrayList<InstalledAppModel>,
    private val model: Int
) : RecyclerView.Adapter<AppsAdapter.ViewHolder>(), Filterable {

    var onItemClick: ((InstalledAppModel) -> Unit)? = null
    val listFull = ArrayList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var v: View? = null
        if (model == 1) {
            v = LayoutInflater.from(parent.context).inflate(R.layout.app_list_item, parent, false)
        } else if (model == 2) {
            v = LayoutInflater.from(parent.context).inflate(R.layout.app_list_item_2, parent, false)
        }
        return ViewHolder(v!!)
    }

    override fun getItemCount(): Int {
        return list.size
    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    override fun getFilter(): Filter {
        return searchFilter
    }

    @OptIn(ExperimentalStdlibApi::class)
    val searchFilter: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {

            var filteredList = ArrayList<InstalledAppModel>()

            if (constraint == null || constraint.length == 0) {
                filteredList.addAll(listFull)
            } else {
                val filterPattern = constraint.toString().lowercase().trim()

                for (book in listFull) {
                    if (book.name.toString().lowercase().contains(filterPattern)) {
                        filteredList.add(book)
                    }
                }
            }
            val results = FilterResults()
            results.values = filteredList
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            list.clear()
            list.addAll(results!!.values as List<InstalledAppModel>)
            notifyDataSetChanged()
        }

    }

    @ExperimentalStdlibApi
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(list.get(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @ExperimentalStdlibApi
        fun bindItems(book: InstalledAppModel) {

            // Set UI
            val txtAppName = itemView.findViewById<TextView>(R.id.txtAppName)
            val txtAppVersionName = itemView.findViewById<TextView>(R.id.txtAppVersionName)
            val txtAppSize = itemView.findViewById<TextView>(R.id.txtAppSize)
            val imgAppIcon = itemView.findViewById<ImageView>(R.id.imgAppIcon)

            txtAppName.text = book.name
            imgAppIcon.setImageDrawable(book.iconDrawable)
            if (model == 1) {
                txtAppVersionName.text = "Version: ${book.versionName}"
                txtAppSize.text = "Boyut: ${Util().humanReadableByteCountBin(book.size)}"
            } else if (model == 2) {
                txtAppVersionName.text = "Version: ${book.versionName}"
                txtAppSize.text = "${Util().humanReadableByteCountBin(book.size)}"
            }

            itemView.setOnClickListener {
                onItemClick?.invoke(book)
            }

        }
    }
}
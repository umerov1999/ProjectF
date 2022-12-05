package dev.ragnarok.fenrir.fragment.proxymanager

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero

class ProxiesAdapter(data: MutableList<ProxyConfig>, private val actionListener: ActionListener) :
    RecyclerBindableAdapter<ProxyConfig, ProxiesAdapter.Holder>(data) {
    private var active: ProxyConfig? = null
    override fun onBindItemViewHolder(viewHolder: Holder, position: Int, type: Int) {
        val config = getItem(position)
        val isActive = config == active
        viewHolder.address.text = config.getAddress()
        viewHolder.port.text = config.getPort().toString()
        viewHolder.username.text = config.getUser()
        val pass = StringBuilder()
        if (config.getPass().nonNullNoEmpty()) {
            for (i in 0 until config.getPass()?.length.orZero()) {
                pass.append("*")
            }
        }
        viewHolder.pass.text = pass.toString()
        viewHolder.setAsActive.setOnClickListener { actionListener.onSetAtiveClick(config) }
        viewHolder.delete.setOnClickListener { actionListener.onDeleteClick(config) }
        viewHolder.disable.setOnClickListener { actionListener.onDisableClick(config) }
        viewHolder.disable.visibility = if (isActive) View.VISIBLE else View.GONE
        viewHolder.setAsActive.visibility = if (isActive) View.GONE else View.VISIBLE
        viewHolder.delete.visibility = if (isActive) View.GONE else View.VISIBLE
    }

    fun setActive(active: ProxyConfig?) {
        this.active = active
        notifyDataSetChanged()
    }

    fun setData(data: MutableList<ProxyConfig>, config: ProxyConfig?) {
        setItems(data, false)
        active = config
        notifyDataSetChanged()
    }

    override fun viewHolder(view: View, type: Int): Holder {
        return Holder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_proxy
    }

    interface ActionListener {
        fun onDeleteClick(config: ProxyConfig)
        fun onSetAtiveClick(config: ProxyConfig)
        fun onDisableClick(config: ProxyConfig)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val address: TextView = itemView.findViewById(R.id.address)
        val port: TextView = itemView.findViewById(R.id.port)
        val username: TextView = itemView.findViewById(R.id.username)
        val pass: TextView = itemView.findViewById(R.id.password)
        val delete: View = itemView.findViewById(R.id.button_delete)
        val setAsActive: View = itemView.findViewById(R.id.button_set_as_active)
        val disable: View = itemView.findViewById(R.id.button_disable)
    }
}
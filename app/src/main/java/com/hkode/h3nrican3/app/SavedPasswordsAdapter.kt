package com.hkode.h3nrican3.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class SavedPasswordsAdapter(
    private val context: Context,
    private var passwords: MutableList<SavedPassword>,
    private val onSelect: (SavedPassword) -> Unit,
    private val onDelete: (SavedPassword) -> Unit
) : RecyclerView.Adapter<SavedPasswordsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(R.id.tv_password_label)
        val tvPreview: TextView = view.findViewById(R.id.tv_password_preview)
        val btnUse: MaterialButton = view.findViewById(R.id.btn_use_password)
        val btnDelete: View = view.findViewById(R.id.btn_delete_password)
        val layoutInfo: View = view.findViewById(R.id.layout_password_info)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_saved_password, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = passwords[position]
        holder.tvLabel.text = item.label
        val masked = if (item.password.length > 2) {
            "•".repeat(minOf(item.password.length, 10))
        } else {
            "••••"
        }
        holder.tvPreview.text = masked

        holder.btnUse.setOnClickListener { onSelect(item) }
        holder.layoutInfo.setOnClickListener { onSelect(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = passwords.size

    fun updateList(newList: List<SavedPassword>) {
        passwords.clear()
        passwords.addAll(newList)
        notifyDataSetChanged()
    }
}

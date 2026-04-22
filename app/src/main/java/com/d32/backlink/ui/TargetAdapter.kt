package com.d32.backlink.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.d32.backlink.databinding.ItemTargetBinding
import com.d32.backlink.model.BacklinkTarget

class TargetAdapter : ListAdapter<BacklinkTarget, TargetAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemTargetBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: BacklinkTarget) {
            b.tvTitle.text  = item.title
            b.tvUrl.text    = item.url
            b.tvReferer.text = "referer: ${item.referer.removePrefix("https://")}"
            val (color, label) = when (item.status) {
                BacklinkTarget.Status.PENDING -> 0xFF888888.toInt() to "대기"
                BacklinkTarget.Status.RUNNING -> 0xFFFF9900.toInt() to "실행 중"
                BacklinkTarget.Status.SUCCESS -> 0xFF22AA44.toInt() to "성공"
                BacklinkTarget.Status.FAILED  -> 0xFFCC3333.toInt() to "실패"
            }
            b.tvStatus.setTextColor(color)
            b.tvStatus.text = label
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTargetBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<BacklinkTarget>() {
            override fun areItemsTheSame(a: BacklinkTarget, b: BacklinkTarget) = a.url == b.url
            override fun areContentsTheSame(a: BacklinkTarget, b: BacklinkTarget) = a == b
        }
    }
}

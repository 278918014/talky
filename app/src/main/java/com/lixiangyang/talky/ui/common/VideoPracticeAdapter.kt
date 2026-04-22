package com.lixiangyang.talky.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lixiangyang.talky.databinding.ItemVideoPracticeBinding
import com.lixiangyang.talky.domain.model.VideoPractice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoPracticeAdapter(
    private val onClick: (VideoPractice) -> Unit
) : RecyclerView.Adapter<VideoPracticeAdapter.VideoViewHolder>() {

    private var items: List<VideoPractice> = emptyList()

    fun submitList(newItems: List<VideoPractice>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding =
            ItemVideoPracticeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class VideoViewHolder(
        private val binding: ItemVideoPracticeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VideoPractice) {
            val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            binding.title.text = item.title
            binding.meta.text =
                "${formatter.format(Date(item.recordedAt))} · ${item.durationSeconds}s · ${item.resolution}"
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

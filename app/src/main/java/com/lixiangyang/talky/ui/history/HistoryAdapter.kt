package com.lixiangyang.talky.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lixiangyang.talky.databinding.ItemDateHeaderBinding
import com.lixiangyang.talky.databinding.ItemVideoPracticeBinding
import com.lixiangyang.talky.domain.model.VideoPractice
import com.lixiangyang.talky.ui.common.VideoThumbnailLoader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 支持日期分组的历史练习列表适配器
 * 包含日期头和视频卡片两种 item 类型
 */
class HistoryAdapter(
    private val onVideoClick: (VideoPractice) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 扁平化的列表，包含日期头和视频项
    private var items: List<ListItem> = emptyList()

    sealed class ListItem {
        data class DateHeader(val dateLabel: String) : ListItem()
        data class VideoItem(val practice: VideoPractice) : ListItem()
    }

    fun submitGrouped(grouped: List<Pair<String, List<VideoPractice>>>) {
        val newItems = mutableListOf<ListItem>()
        for ((dateLabel, videos) in grouped) {
            newItems.add(ListItem.DateHeader(dateLabel))
            videos.forEach { newItems.add(ListItem.VideoItem(it)) }
        }
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.DateHeader -> VIEW_TYPE_HEADER
            is ListItem.VideoItem -> VIEW_TYPE_VIDEO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemDateHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                DateHeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemVideoPracticeBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                VideoViewHolder(binding)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item.dateLabel)
            is ListItem.VideoItem -> (holder as VideoViewHolder).bind(item.practice)
        }
    }

    inner class DateHeaderViewHolder(
        private val binding: ItemDateHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dateLabel: String) {
            binding.dateLabel.text = dateLabel
        }
    }

    inner class VideoViewHolder(
        private val binding: ItemVideoPracticeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(practice: VideoPractice) {
            val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.title.text = "${timeFormatter.format(Date(practice.recordedAt))} 录制"
            binding.meta.text = "${practice.durationSeconds}秒 · ${practice.resolution}"
            VideoThumbnailLoader.loadInto(
                imageView = binding.thumbnail,
                videoPath = practice.filePath,
                thumbnailPath = practice.thumbnailPath
            )
            binding.root.setOnClickListener { onVideoClick(practice) }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_VIDEO = 1
    }
}

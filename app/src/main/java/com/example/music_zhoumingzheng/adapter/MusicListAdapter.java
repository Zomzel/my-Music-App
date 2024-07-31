package com.example.music_zhoumingzheng.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music_zhoumingzheng.R;
import com.example.music_zhoumingzheng.module.DataBean.Data.Record.MusicInfo;
import com.example.music_zhoumingzheng.utils.PlaylistManager;

import java.util.List;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.MusicViewHolder> {
    private List<MusicInfo> musicList;
    private OnItemClickListener listener;
    private PlaylistManager playlistManager;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public MusicListAdapter(List<MusicInfo> musicList) {
        this.musicList = musicList;
        playlistManager = PlaylistManager.getInstance();
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_item, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        MusicInfo music = musicList.get(position);
        holder.musicNameTextView.setText(music.musicName);
        holder.authorTextView.setText(music.author);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
        // 设置删除按钮的点击事件
        holder.deleteBtn.setOnClickListener(v -> {
            int currentIndex = playlistManager.getCurrentIndex();

            // 从播放列表和适配器的数据中删除对应的歌曲
            // 检查删除的是否是当前播放的歌曲
//            if (position == currentIndex) {
//                // 如果是当前播放的歌曲，则播放下一首
//                playlistManager.next();
//                int newCurrentIndex = playlistManager.getCurrentIndex();
//                notifyItemChanged(newCurrentIndex); // 更新新的当前播放的歌曲
//            } else if (position < currentIndex) {
//                // 如果删除的是当前播放歌曲之前的歌曲，则需要更新currentIndex
//                playlistManager.setCurrentIndex(currentIndex - 1);
//            }

            //删除后怎么让歌单更新数量

            playlistManager.removeMusic(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, musicList.size());
        });

    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView musicNameTextView;
        TextView authorTextView;
        ImageView deleteBtn;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            musicNameTextView = itemView.findViewById(R.id.musicName);
            authorTextView = itemView.findViewById(R.id.authorName);
            deleteBtn = itemView.findViewById(R.id.delete);
        }
        //给item设置点击事件，点击item，播放当前点击的音乐
    }
}

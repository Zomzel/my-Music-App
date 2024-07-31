package com.example.music_zhoumingzheng.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.music_zhoumingzheng.MusicDetailActivity;
import com.example.music_zhoumingzheng.module.DataBean.Data.Record.MusicInfo;
import com.example.music_zhoumingzheng.R;
import com.example.music_zhoumingzheng.utils.PlaylistManager;

import java.util.List;

public class RecommendAdapter extends RecyclerView.Adapter<RecommendAdapter.RecommendViewHolder> {

    private Context context;
    private List<MusicInfo> musicInfos;

    //2.准备接口对象
    private OnItemClickListener mOnItemClickListener;
    //1.定义回调接口
    public interface OnItemClickListener{
        void onItemClick(View view,int position);
    }

    // 3. 定义回调方法
    public void setOnItemClickListener(OnItemClickListener listener){
        this.mOnItemClickListener = listener;
    }


    public RecommendAdapter(Context context, List<MusicInfo> musicInfos) {
        this.context = context;
        this.musicInfos = musicInfos;
    }

    @NonNull
    @Override
    public RecommendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recommend_music, parent, false);
        return new RecommendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendViewHolder holder, @SuppressLint("RecyclerView")int position) {
        MusicInfo musicInfo = musicInfos.get(position);
        holder.textMusicName.setText(musicInfo.musicName);
        holder.textAuthor.setText(musicInfo.author);
        Glide.with(context)
                .load(musicInfo.coverUrl)
                .transform(new RoundedCorners(16))
                .into(holder.imageCover);

        // 添加到播放列表按钮的点击事件
        holder.buttonAddToPlaylist.setOnClickListener(v -> {
            // 处理添加到播放列表的逻辑
            Toast.makeText(v.getContext(),"已添加到播放列表",Toast.LENGTH_SHORT).show();
            PlaylistManager.getInstance().addMusic(musicInfo);
        });

        //4. 调用接口方法
        if(mOnItemClickListener != null){
            holder.buttonAddToPlaylist.setOnClickListener(v->mOnItemClickListener.onItemClick(v,position));

            holder.imageCover.setOnClickListener(v->mOnItemClickListener.onItemClick(v,position));

            holder.textAuthor.setOnClickListener(v->mOnItemClickListener.onItemClick(v,position));

            holder.textMusicName.setOnClickListener(v->mOnItemClickListener.onItemClick(v,position));
        }

        //为整个itemView设置点击事件
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(),"点击了"+musicInfo.musicName,Toast.LENGTH_SHORT).show();
                // 跳转到音乐详情页面
                Intent intent = new Intent(v.getContext(), MusicDetailActivity.class);
                //将当前模块的所有歌曲传递给音乐列表
                PlaylistManager.getInstance().addAll(musicInfos,position);
                intent.putExtra("NewPosition",true);
//                Intent serviceIntent = new Intent(context, MusicService.class);
//                context.startService(serviceIntent);
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return musicInfos.size();
    }

    public static class RecommendViewHolder extends RecyclerView.ViewHolder {

        ImageView imageCover;
        TextView textMusicName;
        TextView textAuthor;
        ImageView buttonAddToPlaylist;

        public RecommendViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCover = itemView.findViewById(R.id.coverImage);
            textMusicName = itemView.findViewById(R.id.musicName);
            textAuthor = itemView.findViewById(R.id.authorName);
            buttonAddToPlaylist = itemView.findViewById(R.id.addButton);
        }
    }
}


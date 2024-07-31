package com.example.music_zhoumingzheng.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.music_zhoumingzheng.MusicDetailActivity;
import com.example.music_zhoumingzheng.module.DataBean.Data.Record.MusicInfo;

import java.util.List;

import android.widget.Toast;

import com.example.music_zhoumingzheng.R;
import com.example.music_zhoumingzheng.service.MusicService;
import com.example.music_zhoumingzheng.utils.PlaylistManager;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private static Context context;
    private static List<MusicInfo> musicInfos;

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


    public BannerAdapter(Context context, List<MusicInfo> musicInfos) {
        this.context = context;
        this.musicInfos = musicInfos;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner_image, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, @SuppressLint("RecyclerView") int position) {
        MusicInfo musicInfo = musicInfos.get(position);
        holder.musicName.setText(musicInfo.musicName);
        holder.authorName.setText(musicInfo.author);
        Glide.with(context)
                .load(musicInfo.coverUrl)
                .transform(new RoundedCorners(30)) // 设置圆角
                .into(holder.coverImage);

        // 添加到播放列表按钮的点击事件
        holder.addButton.setOnClickListener(v -> {
            // 处理添加到播放列表的逻辑
            Toast.makeText(v.getContext(),"已添加到播放列表",Toast.LENGTH_SHORT).show();
            PlaylistManager.getInstance().addMusic(musicInfo);
        });

        //4. 调用接口方法
        if(mOnItemClickListener != null){
            holder.addButton.setOnClickListener(v->mOnItemClickListener.onItemClick(v,position));

            holder.coverImage.setOnClickListener(v->mOnItemClickListener.onItemClick(v,position));

            holder.authorName.setOnClickListener(v->mOnItemClickListener.onItemClick(v,position));

            holder.musicName.setOnClickListener(v->mOnItemClickListener.onItemClick(v,position));
        }

        //为整个itemView设置点击事件
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(),"点击了"+musicInfo.musicName,Toast.LENGTH_SHORT).show();
                // 跳转到音乐详情页面
                Intent intent = new Intent(v.getContext(), MusicDetailActivity.class);
                //将当前模块的所有歌曲传递给音乐列表
                intent.putExtra("NewPosition",true);
                PlaylistManager.getInstance().addAll(musicInfos,position);
                v.getContext().startActivity(intent);
            }
        });


    }

    @Override
    public int getItemCount() {
        return musicInfos.size();
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImage;
        TextView musicName;
        TextView authorName;
        ImageView addButton;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.coverImage);
            musicName = itemView.findViewById(R.id.musicName);
            authorName = itemView.findViewById(R.id.authorName);
            addButton = itemView.findViewById(R.id.addButton);
        }

    }

    public void setMusicInfos(List<MusicInfo> musicInfos){
        this.musicInfos = musicInfos;
    }


}


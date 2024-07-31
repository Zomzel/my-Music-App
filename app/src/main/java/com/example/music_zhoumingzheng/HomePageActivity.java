package com.example.music_zhoumingzheng;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.music_zhoumingzheng.adapter.MusicListAdapter;
import com.example.music_zhoumingzheng.module.BannerModule;
import com.example.music_zhoumingzheng.module.BaseModule;
import com.example.music_zhoumingzheng.module.CardModule;
import com.example.music_zhoumingzheng.module.DataBean;
import com.example.music_zhoumingzheng.module.PopularModule;
import com.example.music_zhoumingzheng.module.RecommendModule;
import com.example.music_zhoumingzheng.module.TitleModule;
import com.example.music_zhoumingzheng.service.MusicService;
import com.example.music_zhoumingzheng.utils.PlaylistManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomePageActivity extends AppCompatActivity {

    private static final String TAG = "HomePageActivity";

    //网络请求/解析数据/handler处理和管理消息队列
    private OkHttpClient client; //网络请求客户端
    private Handler handler = new Handler();
    private Gson gson = new Gson();


    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private Random random = new Random();

    //所有模块的数据
    private List<BaseModule> moduleList;
    private MultiModuleAdapter multiModuleAdapter;
    private boolean isLoaded = false;
    private int totalItem = 0;

    private List<DataBean> data = new ArrayList<>(); //页面所有数据的json文件字符串
    private List<DataBean.Data.Record> records = new ArrayList<>(); //只有里面的record才是我需要的数据

    //悬浮view控件
    private ImageView floatingCoverImageView;
    private TextView floatingMusicNameTextView;
    private TextView floatingAuthorTextView;
    private ImageView floatingPlayPauseButton;

    //播放服务
    private PlaylistManager playlistManager;
    private DataBean.Data.Record.MusicInfo currentMusic;
    private boolean isBound = false;
    private MusicService musicService;


    //动画
    private ObjectAnimator rotateAnimator;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            if(musicService.isPlaying()){
                //恢复播放
                musicService.resumeMusic();
            }
            isBound = true;
            //更新悬浮view状态
            updateFloatingView();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        client = new OkHttpClient();
        //发起网络请求
        makeNetworkRequest(true);

        // 获取音乐列表管理
        playlistManager = PlaylistManager.getInstance();

        Intent serviceIntent = new Intent(this, MusicService.class);
        startService(serviceIntent);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        recyclerView = findViewById(R.id.recyclerView);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Simulate network loading
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Clear old data and load new data
                        moduleList.clear();
                        makeNetworkRequest(true);
                        multiModuleAdapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 2000);
            }
        });

        // Set up scroll listener for RecyclerView
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1)) {
                    // Reached the bottom
                    loadMoreData();
                }
            }
        });

        //添加点击事件
        initFloatView();
        // 在Service中更新悬浮View
        //注册广播
        // 注册广播接收器
        LocalBroadcastManager.getInstance(this).registerReceiver(musicInfoReceiver,
                new IntentFilter("com.example.music_zhoumingzheng.UPDATE_FLOATING_VIEW"));
    }



    //首先要发起网络请求获取数据
    private void makeNetworkRequest(boolean isRefresh) {

        //1.创建url,每次请求的current、size参数不同
        //下拉刷新、列表布局和适配器，count记录data的数据数量和网络请求应该使用的参数
        int count = 1;
        if(!isRefresh){
            count = random.nextInt(2) + 2;
        }

        String url = "https://hotfix-service-prod.g.mi.com/music/homePage?current=" + count + "&size=4";

        //OkHttp进行get异步请求
        //2.2创建request对象
        Request request = new Request.Builder().get().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //网络请求无响应
                showToast("Network request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //网络响应成功
                if (response.isSuccessful()) {
                    //获取响应内容
                    assert response.body() != null;
                    String responseData = response.body().string();
                    //gson解析数据内容
                    DataBean mData = gson.fromJson(responseData, DataBean.class);
                    if( mData!=null) {
                        handler.post(new Runnable() {
                            //post Message更新UI
                            @Override
                            public void run() {
                                if(isRefresh) {
                                    data.clear();
                                    records.clear();
                                    isLoaded = true;
                                }
                                totalItem = records.size();
                                //添加数据到列表中
                                data.add(mData);
                                records.addAll(mData.data.records);
                                //渲染界面
                                updateUI();
                            }
                        });
                    }else{
                        //响应内容为空
                        showToast("Response data is empty");
                    }
                } else {
                    //响应失败
                    showToast("Request failed: " + response.message());
                }
            }
        });
    }

    private void updateUI() {
        // 其他模块的数据初始化同理
        LoadData();
    }

    private void LoadData(){
        //装填数据
        if(moduleList==null) moduleList = new ArrayList<>();
        if(records!=null){
            for (int i = totalItem; i < records.size(); i++) {
                switch (records.get(i).style){
                    case 1:
                        moduleList.add(new BannerModule(records.get(i).musicInfoList));
                        break;
                    case 2:
                        moduleList.add(new TitleModule("专属好歌"));
                        moduleList.add(new CardModule(records.get(i).musicInfoList));
                        break;
                    case 3:
                        moduleList.add(new TitleModule("每日推荐"));
                        moduleList.add(new RecommendModule(records.get(i).musicInfoList));
                        break;
                    case 4:
                        moduleList.add(new TitleModule("热门金曲"));
                        moduleList.add(new PopularModule(records.get(i).musicInfoList));
                        break;
                }
            }
            if(multiModuleAdapter==null){
                //更新UI
                recyclerView = findViewById(R.id.recyclerView);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                multiModuleAdapter = new MultiModuleAdapter(this, moduleList);
                multiModuleAdapter.setOnItemClickListener(new MultiModuleAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        //不管view是什么类型，带着自己的数据启动音乐播放详情页
                    }
                });
                recyclerView.setAdapter(multiModuleAdapter);
            }
            multiModuleAdapter.notifyDataSetChanged();
        }
        totalItem = records.size();

    }

    private void loadMoreData() {
        // Simulate network loading
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                makeNetworkRequest(false);
            }
        }, 2000);
    }


    private void showToast(String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(HomePageActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public final class Message implements Parcelable {
        /**
         * 消息类Message
         * 包含消息描述和数据*/
        public int what;
        public Object obj;
        public long when;
        Handler target;
        Runnable callback;
        Message next;

        protected Message(Parcel in) {
            what = in.readInt();
            when = in.readLong();
            next = in.readParcelable(Message.class.getClassLoader());
        }

        public final Creator<Message> CREATOR = new Creator<Message>() {
            @Override
            public Message createFromParcel(Parcel in) {
                return new Message(in);
            }

            @Override
            public Message[] newArray(int size) {
                return new Message[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(what);
            dest.writeLong(when);
            dest.writeParcelable(next, flags);
        }
    }

    private void initFloatView(){
        // 初始化悬浮View
        RelativeLayout floatingMusicView = findViewById(R.id.floatingView);
        floatingCoverImageView = findViewById(R.id.floatingCoverImageView);
        floatingMusicNameTextView = findViewById(R.id.floatingMusicNameTextView);
        floatingAuthorTextView = findViewById(R.id.floatingAuthorTextView);
        floatingPlayPauseButton = findViewById(R.id.floatingPlayPauseButton);
        ImageView floatingPlaylistButton = findViewById(R.id.floatingPlaylistButton);

        // 创建旋转动画
        // 创建旋转动画
        rotateAnimator = ObjectAnimator.ofFloat(floatingCoverImageView, "rotation", 0f, 360f);
        rotateAnimator.setDuration(10000); // 动画持续时间10秒
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE); // 无限循环
        rotateAnimator.setInterpolator(new LinearInterpolator());


        floatingMusicView.setOnClickListener(v -> {
            // 跳转到播放详情页面
            //如果播放列表为空，则不可点击
            if(!playlistManager.getPlayList().isEmpty()){
                Intent intent = new Intent(HomePageActivity.this, MusicDetailActivity.class);
                intent.putExtra("NewPosition",false);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out);
            }else{
                showToast("请先添加歌曲到播放列表");
            }
        });

        // 设置点击事件
        floatingPlayPauseButton.setOnClickListener(v -> {
            if(!playlistManager.getPlayList().isEmpty()) {
                if (musicService.isPlaying()) {
                    musicService.pause();
                    floatingPlayPauseButton.setImageResource(R.drawable.play_light);
                    rotateAnimator.pause();
                } else {
                    musicService.resumeMusic();
                    floatingPlayPauseButton.setImageResource(R.drawable.pause_light);
                    rotateAnimator.start();
                }
            }else{
                showToast("请先添加歌曲到播放列表");
            }
        });

        floatingPlaylistButton.setOnClickListener(v -> {
            // 打开播放列表
            if(!playlistManager.getPlayList().isEmpty()) {
                showMusicListDialog();
            }else{
                showToast("请先添加歌曲到播放列表");
            }
        });


    }

    private void updateFloatingView(){
        if(playlistManager.getPlayList().isEmpty())
            return; //为空则不做更新ui
        currentMusic = playlistManager.getCurrentMusic();
        floatingMusicNameTextView.setText(currentMusic.musicName);
        floatingAuthorTextView.setText(currentMusic.author);
        Glide.with(HomePageActivity.this).load(currentMusic.coverUrl).circleCrop().into(floatingCoverImageView);
        floatingPlayPauseButton.setImageResource(musicService.isPlaying()?R.drawable.pause_light:R.drawable.play_light);
        if(musicService.isPlaying())
            rotateAnimator.start();
        else
            rotateAnimator.pause();
    }

    private BroadcastReceiver musicInfoReceiver = new BroadcastReceiver() {
        //广播，根据收到的广播内容更新悬浮view状态
        @Override
        public void onReceive(Context context, Intent intent) {
            //收到广播，更新悬浮View状态
            Log.d(TAG, "onReceive: 悬浮view收到广播");
            updateFloatingView();
//            floatingMusicView.setVisibility(View.VISIBLE);
        }
    };

    private void showMusicListDialog() {
        //展示歌单
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_music_list, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        RecyclerView recyclerView = view.findViewById(R.id.musicListRecyclerView);
        TextView musicCount = view.findViewById(R.id.music_count);
        TextView modeText = view.findViewById(R.id.tv_mode);
        ImageView modeImage=  view.findViewById(R.id.iv_mode);
        LinearLayout ll_mode = view.findViewById(R.id.ll_mode);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        MusicListAdapter adapter = new MusicListAdapter(playlistManager.getPlayList());
        recyclerView.setAdapter(adapter);

        musicCount.setText(String.valueOf(playlistManager.getPlayList().size()));

        ll_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistManager.switchMode();
                //根据当前的mode修改文字类型
                switch(playlistManager.getPlayMode()){
                    case PlaylistManager.PLAY_MODE_SEQUENCE:
                        modeImage.setImageResource(R.drawable.sequential_light);
                        modeText.setText("顺序模式");
                        break;
                    case PlaylistManager.PLAY_MODE_SHUFFLE:
                        modeImage.setImageResource(R.drawable.random_light);
                        modeText.setText("随机模式");
                        break;
                    case PlaylistManager.PLAY_MODE_REPEAT_ONE:
                        modeImage.setImageResource(R.drawable.loop_light);
                        modeText.setText("循环模式");
                        break;
                }
                //可是修改了模式，对应的UI也要改
            }
        });

        adapter.setOnItemClickListener(position -> {
            playlistManager.setCurrentIndex(position);
            currentMusic = playlistManager.getCurrentMusic();
            // 更新 UI 和播放音乐
            musicService.playMusic();
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 启动音乐服务
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        //释放资源
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放资源
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        handler.removeCallbacksAndMessages(null);
        //取消广播注册
        LocalBroadcastManager.getInstance(this).unregisterReceiver(musicInfoReceiver);
    }


}
package com.example.music_zhoumingzheng;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.music_zhoumingzheng.adapter.MusicListAdapter;
import com.example.music_zhoumingzheng.module.DataBean;
import com.example.music_zhoumingzheng.service.MusicService;
import com.example.music_zhoumingzheng.utils.PlaylistManager;
import com.example.music_zhoumingzheng.module.DataBean.Data.Record.MusicInfo;
import com.google.android.material.bottomsheet.BottomSheetDialog;


import java.util.HashSet;
import java.util.Set;

import me.wcy.lrcview.LrcView;

public class MusicDetailActivity extends AppCompatActivity {

    private static final String TAG = "MusicDetailActivity";


    //音乐信息
    private ImageView coverImageView;
    private TextView musicNameTextView;
    private TextView authorTextView;
    private LrcView lrcView;    //歌词
    private boolean isLyricsVisible = false; //歌词是否显示

    //播放控制
    private SeekBar seekBar;
    private ImageView playPauseButton;
    private ImageView previousButton;
    private ImageView nextButton;
    private ImageView closeButton;
    private ImageView modeButton;
    private ImageView favoriteButton;
    private ImageView musicList;
    private boolean isUserSeeking = false;
    private Runnable seekBarRunnable;
    private boolean newPosition = false;

    //播放服务
    private PlaylistManager playlistManager;
    private MusicInfo currentMusic;
    private boolean isBound = false;
    private MusicService musicService;
    private TextView currentTime;
    private TextView totalTime;

    //处理信息
    private Handler handler = new Handler();

    //动画
    private ObjectAnimator rotateAnimator;

    //通过sharedPreference保存歌曲的收藏状态
    private static final String PREFS_NAME = "favorite_songs";
    private static final String FAVORITES_KEY = "favorite_song_ids";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_detail);

        Intent intent = getIntent();
        if(intent.hasExtra("NewPosition"))
            newPosition = intent.getBooleanExtra("NewPosition", false);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        //音乐信息
        coverImageView = findViewById(R.id.coverImageView);
        musicNameTextView = findViewById(R.id.musicNameTextView);
        authorTextView = findViewById(R.id.authorTextView);

        //音乐控制
        seekBar = findViewById(R.id.seekBar);
        playPauseButton = findViewById(R.id.playPauseButton);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        closeButton = findViewById(R.id.btn_close);
        modeButton = findViewById(R.id.btn_mode);
        favoriteButton = findViewById(R.id.btn_like);
        musicList = findViewById(R.id.listButton);

        playlistManager = PlaylistManager.getInstance();


        //歌词
        lrcView = findViewById(R.id.lrc_view);

        currentMusic = playlistManager.getCurrentMusic();

        if (currentMusic != null){
            // 使用 Glide 加载图片
            Glide.with(this)
                    .asBitmap()
                    .load(currentMusic.coverUrl)
                    .circleCrop()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            coverImageView.setImageBitmap(resource);
                            extractColorFromBitmap(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
            musicNameTextView.setText(currentMusic.musicName);
            authorTextView.setText(currentMusic.author);
            favoriteButton.setImageResource(isFavorite(String.valueOf(currentMusic.id))?R.drawable.like:R.drawable.unlike);
        }


        // 创建旋转动画
        // 创建旋转动画
        rotateAnimator = ObjectAnimator.ofFloat(coverImageView, "rotation", 0f, 360f);
        rotateAnimator.setDuration(10000); // 动画持续时间10秒
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE); // 无限循环
        rotateAnimator.setInterpolator(new LinearInterpolator());
        rotateAnimator.start();

        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // 初始化播放按钮和进度条等控件
        initControls();

        // 注册广播接收器
        LocalBroadcastManager.getInstance(this).registerReceiver(updateUIReceiver,
                        new IntentFilter("com.example.music_zhoumingzheng.UPDATE_UI"));
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver,
                new IntentFilter("com.example.music_zhoumingzheng.UPDATE_PROGRESS"));


    }

    private void initControls() {

        //播放和暂停按钮
        playPauseButton.setOnClickListener(v -> {
            if (musicService.isPlaying()) {
                musicService.pause();
                Toast.makeText(this,"暂停",Toast.LENGTH_SHORT).show();
                playPauseButton.setImageResource(R.drawable.play);
                handler.post(runnable);
                rotateAnimator.pause();
            } else {
                Toast.makeText(this,"播放",Toast.LENGTH_SHORT).show();
                musicService.resumeMusic();
                handler.removeCallbacks(runnable);
                playPauseButton.setImageResource(R.drawable.pause);
                rotateAnimator.start();
            }
        });

        //进度条
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound && musicService != null) {
                    musicService.seekTo(progress);
                    lrcView.updateTime(seekBar.getProgress());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
            }
        });


        modeButton.setOnClickListener(v -> {
            PlaylistManager playlistManager = PlaylistManager.getInstance();
            playlistManager.switchMode();
            int currentMode = playlistManager.getPlayMode();
            switch (currentMode) {
                case PlaylistManager.PLAY_MODE_SEQUENCE:
                    modeButton.setImageResource(R.drawable.sequential_dark);
                    break;
                case PlaylistManager.PLAY_MODE_SHUFFLE:
                    modeButton.setImageResource(R.drawable.random_dark);
                    break;
                case PlaylistManager.PLAY_MODE_REPEAT_ONE:
                    modeButton.setImageResource(R.drawable.loop_dark);
                    break;
            }
        });

        // 点击关闭按钮退出当前页面
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //关闭前保存当前播放的进度
                musicService.setProgress(musicService.getCurrentPosition());
                finish();
                overridePendingTransition(0, R.anim.slide_out_bottom);
            }
        });


        // 初始化上一首按钮
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistManager.previous();
                currentMusic = playlistManager.getCurrentMusic();
                musicService.play(playlistManager.getCurrentIndex());
                rotateAnimator.start();
                updateUI();
            }
        });

        // 初始化下一首按钮
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistManager.next();
                currentMusic = playlistManager.getCurrentMusic();
                musicService.play(playlistManager.getCurrentIndex());
                rotateAnimator.start();
                updateUI();
            }
        });

        //加载歌词
        // 加载在线歌词
        lrcView.loadLrcByUrl(currentMusic.lyricUrl, "utf-8");

        lrcView.setDraggable(true, (view, time) -> {
            musicService.seekTo((int) time);
            if (!musicService.isPlaying()) {
                musicService.playMusic();
                handler.post(runnable);
            }
            return true;
        });

        lrcView.setOnTapListener((view, x, y) -> {
            //将歌词隐藏，显示封面
            lrcView.setVisibility(View.INVISIBLE);
            coverImageView.setVisibility(View.VISIBLE);
            coverImageView.setClickable(true);
            lrcView.setClickable(false);
            isLyricsVisible = !isLyricsVisible;
        });

        // 设置点击封面切换歌词视图
        coverImageView.setOnClickListener(v -> {
            lrcView.setVisibility(View.VISIBLE);
            coverImageView.setVisibility(View.INVISIBLE);
            coverImageView.setClickable(false);
            lrcView.setClickable(true);
            isLyricsVisible = !isLyricsVisible;
        });

        //收藏按钮
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                toggleFavorite(String.valueOf(playlistManager.getCurrentMusic().id));
                boolean isFavorite = isFavorite(String.valueOf(playlistManager.getCurrentMusic().id));
                animateFavoriteButton(favoriteButton, isFavorite);
                updateFavoriteButton(favoriteButton, playlistManager.getCurrentMusic());
            }
        });

        //歌单按钮
        musicList.setOnClickListener(v -> showMusicListDialog());

    }

    private void updateUI() {
        currentMusic = playlistManager.getCurrentMusic();
        // 更新UI
        Glide.with(this)
                .asBitmap()
                .load(currentMusic.coverUrl)
                .circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        coverImageView.setImageBitmap(resource);
                        extractColorFromBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
        musicNameTextView.setText(currentMusic.musicName);
        authorTextView.setText(currentMusic.author);
        playPauseButton.setImageResource(musicService.isPlaying() ? R.drawable.pause : R.drawable.play);
        //更新歌词内容
        lrcView.loadLrcByUrl(currentMusic.lyricUrl, "utf-8");
        //设置点赞状态的按钮
        favoriteButton.setImageResource(isFavorite(String.valueOf(currentMusic.id))?R.drawable.like:R.drawable.unlike);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            if(newPosition)
                musicService.playMusic();
            initializeSeekBar();
            playPauseButton.setImageResource(musicService.isPlaying() ? R.drawable.pause : R.drawable.play);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private void initializeSeekBar() {
        seekBar = findViewById(R.id.seekBar);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        lrcView = findViewById(R.id.lrc_view);
        if (isBound && musicService != null) {
            seekBar.setMax(musicService.getDuration());

            Runnable updateSeekBar = new Runnable() {
                @Override
                public void run() {
                    if (isBound && musicService != null) {
                        int currentPosition = musicService.getCurrentPosition();
                        seekBar.setProgress(currentPosition);
                        currentTime.setText(formatTime(currentPosition));
                        totalTime.setText(formatTime(musicService.getDuration()));
                        lrcView.updateTime(currentPosition);
                        handler.postDelayed(this, 1000);
                    }
                }
            };
            handler.post(updateSeekBar);
        }
    }

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }


    private void extractColorFromBitmap(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null) {
                int defaultColor = Color.BLACK;
                int color = palette.getDominantColor(defaultColor);
                // 设置背景颜色
                findViewById(R.id.music_detail_layout).setBackgroundColor(color);
            }
        });
    }


    private BroadcastReceiver updateUIReceiver = new BroadcastReceiver() {
        //广播，用于更新界面
        @Override
        public void onReceive(Context context, Intent intent) {
            int currentIndex = intent.getIntExtra("currentIndex", -1);
            if (currentIndex != -1) {
                updateUI();
                lrcView.updateTime(0);
                seekBar.setProgress(0);
            }
            Log.d(TAG, "onReceive: 广播已收到 currentIndex:"+currentIndex);
        }
    };

    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //广播，用于更新进度条状态
            int currentPosition = intent.getIntExtra("currentPosition", 0);
            int duration = intent.getIntExtra("duration",0);
            seekBar.setMax(duration);
            seekBar.setProgress(currentPosition);
            lrcView.updateTime(currentPosition);
            currentTime.setText(formatTime(currentPosition));

        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //随着歌曲进度更新歌词位置和进度条的位置
            if (musicService.isPlaying()) {
                long time = musicService.getCurrentPosition();
                lrcView.updateTime(time);
                seekBar.setProgress((int) time);
            }

            handler.postDelayed(this, 300);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放资源
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        handler.removeCallbacksAndMessages(null);   //清除消息队列

        //取消广播注册
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateUIReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
    }

    // 获取收藏的歌曲ID集合
    private Set<String> getFavoriteSongs() {
        return sharedPreferences.getStringSet(FAVORITES_KEY, new HashSet<>());
    }

    // 检查歌曲是否被收藏
    public boolean isFavorite(String songId) {
        return getFavoriteSongs().contains(songId);
    }

    // 切换收藏状态
    public void toggleFavorite(String songId) {
        Set<String> favorites = new HashSet<>(getFavoriteSongs());
        if (favorites.contains(songId)) {
            favorites.remove(songId);
        } else {
            favorites.add(songId);
        }
        sharedPreferences.edit().putStringSet(FAVORITES_KEY, favorites).apply();
    }

    private void updateFavoriteButton(ImageView favoriteButton, MusicInfo musicInfo) {
        boolean isFavorite = isFavorite(String.valueOf(musicInfo.id));
        favoriteButton.setImageResource(isFavorite ? R.drawable.like : R.drawable.unlike);
    }

    private void animateFavoriteButton(ImageView favoriteButton, boolean isFavorite) {
        if (isFavorite) {
            // 收藏动画
            PropertyValuesHolder scaleXUp = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.2f);
            PropertyValuesHolder scaleYUp = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.2f);
            PropertyValuesHolder rotation = PropertyValuesHolder.ofFloat(View.ROTATION_Y, 0f, 360f);

            ObjectAnimator scaleUp = ObjectAnimator.ofPropertyValuesHolder(favoriteButton, scaleXUp, scaleYUp);
            scaleUp.setDuration(500);  // 前半部分动画时间

            PropertyValuesHolder scaleXDown = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f, 1.0f);
            PropertyValuesHolder scaleYDown = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f, 1.0f);

            ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(favoriteButton, scaleXDown, scaleYDown);
            scaleDown.setDuration(500);  // 后半部分动画时间

            ObjectAnimator rotate = ObjectAnimator.ofPropertyValuesHolder(favoriteButton, rotation);
            rotate.setDuration(1000);  // 整个动画时间

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleUp, rotate);
            animatorSet.playSequentially(scaleUp, scaleDown);
            animatorSet.start();
        } else {
            // 取消收藏动画
            PropertyValuesHolder scaleXDown = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 0.8f);
            PropertyValuesHolder scaleYDown = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 0.8f);

            ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(favoriteButton, scaleXDown, scaleYDown);
            scaleDown.setDuration(500);  // 前半部分动画时间

            PropertyValuesHolder scaleXUp = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1.0f);
            PropertyValuesHolder scaleYUp = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1.0f);

            ObjectAnimator scaleUp = ObjectAnimator.ofPropertyValuesHolder(favoriteButton, scaleXUp, scaleYUp);
            scaleUp.setDuration(500);  // 后半部分动画时间

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(scaleDown, scaleUp);
            animatorSet.start();
        }
    }

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
                        modeButton.setImageResource(R.drawable.sequential_dark); //播放界面的icon
                        modeText.setText("顺序模式");
                        break;
                    case PlaylistManager.PLAY_MODE_SHUFFLE:
                        modeImage.setImageResource(R.drawable.random_light);
                        modeButton.setImageResource(R.drawable.random_dark);
                        modeText.setText("随机模式");
                        break;
                    case PlaylistManager.PLAY_MODE_REPEAT_ONE:
                        modeImage.setImageResource(R.drawable.loop_light);
                        modeButton.setImageResource(R.drawable.loop_dark);
                        modeText.setText("循环模式");
                        break;
                }
                //可是修改了模式，对应的UI也要改
            }
        });

        adapter.setOnItemClickListener(position -> {
            playlistManager.setCurrentIndex(position);
            currentMusic = playlistManager.getCurrentMusic();
            musicService.playMusic();
            // 更新 UI 和播放音乐
            dialog.dismiss();
        });

        dialog.show();
    }

}

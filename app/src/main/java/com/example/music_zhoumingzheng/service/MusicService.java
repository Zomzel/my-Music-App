package com.example.music_zhoumingzheng.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.music_zhoumingzheng.module.DataBean;
import com.example.music_zhoumingzheng.utils.PlaylistManager;

import java.io.IOException;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private PlaylistManager playlistManager;
    private final Handler handler = new Handler();
    private Runnable updateProgressTask;
    private static MusicService instance;
    private boolean isplaying = false;
    private int progress = 0; //保存当前播放的进度

    public static  synchronized MusicService getInstance() {
        if(instance == null )
            instance = new MusicService();
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        playlistManager = PlaylistManager.getInstance();
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnPreparedListener(mp ->{
            //准备好后更新seekbar
            notifyPrepared();
        });
        mediaPlayer.setOnCompletionListener(mp -> {

            //播放下一首歌曲
            Log.d(TAG, "PlayMode:"+playlistManager.getPlayMode());
            switch (playlistManager.getPlayMode()) {
                case PlaylistManager.PLAY_MODE_REPEAT_ONE:  //单曲循环
                    Log.d(TAG, "PlayMode:Repeat");
                    play(playlistManager.getCurrentIndex());
                    break;
                case PlaylistManager.PLAY_MODE_SHUFFLE: //随机一首歌
                    Log.d(TAG, "PlayMode:Random");
                    playlistManager.shuffle();
                    play(playlistManager.getCurrentIndex());
                    break;
                case PlaylistManager.PLAY_MODE_SEQUENCE:
                default:
                    Log.d(TAG, "PlayMode:default");
                    playlistManager.next();
                    play(playlistManager.getCurrentIndex());
                    break;
            }
            //广播通知UI更新界面
            notifyUI();
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        playMusic();
        return START_NOT_STICKY;
    }

    public void playMusic() {
        if (playlistManager.getPlayList().isEmpty()) {
            return;
        }

        DataBean.Data.Record.MusicInfo currentMusic = playlistManager.getCurrentMusic();

        if (currentMusic != null) {
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(currentMusic.musicUrl);
                mediaPlayer.prepare();
                mediaPlayer.start();
                isplaying = true;
                notifyUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void playNext() {
        playlistManager.next();
        progress = 0;   //重置播放进度
        playMusic();
    }

    private void playPrevious() {
        playlistManager.previous();
        progress = 0;   //重置播放进度
        playMusic();
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateProgressTask);
    }

    public void play(int index) {
        if (!playlistManager.getPlayList().isEmpty()) {
            DataBean.Data.Record.MusicInfo musicInfo = playlistManager.getCurrentMusic();
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(musicInfo.musicUrl);
                mediaPlayer.prepare();
                mediaPlayer.start();
                startUpdatingProgress();
                isplaying = true;
                notifyUI();
            } catch (IOException e) {
                Log.e(TAG, "Error playing music", e);
            }
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            //保存当前的位置
            progress = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            isplaying = false;
        }
    }

    public void setProgress(int position){
        progress = position;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public boolean isPlaying() {
        return isplaying;
    }

    private void startUpdatingProgress() {
        updateProgressTask = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    Intent intent = new Intent("UPDATE_PROGRESS");
                    intent.putExtra("currentPosition", mediaPlayer.getCurrentPosition());
                    intent.putExtra("duration",mediaPlayer.getDuration());
                    sendBroadcast(intent);
                    Intent intent2 = new Intent("UPDATE_FLOATING_VIEW");    //更新悬浮view
                    sendBroadcast(intent2);
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(updateProgressTask);
    }

    public void switchMode() {
        playlistManager.switchMode();
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public void resumeMusic(){
        //恢复播放
        if(mediaPlayer == null)
            mediaPlayer = new MediaPlayer();
        if(!mediaPlayer.isPlaying()){
            //未在播放（已暂停、已停止或未开始）
            mediaPlayer.seekTo(progress);   //恢复播放位置
            mediaPlayer.start();
            isplaying = true;
        }
    }

    private void notifyUI() {
        //广播通知ui更新界面
        Intent intent = new Intent("com.example.music_zhoumingzheng.UPDATE_UI");
        intent.putExtra("currentIndex", playlistManager.getCurrentIndex());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        //广播通知悬浮view更新界面
        Log.d(TAG, "notifyUI: 发送更新悬浮view广播");
        Intent intent2 = new Intent("com.example.music_zhoumingzheng.UPDATE_FLOATING_VIEW");
        intent2.putExtra("currentIndex", playlistManager.getCurrentIndex());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent2);
    }

    private void notifyPrepared() {
        //广播通知ui更新界面
        Intent intent = new Intent("com.example.music_zhoumingzheng.PREPARED");
        intent.putExtra("prepared", playlistManager.getCurrentIndex());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        //广播通知悬浮view更新界面
        Intent intent2 = new Intent("com.example.music_zhoumingzheng.UPDATE_FLOATING_VIEW");
        intent2.putExtra("currentIndex", playlistManager.getCurrentIndex());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent2);
    }


}


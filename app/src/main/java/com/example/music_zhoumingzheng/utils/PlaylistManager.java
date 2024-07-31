package com.example.music_zhoumingzheng.utils;

import com.example.music_zhoumingzheng.module.DataBean.Data.Record.MusicInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PlaylistManager {
    public static final int PLAY_MODE_SEQUENCE = 0;
    public static final int PLAY_MODE_SHUFFLE = 1;
    public static final int PLAY_MODE_REPEAT_ONE = 2;

    private List<MusicInfo> playList;
    private int currentIndex;
    private int playMode = 0;
    private static PlaylistManager instance;


    private PlaylistManager() {
        playList = new ArrayList<>();
        currentIndex = 0;
        playMode = PLAY_MODE_SEQUENCE;
    }

    public static synchronized PlaylistManager getInstance() {
        if (instance == null) {
            instance = new PlaylistManager();
        }
        return instance;
    }

    public List<MusicInfo> getPlayList() {
        return playList;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public MusicInfo getCurrentMusic() {
        return playList.get(currentIndex);
    }

    public void addMusic(MusicInfo musicInfo) {
        //先检查是否已经存在了该首歌曲，避免重复添加
        for(int i=0;i<playList.size();i++) {
            if (playList.get(i).id == musicInfo.id)
                return;
        }
        playList.add(musicInfo);

    }


    public int getPlayMode() {
        return playMode;
    }

    public void next() {
        if (playMode == PLAY_MODE_SEQUENCE || playMode == PLAY_MODE_REPEAT_ONE) {
            currentIndex = (currentIndex + 1) % playList.size();
        } else if (playMode == PLAY_MODE_SHUFFLE) {
            currentIndex = (int) (Math.random() * playList.size())%playList.size();
        }
    }

    public void previous() {
        if (playMode == PLAY_MODE_SEQUENCE || playMode == PLAY_MODE_REPEAT_ONE) {
            currentIndex = (currentIndex - 1 + playList.size()) % playList.size();
        } else if (playMode == PLAY_MODE_SHUFFLE) {
            currentIndex = (int) (Math.random() * playList.size())%playList.size();
        }
    }

    public void shuffle() {
        Collections.shuffle(playList);
        currentIndex = (int) (Math.random() * playList.size())%playList.size();
    }

    public void switchMode() {
        playMode = (playMode+1)%3;
    }

    public void addAll(List<MusicInfo> musicInfos, int position) {
        // 获取position对应的歌曲ID
        int targetId = musicInfos.get(position).id;

        // 创建一个临时列表来保存最终需要添加的歌曲
        List<MusicInfo> songsToAdd = new ArrayList<>();

        // 遍历要添加的歌曲列表，过滤出不在播放列表中的歌曲
        for (MusicInfo newMusic : musicInfos) {
            boolean exists = false;
            for (MusicInfo existingMusic : playList) {
                if (existingMusic.id == newMusic.id) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                songsToAdd.add(newMusic);
            }
        }

        // 将需要添加的歌曲添加到播放列表的队首
        playList.addAll(0, songsToAdd);

        // 更新currentIndex为目标歌曲在最终播放列表中的位置
        for (int i = 0; i < playList.size(); i++) {
            if (playList.get(i).id==targetId) {
                currentIndex = i;
                break;
            }
        }
    }

    public void setCurrentIndex(int position) {
        //当前播放的歌曲切换为position
        if(position>-1&&position<playList.size())
            currentIndex = position;
    }

    public void removeMusic(int position) {
        if (position >= 0 && position < playList.size()) {
            if(position == playList.size()-1)
                currentIndex--;
            playList.remove(position);
        }
        //除非删除最后一首歌且currentIndex是最后一首歌，否则不变

    }


}




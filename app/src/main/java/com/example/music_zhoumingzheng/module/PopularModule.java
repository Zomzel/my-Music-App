package com.example.music_zhoumingzheng.module;

import java.util.List;

import com.example.music_zhoumingzheng.module.DataBean.Data.Record.MusicInfo;


public class PopularModule extends BaseModule {
    private final List<MusicInfo> musicInfos;

    public PopularModule(List<MusicInfo> musicInfos) {
        this.musicInfos = musicInfos;
    }

    public List<MusicInfo> getMusicInfos() {
        return musicInfos;
    }

    @Override
    public int getType() {
        return TYPE_POPULAR;
    }
}


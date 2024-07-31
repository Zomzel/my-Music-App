package com.example.music_zhoumingzheng.module;

import java.util.List;

public abstract class BaseModule {



    public List<DataBean.Data.Record.MusicInfo> musicInfos;
    public static final int TYPE_TITLE = 0;
    public static final int TYPE_BANNER = 1;
    public static final int TYPE_CARD = 2;
    public static final int TYPE_RECOMMEND = 3;
    public static final int TYPE_POPULAR = 4;

    abstract public int getType();
}

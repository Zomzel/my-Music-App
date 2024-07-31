package com.example.music_zhoumingzheng.module;

public class TitleModule extends BaseModule {
    private String title;

    public TitleModule(String title) {
        this.title = title;
    }

    public String getTittle() {
        return title;
    }

    @Override
    public int getType() {
        return TYPE_TITLE;
    }
}
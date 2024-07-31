package com.example.music_zhoumingzheng.module;

import com.google.gson.annotations.SerializedName;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DataBean {
    @SerializedName("code")
    public  int code;

    @SerializedName("msg")
    public  String msg;

    @SerializedName("data")
    public  Data data;

    // Getters and setters

    public static class Data {
        @SerializedName("total")
        public  int total;

        @SerializedName("size")
        public  int size;

        @SerializedName("current")
        public  int current;

        @SerializedName("pages")
        public  int pages;

        @SerializedName("records")
        public  List<Record> records;

        // Getters and setters

        public static class Record {
            @SerializedName("moduleConfigId")
            public int moduleConfigId;

            @SerializedName("moduleName")
            public String moduleName;

            @SerializedName("style")
            public  int style;

            @SerializedName("musicInfoList")
            public  List<MusicInfo> musicInfoList;

            // Getters and setters

            public static class MusicInfo {
                @SerializedName("id")
                public  int id;

                @SerializedName("musicName")
                public  String musicName;

                @SerializedName("author")
                public  String author;

                @SerializedName("coverUrl")
                public  String coverUrl;

                @SerializedName("musicUrl")
                public  String musicUrl;

                @SerializedName("lyricUrl")
                public  String lyricUrl;

                // Getters and setters
            }
        }
    }

}


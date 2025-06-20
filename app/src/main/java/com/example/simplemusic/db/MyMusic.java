package com.example.simplemusic.db;

import org.litepal.crud.LitePalSupport;

public class MyMusic extends LitePalSupport {

    public String artist;   //歌手
    public String title;     //歌曲名
    public String songUrl;     //歌曲地址
    public String imgUrl;
    public boolean isOnlineMusic;
    public long duration;
    public String path;

    public MyMusic(String songUrl, String title, String artist, String imgUrl, boolean isOnlineMusic, long duration, String path) {
        this.title = title;
        this.artist = artist;
        this.songUrl = songUrl;
        this.imgUrl = imgUrl;
        this.isOnlineMusic = isOnlineMusic;
        this.duration = duration;
        this.path = path;
    }
}
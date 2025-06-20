package com.example.simplemusic.db;

import org.litepal.crud.LitePalSupport;

public class PlayingMusic  extends LitePalSupport {

    public String artist;   //歌手
    public String title;     //歌曲名
    public String songUrl;     //歌曲地址
    public String imgUrl;
    public long rutation;
    public boolean isOnlineMusic;
    public String path;

    public PlayingMusic(String songUrl, String title, String artist, String imgUrl, boolean isOnlineMusic, long rutation, String path) {
        this.title = title;
        this.artist = artist;
        this.songUrl = songUrl;
        this.imgUrl = imgUrl;
        this.rutation = rutation;
        this.isOnlineMusic = isOnlineMusic;
        this.path = path;
    }
}

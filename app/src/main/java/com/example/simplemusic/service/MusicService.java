package com.example.simplemusic.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.example.simplemusic.activity.MainActivity;
import com.example.simplemusic.adapter.MusicAdapter;
import com.example.simplemusic.bean.Music;
import com.example.simplemusic.util.Utils;
import com.example.simplemusic.db.PlayingMusic;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private static final String TAG = "abc";
    private MediaPlayer player;
    private List<Music> playingMusicList;
    private List<OnStateChangeListenr> listenrList;
    private MusicServiceBinder binder;
    private AudioManager audioManager;
    private Music currentMusic; // 当前就绪的音乐
    private  boolean autoPlayAfterFocus;    // 获取焦点之后是否自动播放
    private boolean isNeedReload;     // 播放时是否需要重新加载
    private int playMode;  // 播放模式
    private SharedPreferences spf;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MusicChannel";
    // 媒体会话
    //private MediaSessionCompat mediaSession;

    // 当前播放信息
    private String currentSongTitle = "歌曲标题";
    private String currentArtist = "歌手名";
    private String currentAlbum = "专辑名";

    // 广播接收器动作
    public static final String NOTI_ACTION_PLAY = "com.example.action.PLAY";
    public static final String NOTI_ACTION_PAUSE = "com.example.action.PAUSE";
    public static final String NOTI_ACTION_PREVIOUS = "com.example.action.PREVIOUS";
    public static final String NOTI_ACTION_NEXT = "com.example.action.NEXT";

    /*
    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "MusicService");

        // 设置回调
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                playInner();
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseInner();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                playPreInner();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                playNextInner();
            }

        });

        // 启用媒体按钮和传输控制
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // 设置初始播放状态
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);

        // 激活会话
        mediaSession.setActive(true);
    }


    private void updatePlaybackState(int state) {
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();

        // 设置可用的操作
        long actions = PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_STOP;

        // 设置状态和可用操作
        playbackStateBuilder.setState(state, 0, 1.0f)
                .setActions(actions);

        mediaSession.setPlaybackState(playbackStateBuilder.build());
    }
    */

    @Override
    public void onCreate() {
        super.onCreate();
        initPlayList();     //初始化播放列表
        // 初始化媒体会话
        //initMediaSession();
        listenrList = new ArrayList<>();    //初始化监听器列表
        player = new MediaPlayer();   //初始化播放器
        player.setOnCompletionListener(onCompletionListener);   //设置播放完成的监听器
        binder = new MusicServiceBinder();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE); //获得音频管理服务
        createNotificationChannel();

    }

    // 当服务被系统强制终止时调用
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // 尝试重启服务
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
    }

//    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            switch (action) {
                case NOTI_ACTION_PLAY:
                case NOTI_ACTION_PAUSE:
                    binder.playOrPause();
                    break;
                case NOTI_ACTION_PREVIOUS:
                    binder.playPre();
                    break;
                case NOTI_ACTION_NEXT:
                    binder.playNext();
                    break;
            }
        }

        // 启动前台服务 - 这是关键
        if (!isServiceRunning) {
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification);
            isServiceRunning = true;
        }
        // 如果服务被系统杀死，将尝试重新启动
        return START_STICKY;
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "音乐播放",
                    NotificationManager.IMPORTANCE_LOW // 低优先级，不会打断用户
            );
            channel.setDescription("音乐播放器通知");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private PendingIntent createActionIntent(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    //@RequiresApi(api = Build.VERSION_CODES.O)
    private Notification createNotification() {
        // 创建一个点击通知时打开的 Intent
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        if (currentMusic != null) {
            currentSongTitle = currentMusic.title;
            currentArtist = currentMusic.artist;
            currentAlbum = MusicAdapter.secondsToMMSS(currentMusic.duration);
        }

        // 创建媒体样式
        MediaStyle mediaStyle = new MediaStyle()
//                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2); // 在紧凑视图中显示的操作索引

        // 创建媒体控制意图
        PendingIntent playPauseIntent = createActionIntent(player.isPlaying() ? NOTI_ACTION_PAUSE : NOTI_ACTION_PLAY);
        PendingIntent previousIntent = createActionIntent(NOTI_ACTION_PREVIOUS);
        PendingIntent nextIntent = createActionIntent(NOTI_ACTION_NEXT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentSongTitle)
                .setContentText(currentArtist)
                .setSubText(currentAlbum)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
//                .setStyle(mediaStyle);
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .setOngoing(true);
//                .setOnlyAlertOnce(true);

        //添加媒体控制按钮
        builder.addAction(android.R.drawable.ic_media_previous, "上一首", previousIntent); // 索引 0

        // 根据播放状态添加播放/暂停按钮
        if (player.isPlaying()) {
            builder.addAction(android.R.drawable.ic_media_pause, "暂停", playPauseIntent); // 索引 1
        } else {
            builder.addAction(android.R.drawable.ic_media_play, "播放", playPauseIntent); // 索引 1
        }
        builder.addAction(android.R.drawable.ic_media_next, "下一首", nextIntent); // 索引 2

        builder.setStyle(mediaStyle);
        return builder.build();
    }


    private boolean isServiceRunning = false;
    private void updateNotification() {
        Log.d(TAG, "更新通知: " + currentSongTitle + " - " + currentArtist);

        // 检查服务是否在前台运行
        if (!isServiceRunning) {
            Log.d(TAG, "服务不在前台运行，启动前台服务");
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification);
            isServiceRunning = true;
            return;
        }

        // 服务已在前台运行，只需更新通知
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            try {
                Notification notification = createNotification();
                notificationManager.notify(NOTIFICATION_ID, notification);
                Log.d(TAG, "通知已更新");
            } catch (Exception e) {
                Log.e(TAG, "更新通知失败: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "无法获取NotificationManager");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player.isPlaying()) {
            player.stop();
        }
        player.release();

        playingMusicList.clear();
        listenrList.clear();
        handler.removeMessages(66);
        audioManager.abandonAudioFocus(audioFocusListener); //注销音频管理服务
    }

    //对外监听器接口
    public interface OnStateChangeListenr {
        void onPlayProgressChange(long played, long duration);  //播放进度变化
        void onPlay(Music item);    //播放状态变化
        void onPause();   //播放状态变化
    }

    //定义binder与活动通信
    public class MusicServiceBinder extends Binder {

        // 添加一首歌曲
        public void addPlayList(Music item) {
            addPlayListInner(item);
        }

        // 添加多首歌曲
        public void addPlayList(List<Music> items) {
            addPlayListInner(items);
        }

        // 移除一首歌曲
        public void removeMusic(int i) {
            removeMusicInner(i);
        }

        public void playOrPause(){
            if (player.isPlaying()){
                pauseInner();
            }
            else {
                playInner();
            }
        }

        // 下一首
        public void playNext() {
            playNextInner();
        }

        // 上一首
        public void playPre() {
            playPreInner();
        }

        // 获取当前播放模式
        public int getPlayMode(){
            return getPlayModeInner();
        }

        // 设置播放模式
        public void setPlayMode(int mode){
            setPlayModeInner(mode);
        }

        // 设置播放器进度
        public void seekTo(int pos) {
            seekToInner(pos);
        }

        // 获取当前就绪的音乐
        public Music getCurrentMusic() {
            return getCurrentMusicInner();
        }

        // 获取播放器播放状态
        public boolean isPlaying() {
            return isPlayingInner();
        }

        // 获取播放列表
        public List<Music> getPlayingList() {
            return getPlayingListInner();
        }

        // 注册监听器
        public void registerOnStateChangeListener(OnStateChangeListenr l) {
            listenrList.add(l);
        }

        // 注销监听器
        public void unregisterOnStateChangeListener(OnStateChangeListenr l) {
            listenrList.remove(l);
        }
    }

    private void addPlayListInner(Music music){
        if (!playingMusicList.contains(music)) {
            playingMusicList.add(0, music);
            PlayingMusic playingMusic = new PlayingMusic(music.songUrl, music.title, music.artist, music.imgUrl, music.isOnlineMusic, music.duration, music.path);
            playingMusic.save();
        }
        currentMusic = music;
        SaveCurrentMusic();
        isNeedReload = true;
        playInner();
    }

    private void addPlayListInner(List<Music> musicList){
        playingMusicList.clear();
        LitePal.deleteAll(PlayingMusic.class);
        playingMusicList.addAll(musicList);
        for (Music i: musicList){
            PlayingMusic playingMusic = new PlayingMusic(i.songUrl, i.title, i.artist, i.imgUrl, i.isOnlineMusic, i.duration, i.path);
            playingMusic.save();
        }
        currentMusic = playingMusicList.get(0);
        SaveCurrentMusic();
        playInner();
    }

    private void removeMusicInner(int i){
        LitePal.deleteAll(PlayingMusic.class, "title=?", playingMusicList.get(i).title);
        playingMusicList.remove(i);
    }

    private void playInner() {

        //获取音频焦点
        audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        //如果之前没有选定要播放的音乐，就选列表中的第一首音乐开始播放
        if (currentMusic == null && playingMusicList.size() > 0) {
            currentMusic = playingMusicList.get(0);
            SaveCurrentMusic();
            isNeedReload = true;
        }

        playMusicItem(currentMusic, isNeedReload);

        updateNotification();
    }

    private void pauseInner(){
        player.pause();

        for (OnStateChangeListenr l : listenrList) {
            l.onPause();
        }
        // 暂停后不需要重新加载
        isNeedReload = false;
        updateNotification();
    }

    private void playPreInner(){
        //获取当前播放（或者被加载）音乐的上一首音乐
        //如果前面有要播放的音乐，把那首音乐设置成要播放的音乐
        int currentIndex = playingMusicList.indexOf(currentMusic);
        if (currentIndex - 1 >= 0) {
            currentMusic = playingMusicList.get(currentIndex - 1);
        } else {
            currentMusic = playingMusicList.get(playingMusicList.size()-1);
        }
        SaveCurrentMusic();
        isNeedReload = true;
        playInner();

    }

    private void SaveCurrentMusic() {
        updateNotification();
        SharedPreferences sharedPref = getSharedPreferences("music_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("songUrl", currentMusic.songUrl);
        editor.putString("path", currentMusic.path);
        editor.putString("title", currentMusic.title);
        editor.putString("artist", currentMusic.artist);
        editor.putString("imgUrl", currentMusic.imgUrl);
        editor.putBoolean("isOnlineMusic", currentMusic.isOnlineMusic);
        editor.putLong("duration", currentMusic.duration);

        editor.apply();
    }
    private void playNextInner() {

        if (playMode == Utils.TYPE_RANDOM){
            //随机播放
            int i = (int) (0 + Math.random() * (playingMusicList.size() + 1));
            currentMusic = playingMusicList.get(i);
        }
        else {
            //列表循环
            int currentIndex = playingMusicList.indexOf(currentMusic);
            if (currentIndex < playingMusicList.size() - 1) {
                currentMusic = playingMusicList.get(currentIndex + 1);
            } else {
                currentMusic = playingMusicList.get(0);
            }
        }
        SaveCurrentMusic();
        isNeedReload = true;
        playInner();
    }

    private void seekToInner(int pos){
        //将音乐拖动到指定的时间
        player.seekTo(pos);
    }

    private Music getCurrentMusicInner(){
        return currentMusic;
    }

    private boolean isPlayingInner(){
        return player.isPlaying();
    }

    public List<Music> getPlayingListInner(){
        return playingMusicList;
    }

    private int getPlayModeInner(){
        return playMode;
    }

    private void setPlayModeInner(int mode){
        playMode = mode;
    }

    // 将要播放的音乐载入MediaPlayer，但是并不播放
    private void prepareToPlay(Music item) {
        try {
            player.reset();
            //设置播放音乐的地址
            player.setDataSource(MusicService.this, Uri.parse(item.songUrl));
            //准备播放音乐
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 播放音乐，根据reload标志位判断是非需要重新加载音乐
    private void playMusicItem(Music item, boolean reload) {

        if (item == null) {
            return;
        }

        if (reload) {
            //需要重新加载音乐
            prepareToPlay(item);
        }
        player.start();
        for (OnStateChangeListenr l : listenrList) {
            l.onPlay(item);
        }
        isNeedReload = true;

        //移除现有的更新消息，重新启动更新
        handler.removeMessages(66);
        handler.sendEmptyMessage(66);
    }

    Music LoadCurrentMusic() {
        SharedPreferences sharedPref = getSharedPreferences("music_data", Context.MODE_PRIVATE);

        String songUrl = sharedPref.getString("songUrl", "");
        String title = sharedPref.getString("title", "未知标题");
        String artist = sharedPref.getString("artist", "未知歌手");
        String imgUrl = sharedPref.getString("imgUrl", null);
        boolean isOnlineMusic = sharedPref.getBoolean("isOnlineMusic", false);
        Long duration = sharedPref.getLong("duration", 0);
        String path = sharedPref.getString("path", null);

        Music m = new Music(songUrl, title, artist, imgUrl, isOnlineMusic, duration, path);
        return m;
    }
    // 初始化播放列表
    private void initPlayList() {
        playingMusicList = new ArrayList<>();
        List<PlayingMusic> list = LitePal.findAll(PlayingMusic.class);
        for (PlayingMusic i : list) {
            Music m = new Music(i.songUrl, i.title, i.artist, i.imgUrl, i.isOnlineMusic, 0, i.path);
            playingMusicList.add(m);
        }
        //if (playingMusicList.size() > 0)
        {
            currentMusic = LoadCurrentMusic();
            isNeedReload = true;
        }
    }

    //当前歌曲播放完成的监听器
    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {

            Utils.count ++; //累计听歌数量+1

            if (playMode == Utils.TYPE_SINGLE) {
                //单曲循环
                isNeedReload = true;
                playInner();
            }
            else {
                playNextInner();
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 66:
                    //通知监听者当前的播放进度
                    long played = player.getCurrentPosition();
                    long duration = player.getDuration();
                    for (OnStateChangeListenr l : listenrList) {
                        l.onPlayProgressChange(played, duration);
                    }
                    //间隔一秒发送一次更新播放进度的消息
                    sendEmptyMessageDelayed(66, 1000);
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        //当组件bindService()之后，将这个Binder返回给组件使用
        return binder;
    }

    //焦点控制
    private AudioManager.OnAudioFocusChangeListener audioFocusListener = new AudioManager.OnAudioFocusChangeListener(){

        public void onAudioFocusChange(int focusChange) {
            switch(focusChange){
                case AudioManager.AUDIOFOCUS_LOSS:
                    if(player.isPlaying()){
                        //会长时间失去，所以告知下面的判断，获得焦点后不要自动播放
                        autoPlayAfterFocus = false;
                        pauseInner();//因为会长时间失去，所以直接暂停
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if(player.isPlaying()){
                        //短暂失去焦点，先暂停。同时将标志位置成重新获得焦点后就开始播放
                        autoPlayAfterFocus = true;
                        pauseInner();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if(player.isPlaying()){
                        //短暂失去焦点，先暂停。同时将标志位置成重新获得焦点后就开始播放
                        autoPlayAfterFocus = true;
                        pauseInner();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    //重新获得焦点，且符合播放条件，开始播放
                    if(!player.isPlaying()&& autoPlayAfterFocus){
                        autoPlayAfterFocus = false;
                        playInner();
                    }
                    break;
            }
        }
    };
}

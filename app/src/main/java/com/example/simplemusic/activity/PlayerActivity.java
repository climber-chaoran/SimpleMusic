package com.example.simplemusic.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.simplemusic.bean.Music;
import com.example.simplemusic.adapter.PlayingMusicAdapter;
import com.example.simplemusic.R;
import com.example.simplemusic.util.Utils;
import com.example.simplemusic.service.MusicService;

import java.util.List;
import java.util.Objects;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView                                    musicTitleView;
    private TextView                                    musicArtistView;
    private ImageView                                   musicImgView;
    private ImageView                                   btnPlayMode;
    private ImageView                                   btnPlayPre;
    private ImageView                                   btnPlayOrPause;
    private ImageView                                   btnPlayNext;
    private ImageView                                   btnPlayingList;
    private TextView                                    nowTimeView;
    private TextView                                    totalTimeView;
    private SeekBar                                     seekBar;
    private com.example.simplemusic.view.RotateAnimator rotateAnimator;
    private MusicService.MusicServiceBinder             serviceBinder;

    // 在你的 Activity 中
    //private MediaSessionCompat mediaSession;

    /*
    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "MusicPlayerSession");

        // 设置可用的媒体控制动作
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE | // 添加这个
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_STOP |
                                PlaybackStateCompat.ACTION_SEEK_TO |
                                PlaybackStateCompat.ACTION_FAST_FORWARD | // 添加这个
                                PlaybackStateCompat.ACTION_REWIND // 添加这个
                );

        mediaSession.setPlaybackState(stateBuilder.build());

        // 设置媒体会话回调
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // 添加处理媒体按钮事件的方法
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                Log.e("1", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
                KeyEvent keyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = keyEvent.getKeyCode();
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            serviceBinder.playPre();
                            return true;
                    }
                }
                // 让系统继续处理其他按键事件
                return super.onMediaButtonEvent(mediaButtonIntent);
            }

            @Override
            public void onPlay() {
                serviceBinder.playOrPause();
                Log.e("1", "++++++++++++++++++++++++++++++++playOrPause");
            }

            @Override
            public void onPause() {
                serviceBinder.playOrPause();
                Log.e("1", "++++++++++++++++++++++++++++++++playOrPause");
            }

            @Override
            public void onSkipToNext() {
                serviceBinder.playNext();
                Log.e("1", "++++++++++++++++++++++++++++++++onSkipToNext");
            }

            @Override
            public void onSkipToPrevious() {
                serviceBinder.playPre();
            }
        });

        // 设置会话为活动状态
        mediaSession.setActive(true);

        // 初始化更新元数据和播放状态
        updateMetadata();
        updatePlaybackState();
    }*/

    private void updatePlaybackState() {/*
        if (mediaSession == null || mediaPlayer == null) return;

        long position = mediaPlayer.getCurrentPosition();

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                );

        int state;
        if (mediaPlayer.isPlaying()) {
            state = PlaybackStateCompat.STATE_PLAYING;
        } else {
            state = PlaybackStateCompat.STATE_PAUSED;
        }

        stateBuilder.setState(state, position, 1.0f);
        mediaSession.setPlaybackState(stateBuilder.build());*/
    }

    private void updateMetadata() {/*
        if (mediaSession == null || localMusicList == null || localMusicList.isEmpty() ||
                currentMusicIndex < 0 || currentMusicIndex >= localMusicList.size()) return;

        Music currentMusic = localMusicList.get(currentMusicIndex);

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentMusic.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentMusic.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentMusic.duration * 1000);

        mediaSession.setMetadata(metadataBuilder.build());*/
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        //初始化
        initActivity();
        //initMediaSession();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
//        if (mediaSession != null) {
//            mediaSession.setActive(false);
//            mediaSession.release();
//        }
    }

    // 控件监听
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.play_mode:
                // 改变播放模式
                int mode = serviceBinder.getPlayMode();
                switch (mode){
                    case Utils.TYPE_ORDER:
                        serviceBinder.setPlayMode(Utils.TYPE_SINGLE);
                        Toast.makeText(PlayerActivity.this, "单曲循环", Toast.LENGTH_SHORT).show();
                        btnPlayMode.setImageResource(R.drawable.ic_singlerecycler);
                        break;
                    case Utils.TYPE_SINGLE:
                        serviceBinder.setPlayMode(Utils.TYPE_RANDOM);
                        Toast.makeText(PlayerActivity.this, "随机播放", Toast.LENGTH_SHORT).show();
                        btnPlayMode.setImageResource(R.drawable.ic_random);
                        break;
                    case Utils.TYPE_RANDOM:
                        serviceBinder.setPlayMode(Utils.TYPE_ORDER);
                        Toast.makeText(PlayerActivity.this, "列表循环", Toast.LENGTH_SHORT).show();
                        btnPlayMode.setImageResource(R.drawable.ic_playrecycler);
                        break;
                    default:
                }
                break;
            case R.id.play_pre:
                // 上一首
                serviceBinder.playPre();
                break;
            case R.id.play_next:
                // 下一首
                serviceBinder.playNext();
                break;
            case R.id.play_or_pause:
                // 播放或暂停
                serviceBinder.playOrPause();
                break;
            case R.id.playing_list:
                // 播放列表
                showPlayList();
                break;
            default:
        }
    }

    private void initActivity() {
        musicTitleView = findViewById(R.id.title);
        musicArtistView = findViewById(R.id.artist);
        musicImgView = findViewById(R.id.imageView);
        btnPlayMode = findViewById(R.id.play_mode);
        btnPlayOrPause = findViewById(R.id.play_or_pause);
        btnPlayPre = findViewById(R.id.play_pre);
        btnPlayNext = findViewById(R.id.play_next);
        btnPlayingList = findViewById(R.id.playing_list);
        seekBar = findViewById(R.id.seekbar);
        nowTimeView = findViewById(R.id.current_time);
        totalTimeView = findViewById(R.id.total_time);
        ImageView needleView = findViewById(R.id.ivNeedle);

        // ToolBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(toolbar);

        // 设置监听
        btnPlayMode.setOnClickListener(this);
        btnPlayOrPause.setOnClickListener(this);
        btnPlayPre.setOnClickListener(this);
        btnPlayNext.setOnClickListener(this);
        btnPlayingList.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //拖动进度条时
                nowTimeView.setText(Utils.formatTime((long) progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                serviceBinder.seekTo(seekBar.getProgress());
            }
        });

        //初始化动画
        rotateAnimator = new com.example.simplemusic.view.RotateAnimator(this, musicImgView, needleView);
        rotateAnimator.set_Needle();

        // 绑定service
        Intent i = new Intent(this, MusicService.class);
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);
    }

    //显示当前正在播放的音乐
    private void showPlayList(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("播放列表");

        //获取播放列表
        final List<Music> playingList = serviceBinder.getPlayingList();

        if(playingList.size() > 0) {
            //播放列表有曲目，显示所有音乐
            final PlayingMusicAdapter playingAdapter = new PlayingMusicAdapter(this, R.layout.playinglist_item, playingList);
            builder.setAdapter(playingAdapter, new DialogInterface.OnClickListener() {
                //监听列表项点击事件
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    serviceBinder.addPlayList(playingList.get(which));
                }
            });

            //列表项中删除按钮的点击事件
            playingAdapter.setOnDeleteButtonListener(new PlayingMusicAdapter.onDeleteButtonListener() {
                @Override
                public void onClick(int i) {
                    serviceBinder.removeMusic(i);
                    playingAdapter.notifyDataSetChanged();
                }
            });
        }
        else {
            //播放列表没有曲目，显示没有音乐
            builder.setMessage("没有正在播放的音乐");
        }
        builder.setCancelable(true);
        builder.create().show();
    }

    //定义与服务的连接的匿名类
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //绑定成功后，取得MusicSercice提供的接口
            serviceBinder = (MusicService.MusicServiceBinder) service;

            //注册监听器
            serviceBinder.registerOnStateChangeListener(listenr);

            //获得当前音乐
            Music item = serviceBinder.getCurrentMusic();

            if(item == null) {
                //当前音乐为空, seekbar不可拖动
                seekBar.setEnabled(false);
            }
            else if (serviceBinder.isPlaying()){
                //如果正在播放音乐, 更新信息
                musicTitleView.setText(item.title);
                musicArtistView.setText(item.artist);
                btnPlayOrPause.setImageResource(R.drawable.ic_pause);
                rotateAnimator.playAnimator();
                if (item.isOnlineMusic){
                    Glide.with(getApplicationContext())
                            .load(item.imgUrl)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(musicImgView);
                }
                else {
                    ContentResolver resolver = getContentResolver();
                    Bitmap img = Utils.getLocalMusicBmp(item.path);
                    Glide.with(getApplicationContext())
                            .load(img)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(musicImgView);
                }
            }
            else {
                //当前有可播放音乐但没有播放
                musicTitleView.setText(item.title);
                musicArtistView.setText(item.artist);
                btnPlayOrPause.setImageResource(R.drawable.ic_play);
                if (item.isOnlineMusic){
                    Glide.with(getApplicationContext())
                            .load(item.imgUrl)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(musicImgView);
                }
                else {
                    ContentResolver resolver = getContentResolver();
                    Bitmap img = Utils.getLocalMusicBmp(item.path);
                    Glide.with(getApplicationContext())
                            .load(img)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(musicImgView);
                }
            }

            // 获取当前播放模式
            int mode = (serviceBinder.getPlayMode());
            switch (mode){
                case Utils.TYPE_ORDER:
                    btnPlayMode.setImageResource(R.drawable.ic_playrecycler);
                    break;
                case Utils.TYPE_SINGLE:
                    btnPlayMode.setImageResource(R.drawable.ic_singlerecycler);
                    break;
                case Utils.TYPE_RANDOM:
                    btnPlayMode.setImageResource(R.drawable.ic_random);
                    break;
                default:
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //断开连接之后, 注销监听器
            serviceBinder.unregisterOnStateChangeListener(listenr);
        }
    };

    //实现监听器监听MusicService的变化，
    private MusicService.OnStateChangeListenr listenr = new MusicService.OnStateChangeListenr() {

        @Override
        public void onPlayProgressChange(long played, long duration) {
            seekBar.setMax((int) duration);
            totalTimeView.setText(Utils.formatTime(duration));
            nowTimeView.setText(Utils.formatTime(played));
            seekBar.setProgress((int) played);
        }

        @Override
        public void onPlay(final Music item) {
            //变为播放状态时
            musicTitleView.setText(item.title);
            musicArtistView.setText(item.artist);
            btnPlayOrPause.setImageResource(R.drawable.ic_pause);
            rotateAnimator.playAnimator();
            if (item.isOnlineMusic){
                Glide.with(getApplicationContext())
                        .load(item.imgUrl)
                        .placeholder(R.drawable.defult_music_img)
                        .error(R.drawable.defult_music_img)
                        .into(musicImgView);
            }
            else {
                ContentResolver resolver = getContentResolver();
                Bitmap img = Utils.getLocalMusicBmp(item.path);
                Glide.with(getApplicationContext())
                        .load(img)
                        .placeholder(R.drawable.defult_music_img)
                        .error(R.drawable.defult_music_img)
                        .into(musicImgView);
            }
        }

        @Override
        public void onPause() {
            //变为暂停状态时
            btnPlayOrPause.setImageResource(R.drawable.ic_play);
            rotateAnimator.pauseAnimator();
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        //界面退出时的动画
        overridePendingTransition(R.anim.bottom_silent,R.anim.bottom_out);
    }
}

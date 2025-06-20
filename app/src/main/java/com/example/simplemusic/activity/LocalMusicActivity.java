package com.example.simplemusic.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.simplemusic.R;
import com.example.simplemusic.adapter.MusicAdapter;
import com.example.simplemusic.adapter.PlayingMusicAdapter;
import com.example.simplemusic.bean.Music;
import com.example.simplemusic.db.LocalMusic;
import com.example.simplemusic.service.MusicService;
import com.example.simplemusic.util.Utils;

import org.litepal.LitePal;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LocalMusicActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "LocalMusicActivity";
    private TextView musicCountView;
    private ListView musicListView;
    private TextView playingTitleView;
    private TextView playingArtistView;
    private ImageView playingImgView;
    private ImageView btnPlayOrPause;

    private List<Music> localMusicList;
    private MusicAdapter adapter;
    private MusicService.MusicServiceBinder serviceBinder;
    private MusicUpdateTask updateTask;
    private ProgressDialog progressDialog;

    private boolean sort_duration = true;
    private boolean sort_name = true;

    // 在你的 Activity 中
    private MediaSessionCompat mediaSession;
    private BroadcastReceiver headsetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // 耳机被拔出，自动暂停
                if (serviceBinder != null && serviceBinder.isPlaying()) {
                    serviceBinder.playOrPause();
                }
            }
        }
    };

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
                Log.e("1", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                KeyEvent keyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = keyEvent.getKeyCode();
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        serviceBinder.playOrPause(); // 统一处理播放/暂停
                        return true;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        serviceBinder.playNext();
                        return true;
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
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);

    }

    private void updatePlaybackState(int state) {
        if (serviceBinder == null) return;

        // 获取当前实际播放状态
        boolean actuallyPlaying = false;
        try {
            actuallyPlaying = serviceBinder.isPlaying();
        } catch (Exception e) {
            Log.e(TAG, "检查播放状态出错: " + e.getMessage());
        }

        // 确保状态与实际播放状态一致
        if (state == PlaybackStateCompat.STATE_PLAYING && !actuallyPlaying) {
            Log.w(TAG, "状态不一致: 请求PLAYING但实际未播放，修正为PAUSED");
            state = PlaybackStateCompat.STATE_PAUSED;
        } else if (state == PlaybackStateCompat.STATE_PAUSED && actuallyPlaying) {
            Log.w(TAG, "状态不一致: 请求PAUSED但实际在播放，修正为PLAYING");
            state = PlaybackStateCompat.STATE_PLAYING;
        }

        // 构建播放状态
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();

        // 定义可用的播放控制动作
        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_STOP |
                PlaybackStateCompat.ACTION_SEEK_TO;

        // 根据当前状态添加特定动作
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }

        // 设置状态
        playbackStateBuilder.setActions(actions);

        // 更新媒体会话状态
        mediaSession.setPlaybackState(playbackStateBuilder.build());
    }

    private void startAndBindService() {
        // 创建 Intent
        Intent intent = new Intent(this, MusicService.class);

        // 启动服务（对于 Android 8.0 及以上使用 startForegroundService）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        // 绑定服务
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localmusic);
        startAndBindService();

        //初始化
        initActivity();
        initMediaSession();
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(headsetReceiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        // 列表项点击事件
        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Music music = localMusicList.get(position);
                serviceBinder.addPlayList(music);
            }
        });

        //列表项中更多按钮的点击事件
        adapter.setOnMoreButtonListener(new MusicAdapter.onMoreButtonListener() {
            @Override
            public void onClick(final int i) {
                final Music music = localMusicList.get(i);
                final String[] items = new String[] {"收藏到我的音乐", "添加到播放列表", "删除",  music.path};
                AlertDialog.Builder builder = new AlertDialog.Builder(LocalMusicActivity.this);
                builder.setTitle(music.title+"-"+music.artist);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
                                MainActivity.addMymusic(music);
                                break;
                            case 1:
                                serviceBinder.addPlayList(music);
                                break;
                            case 2:
                                //从列表和数据库中删除
                                localMusicList.remove(i);
                                LitePal.deleteAll(LocalMusic.class, "title=?", music.title);
                                adapter.notifyDataSetChanged();
                                musicCountView.setText("播放全部(共"+ localMusicList.size()+"首)");
                                break;
                        }
                    }
                });
                builder.create().show();
            }
        });

        if (localMusicList.isEmpty()) {
            onClick(this.findViewById(R.id.refresh));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.playing_img:
                new AlertDialog.Builder(this)
                        .setTitle("音乐地址")
                        .setMessage(serviceBinder.getCurrentMusic().path)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // 关闭对话框
                            }
                        })
                        .show();
            break;
            case R.id.play_all:
                serviceBinder.addPlayList(localMusicList);
                break;
            case R.id.sort_by_name:
                //按照音乐名称排序
                Collator collator = Collator.getInstance(Locale.CHINA);
                if (sort_name)
                    Collections.sort(localMusicList, (m1, m2) -> collator.compare(m1.title, m2.title));
                else {
                    Collections.sort(localMusicList, (m1, m2) -> collator.compare(m2.title, m1.title));
                }
                sort_name = !sort_name;
                adapter.notifyDataSetChanged();

                LitePal.deleteAll(LocalMusic.class);
                for (Music data : localMusicList) {
                    LocalMusic music = new LocalMusic(data.songUrl, data.title, data.artist, data.imgUrl, data.isOnlineMusic, data.duration, data.path);
                    music.save();
                }
                Toast.makeText(this, "排序完成", Toast.LENGTH_SHORT).show();
                break;
            case R.id.sort_by_duration:
                //按照音乐时长排序
                if (sort_duration)
                    Collections.sort(localMusicList, (m1, m2) -> Long.compare(m1.duration, m2.duration));
                else {
                    Collections.sort(localMusicList, (m1, m2) -> Long.compare(m2.duration, m1.duration));
                }
                sort_duration = !sort_duration;
                adapter.notifyDataSetChanged();

                LitePal.deleteAll(LocalMusic.class);
                for (Music data : localMusicList) {
                    LocalMusic music = new LocalMusic(data.songUrl, data.title, data.artist, data.imgUrl, data.isOnlineMusic, data.duration, data.path);
                    music.save();
                }
                Toast.makeText(this, "排序完成", Toast.LENGTH_SHORT).show();
                break;
            case R.id.refresh:
                localMusicList.clear();
                LitePal.deleteAll(LocalMusic.class);
                updateTask = new MusicUpdateTask();
                updateTask.execute();
                break;
            case R.id.player:
                Intent intent = new Intent(LocalMusicActivity.this, PlayerActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.bottom_in, R.anim.bottom_silent);
                break;
            case R.id.play_or_pause:
                serviceBinder.playOrPause();
                break;
            case R.id.playing_list:
                showPlayList();
                break;

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(updateTask != null && updateTask.getStatus() == AsyncTask.Status.RUNNING) {
            updateTask.cancel(true);
        }
        updateTask = null;
        localMusicList.clear();
        unbindService(mServiceConnection);
        if (headsetReceiver != null) {
            unregisterReceiver(headsetReceiver);
        }

    }

    private void initActivity(){
        //初始化控件
        ImageView btn_playAll = this.findViewById(R.id.play_all);
        musicCountView = this.findViewById(R.id.play_all_title);
        ImageView btn_refresh = this.findViewById(R.id.refresh);

        ImageView btn_sort_by_duration = this.findViewById(R.id.sort_by_duration);
        ImageView btn_sort_by_name = this.findViewById(R.id.sort_by_name);

        musicListView = this.findViewById(R.id.music_list);
        RelativeLayout playerToolView = this.findViewById(R.id.player);
        playingImgView = this.findViewById(R.id.playing_img);
        playingTitleView = this.findViewById(R.id.playing_title);
        playingArtistView = this.findViewById(R.id.playing_artist);
        btnPlayOrPause = this.findViewById(R.id.play_or_pause);
        ImageView btn_playingList = this.findViewById(R.id.playing_list);

        btn_playAll.setOnClickListener(this);
        btn_refresh.setOnClickListener(this);
        btn_sort_by_duration.setOnClickListener(this);
        btn_sort_by_name.setOnClickListener(this);
        playerToolView.setOnClickListener(this);
        btnPlayOrPause.setOnClickListener(this);
        btn_playingList.setOnClickListener(this);
        playingImgView.setOnClickListener(this);

        localMusicList = new ArrayList<>();


        //绑定播放服务
        Intent i = new Intent(this, MusicService.class);
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);

        // 使用ToolBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("本地音乐");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(toolbar);

        //从数据库获取保存的本地音乐列表
        List<LocalMusic> list = LitePal.findAll(LocalMusic.class);
        for (LocalMusic s:list){
            Music m = new Music(s.songUrl, s.title, s.artist, s.imgUrl, s.isOnlineMusic, s.duration, s.path);
            localMusicList.add(m);
        }

        // 本地音乐列表绑定适配器
        adapter = new MusicAdapter(this, R.layout.music_item, localMusicList);
        musicListView.setAdapter(adapter);

        musicCountView.setText("播放全部(共"+ localMusicList.size()+"首)");
    }

    // 显示当前正在播放的音乐
    private void showPlayList(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //设计对话框的显示标题
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

        //设置该对话框是可以自动取消的，例如当用户在空白处随便点击一下，对话框就会关闭消失
        builder.setCancelable(true);

        //创建并显示对话框
        builder.create().show();
    }


    private boolean isBound = false;
    // 定义与服务的连接的匿名类
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        // 绑定成功时调用
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            // 绑定成功后，取得MusicSercice提供的接口
            serviceBinder = (MusicService.MusicServiceBinder) service;

            // 注册监听器
            serviceBinder.registerOnStateChangeListener(listenr);

            Music item = serviceBinder.getCurrentMusic();

            if (serviceBinder.isPlaying()){
                // 如果正在播放音乐, 更新控制栏信息
                btnPlayOrPause.setImageResource(R.drawable.zanting);
                playingTitleView.setText(item.title);
                playingArtistView.setText(item.artist);
                if (item.isOnlineMusic){
                    Glide.with(getApplicationContext())
                            .load(item.imgUrl)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(playingImgView);
                }
                else {
                    ContentResolver resolver = getContentResolver();
                    Bitmap img = Utils.getLocalMusicBmp(item.path);
                    Glide.with(getApplicationContext())
                            .load(img)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(playingImgView);
                }
            }
            else if (item != null){
                // 当前有可播放音乐但没有播放
                btnPlayOrPause.setImageResource(R.drawable.bofang);
                playingTitleView.setText(item.title);
                playingArtistView.setText(item.artist);
                if (item.isOnlineMusic){
                    Glide.with(getApplicationContext())
                            .load(item.imgUrl)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(playingImgView);
                }
                else {
                    ContentResolver resolver = getContentResolver();
                    Bitmap img = Utils.getLocalMusicBmp(item.path);
                    Glide.with(getApplicationContext())
                            .load(img)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(playingImgView);
                }
            }
            isBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 断开连接时注销监听器
            serviceBinder.unregisterOnStateChangeListener(listenr);
            isBound = false;
        }
    };

    // 实现监听器监听MusicService的变化，
    private MusicService.OnStateChangeListenr listenr = new MusicService.OnStateChangeListenr() {

        @Override
        public void onPlayProgressChange(long played, long duration) {}

        @Override
        public void onPlay(Music item) {
            // 播放状态变为播放时
            btnPlayOrPause.setImageResource(R.drawable.zanting);
            playingTitleView.setText(item.title);
            playingArtistView.setText(item.artist);
            btnPlayOrPause.setEnabled(true);
            if (item.isOnlineMusic){
                Glide.with(getApplicationContext())
                        .load(item.imgUrl)
                        .placeholder(R.drawable.defult_music_img)
                        .error(R.drawable.defult_music_img)
                        .into(playingImgView);
            }
            else {
                ContentResolver resolver = getContentResolver();
                Bitmap img = Utils.getLocalMusicBmp(item.path);
                Glide.with(getApplicationContext())
                        .load(img)
                        .placeholder(R.drawable.defult_music_img)
                        .error(R.drawable.defult_music_img)
                        .into(playingImgView);
            }
        }

        @Override
        public void onPause() {
            // 播放状态变为暂停时
            btnPlayOrPause.setImageResource(R.drawable.bofang);
            btnPlayOrPause.setEnabled(true);
        }
    };

    // 异步获取本地所有音乐
    @SuppressLint("StaticFieldLeak")
    private class MusicUpdateTask extends AsyncTask<Object, Music, Void> {

        // 开始获取, 显示一个进度条
        @Override
        protected void onPreExecute(){
            progressDialog = new ProgressDialog(LocalMusicActivity.this);
            progressDialog.setMessage("获取本地音乐中...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        // 子线程中获取音乐
        @Override
        protected Void doInBackground(Object... params) {

            Uri uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL); //MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            String[] searchKey = new String[]{
                    MediaStore.Audio.Media._ID,     //对应文件在数据库中的检索ID
                    MediaStore.Audio.Media.DATA,   //标题
                    MediaStore.Audio.Media.TITLE,   //标题
                    MediaStore.Audio.Media.ARTIST,  //歌手
                    MediaStore.Audio.Media.ALBUM_ID,   //专辑ID
                    MediaStore.Audio.Media.DURATION,     //播放时长
                    MediaStore.Audio.Media.IS_MUSIC     //是否为音乐文件
            };

            ContentResolver resolver = getContentResolver();
            Cursor cursor = resolver.query(uri, searchKey, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext() && !isCancelled()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    //通过URI和ID，组合出该音乐特有的Uri地址
                    Uri musicUri = Uri.withAppendedPath(uri, id);
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    @SuppressLint("Range") String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    int albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                    int isMusic = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
                    if (isMusic != 0 && duration/(500*60) >= 2) {
                        //再通过专辑Id组合出音乐封面的Uri地址
                        Uri musicPic = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
                        Music data = new Music(musicUri.toString(), title, artist, musicPic.toString(), false, duration, path);
                        //切换到主线程进行更新
                        publishProgress(data);
                    }
                }
                cursor.close();
            }
            return null;
        }

        //主线程
        @Override
        protected void onProgressUpdate(Music... values) {
            Music data = values[0];
            //判断列表中是否已存在当前音乐
            if (!localMusicList.contains(data)){
                //添加到列表和数据库
                localMusicList.add(data);
                LocalMusic music = new LocalMusic(data.songUrl, data.title, data.artist, data.imgUrl, data.isOnlineMusic, data.duration, data.path);
                music.save();
            }
            //刷新UI界面
            MusicAdapter adapter = (MusicAdapter) musicListView.getAdapter();
            adapter.notifyDataSetChanged();
            musicCountView.setText("播放全部(共"+ localMusicList.size()+"首)");
        }

        //任务结束, 关闭进度条
        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

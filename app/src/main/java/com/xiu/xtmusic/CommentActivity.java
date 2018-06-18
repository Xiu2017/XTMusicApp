package com.xiu.xtmusic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xiu.adapter.CommentAdapter;
import com.xiu.api.NeteaseMusic;
import com.xiu.entity.Comment;
import com.xiu.entity.CommentList;
import com.xiu.entity.Msg;
import com.xiu.entity.Music;

import java.util.ArrayList;
import java.util.List;

public class CommentActivity extends AppCompatActivity {

    private String musicId;  //音乐id
    private ProgressBar bar;
    private TextView noComment;
    private CommentAdapter adapter;
    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        //初始化沉浸式状态栏
        initStatusBar();

        bar = (ProgressBar) findViewById(R.id.loadlist);
        noComment = (TextView) findViewById(R.id.noComment);

        Intent intent = getIntent();
        musicId = intent.getStringExtra("musicId");
        //获取评论
        if (musicId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    new NeteaseMusic(CommentActivity.this).comment(musicId);
                }
            }).start();
        } else {
            bar.setVisibility(View.GONE);
            noComment.setVisibility(View.VISIBLE);
        }
    }

    //初始化沉浸式状态栏
    private void initStatusBar() {
        Window win = getWindow();
        //KITKAT也能满足，只是SYSTEM_UI_FLAG_LIGHT_STATUS_BAR（状态栏字体颜色反转）只有在6.0才有效
        win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);//透明状态栏
        // 状态栏字体设置为深色，SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 为SDK23增加
        win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // 部分机型的statusbar会有半透明的黑色背景
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            win.setStatusBarColor(Color.TRANSPARENT);// SDK21
        }
    }

    //广播接收
    BroadcastReceiver cBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra("what", 0)) {
                case Msg.SEARCH_RESULT:
                    CommentList commentList = intent.getParcelableExtra("list");
                    if (commentList == null || commentList.getList() == null || commentList.getList().size() == 0) {
                        noComment.setVisibility(View.VISIBLE);
                    } else {
                        noComment.setVisibility(View.GONE);
                        adapter = new CommentAdapter(commentList.getList(), CommentActivity.this);
                        list = (ListView) findViewById(R.id.commentList);
                        list.setAdapter(adapter);
                    }
                    bar.setVisibility(View.GONE);
                    break;
                case Msg.SEARCH_ERROR:
                    bar.setVisibility(View.GONE);
                    noComment.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        //动态注册一个广播接收者
        IntentFilter filter = new IntentFilter();
        filter.addAction("cBroadcast");
        registerReceiver(cBroadcast, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //注销广播接收者
        unregisterReceiver(cBroadcast);
    }
}

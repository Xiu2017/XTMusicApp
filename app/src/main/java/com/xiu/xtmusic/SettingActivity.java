package com.xiu.xtmusic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sdsmdg.tastytoast.TastyToast;
import com.xiu.customview.CustomVisualizer;
import com.xiu.entity.Msg;
import com.xiu.utils.mApplication;

import java.text.DecimalFormat;

public class SettingActivity extends AppCompatActivity {

    private SeekBar speedSB,bassSB,reverbSB,pitchSB;
    private TextView speedTV,bassTV,reverbTV,pitchTV;
    private CheckBox checkBox;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private DecimalFormat df = new DecimalFormat("#0.00");

    private boolean binding;
    private boolean synced;

    private CustomVisualizer customVisualizer;  //可视化

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        mApplication app = (mApplication) getApplicationContext();
        app.addActivity(this);

        pref = getSharedPreferences("xtmusic", Context.MODE_PRIVATE); //私有数据
        editor = pref.edit();//获取编辑器

        initStatusBar();
        initView();
        hideNotSupOpt();
        initListener();
        initData();
    }

    //隐藏不支持的音效
    public void hideNotSupOpt(){
        TextView tips = (TextView) findViewById(R.id.tips);

        if(!mApplication.supSpeed){
            RelativeLayout speedView = (RelativeLayout) findViewById(R.id.speedView);
            speedView.setAlpha(0.5f);
            speedSB.setEnabled(false);
            //speedView.setVisibility(View.GONE);
            RelativeLayout pitchView = (RelativeLayout) findViewById(R.id.pitchView);
            pitchView.setAlpha(0.5f);
            pitchSB.setEnabled(false);
            //pitchView.setVisibility(View.GONE);
            checkBox.setVisibility(View.GONE);
        }
        if(!mApplication.supBassBoost){
            RelativeLayout bassView = (RelativeLayout) findViewById(R.id.bassView);
            bassView.setAlpha(0.5f);
            bassSB.setEnabled(false);
            //bassView.setVisibility(View.GONE);
        }
        if(!mApplication.supPresetReverb){
            RelativeLayout reverbView = (RelativeLayout) findViewById(R.id.reverbView);
            reverbView.setAlpha(0.5f);
            reverbSB.setEnabled(false);
            //reverbView.setVisibility(View.GONE);
        }

        if(!mApplication.supSpeed && !mApplication.supBassBoost && !mApplication.supPresetReverb){
            tips.setVisibility(View.VISIBLE);
            tips.setText("您的系统不支持音效调节");
        }else if(!mApplication.supSpeed || !mApplication.supBassBoost || !mApplication.supPresetReverb){
            tips.setVisibility(View.VISIBLE);
            tips.setText("检测到音效冲突，部分音效可能无法正常工作");
        }
    }

    //初始化可视化
    public void initVisualizer() {
        mApplication app = (mApplication) getApplicationContext();
        if (app.getMp() != null) {
            customVisualizer = (CustomVisualizer) findViewById(R.id.visualizer);
            //设置自定义颜色
            customVisualizer.setColor(getResources().getColor(R.color.colorPrimary));
            //设置可视化的采样率，10 - 256
            customVisualizer.setDensity(96);
            //绑定MediaPlayer
            customVisualizer.setPlayer(app.getMp().getAudioSessionId());
        }
    }

    //初始化监听事件
    public void initListener(){
        speedSB.setOnSeekBarChangeListener(sbChangeListener);
        bassSB.setOnSeekBarChangeListener(sbChangeListener);
        reverbSB.setOnSeekBarChangeListener(sbChangeListener);
        pitchSB.setOnSeekBarChangeListener(sbChangeListener);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                binding = b;
                editor.putBoolean("binding", b);
                editor.apply();
                editor.commit();
                int speed = pref.getInt("speed", 50);
                if(!binding){
                    speed = 50;
                }
                synced = false;
                pitchSB.setProgress(speed);
                editor.putInt("pitch", speed);
            }
        });
    }

    SeekBar.OnSeekBarChangeListener sbChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            Intent sBroadcast = new Intent();
            sBroadcast.setAction("sBroadcast");
            switch (seekBar.getId()){
                case R.id.speedSB:
                    //确保不会死循环
                    if(synced){
                        synced = false;
                        return;
                    }
                    speedTV.setText(df.format((i+50)/100.0f));
                    editor.putInt("speed", i);
                    sBroadcast.putExtra("what", Msg.SPEED_PITCH);
                    sBroadcast.putExtra("speed", i);
                    if(binding){
                        synced = true;
                        pitchSB.setProgress(i);
                        pitchTV.setText(df.format((i+50)/100.0f));
                        editor.putInt("pitch", i);
                        sBroadcast.putExtra("pitch", i);
                    }
                    break;
                case R.id.pitchSB:
                    //确保不会死循环
                    if(synced){
                        synced = false;
                        return;
                    }
                    pitchTV.setText(df.format((i+50)/100.0f));
                    editor.putInt("pitch", i);
                    sBroadcast.putExtra("what", Msg.SPEED_PITCH);
                    sBroadcast.putExtra("pitch", i);
                    if(binding){
                        synced = true;
                        speedSB.setProgress(i);
                        speedTV.setText(df.format((i+50)/100.0f));
                        editor.putInt("speed", i);
                        sBroadcast.putExtra("speed", i);
                    }
                    break;
                case R.id.bassSB:
                    bassTV.setText(String.valueOf(i));
                    editor.putInt("bass", i);
                    sBroadcast.putExtra("what", Msg.BASS_LEVEL);
                    sBroadcast.putExtra("bass", i);
                    break;
                case R.id.reverbSB:
                    reverbTV.setText(reverbToStr(i));
                    editor.putInt("reverb", i);
                    sBroadcast.putExtra("what", Msg.REVERB_LEVEL);
                    sBroadcast.putExtra("reverb", i);
                    break;
            }
            editor.apply();
            sendBroadcast(sBroadcast);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            editor.commit();
        }
    };

    //初始化持久化数据
    public void initData() {
        //速度和音高
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            speedSB.setProgress(50);
            speedSB.setEnabled(false);
            speedTV.setText("不支持");
            pitchSB.setProgress(50);
            pitchSB.setEnabled(false);
            speedTV.setText("不支持");
        }else {
            int speed = pref.getInt("speed", 50);
            speedSB.setProgress(speed);
            speedTV.setText(df.format((speed+50)/100.0f));
            int pitch = pref.getInt("pitch", 50);
            pitchSB.setProgress(pitch);
            pitchTV.setText(df.format((pitch+50)/100.0f));
            binding = pref.getBoolean("binding", true);
            checkBox.setChecked(binding);
        }

        //低音增益
        int bass = pref.getInt("bass", 0);
        bassSB.setProgress(bass);
        bassTV.setText(String.valueOf(bass));

        //混音
        int reverb = pref.getInt("reverb", 0);
        reverbSB.setProgress(reverb);
        reverbTV.setText(reverbToStr(reverb));
    }

    //混响等级转字符串
    public String reverbToStr(int reverb){
        String strReverb = "关闭";
        switch (reverb){
            case 0:
                strReverb = "关闭";
                break;
            case 1:
                strReverb = "小房间";
                break;
            case 2:
                strReverb = "中房间";
                break;
            case 3:
                strReverb = "大房间";
                break;
            case 4:
                strReverb = "中厅";
                break;
            case 5:
                strReverb = "大厅";
                break;
            case 6:
                strReverb = "板式";
                break;
        }
        return strReverb;
    }

    //重置效果
    public void resetAll(View view){
        editor.putInt("speed", 50);
        editor.putInt("pitch", 50);
        editor.putBoolean("binding", true);
        editor.putInt("bass", 0);
        editor.putInt("reverb", 0);
        editor.apply();
        editor.commit();

        synced = false;
        speedSB.setProgress(50);
        speedTV.setText(String.valueOf(1.00));
        synced = false;
        pitchSB.setProgress(50);
        pitchTV.setText(String.valueOf(1.00));
        checkBox.setChecked(true);
        synced = false;
        bassSB.setProgress(0);
        bassTV.setText(String.valueOf(0));
        synced = false;
        reverbSB.setProgress(0);
        reverbTV.setText(reverbToStr(0));
    }

    //初始化视图
    public void initView(){
        speedSB = (SeekBar) findViewById(R.id.speedSB);
        bassSB = (SeekBar) findViewById(R.id.bassSB);
        reverbSB = (SeekBar) findViewById(R.id.reverbSB);
        pitchSB = (SeekBar) findViewById(R.id.pitchSB);

        speedTV = (TextView) findViewById(R.id.speedTV);
        bassTV = (TextView) findViewById(R.id.bassTV);
        reverbTV = (TextView) findViewById(R.id.reverbTV);
        pitchTV = (TextView) findViewById(R.id.pitchTV);

        checkBox = (CheckBox) findViewById(R.id.checkBox);
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

    @Override
    protected void onResume() {
        super.onResume();
        initVisualizer();  //初始化可视化
    }

    @Override
    protected void onPause() {
        super.onPause();
        //释放资源
        if (customVisualizer != null) {
            customVisualizer.release();
        }
    }
}

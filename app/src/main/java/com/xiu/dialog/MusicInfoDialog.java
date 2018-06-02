package com.xiu.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.xiu.entity.Music;
import com.xiu.utils.TimeFormatUtil;
import com.xiu.xtmusic.R;

import java.text.DecimalFormat;

/**
 * 显示音乐信息的对话框
 */

public class MusicInfoDialog extends Dialog {

    private Music music;

    public MusicInfoDialog(@NonNull Context context, Music music) {
        super(context, R.style.CustomDialog);
        this.music = music;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_music_info);
    }

    @Override
    public void show() {
        super.show();

        //设置宽度全屏，要设置在show的后面
        View view = getWindow().getDecorView();

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        view.setPadding(0, 0, 0, 0);
        getWindow().setAttributes(layoutParams);

        //绑定数据
        TextView musicTitle, musicArtist, musicAlbum, fileName, musicTime, fileSize, fileType, filePath;
        musicTitle = view.findViewById(R.id.musicTitle);
        musicArtist = view.findViewById(R.id.musicArtist);
        musicAlbum = view.findViewById(R.id.musicAlbum);
        fileName = view.findViewById(R.id.fileName);
        musicTime = view.findViewById(R.id.musicTime);
        fileSize = view.findViewById(R.id.fileSize);
        fileType = view.findViewById(R.id.fileType);
        filePath = view.findViewById(R.id.filePath);

        String name = music.getName();

        musicTitle.setText(music.getTitle());
        musicArtist.setText(music.getArtist());
        musicAlbum.setText(music.getAlbum());

        fileName.setText(name.substring(0, name.lastIndexOf(".")));
        musicTime.setText(TimeFormatUtil.secToTime(music.getTime() / 1000));

        DecimalFormat df = new DecimalFormat("#0.00M");
        float temp = music.getSize() / 1024.0f / 1024.0f;

        fileSize.setText(df.format(temp));

        fileType.setText(name.substring(name.lastIndexOf(".") + 1));
        filePath.setText(music.getPath().replace("/" + name, ""));
    }

    //下滑关闭dialog
    private float startY;
    private float moveY = 0;

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        View view = getWindow().getDecorView();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                moveY = ev.getY() - startY;
                view.scrollBy(0, -(int) moveY);
                startY = ev.getY();
                if (view.getScrollY() > 0) {
                    view.scrollTo(0, 0);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (view.getScrollY() < -this.getWindow().getAttributes().height / 4 && moveY > 0) {
                    this.dismiss();
                }
                view.scrollTo(0, 0);
                break;
        }
        return super.onTouchEvent(ev);
    }
}

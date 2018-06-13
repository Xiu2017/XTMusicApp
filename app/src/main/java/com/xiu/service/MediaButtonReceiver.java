package com.xiu.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import com.xiu.entity.Msg;

/**
 * 线控广播
 */

public class MediaButtonReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Intent sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        // 获得KeyEvent对象
        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
            boolean isActionUp = (event.getAction() == KeyEvent.ACTION_UP);
            // 这里会收到两次，我们只判断 up
            if (!isActionUp) {
                return;
            }
            // 获得按键码
            int keycode = event.getKeyCode();
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    //播放下一首
                    sBroadcast.putExtra("what", Msg.PLAY_NEXT);
                    context.sendBroadcast(sBroadcast);
                    Log.d("MediaButton", "next");
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    //播放上一首
                    sBroadcast.putExtra("what", Msg.PLAY_LAST);
                    context.sendBroadcast(sBroadcast);
                    Log.d("MediaButton", "last");
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    //中间按钮,暂停or播放
                    //可以通过发送一个新的广播通知正在播放的视频页面,暂停或者播放视频
                    sBroadcast.putExtra("what", Msg.PLAY_PAUSE);
                    context.sendBroadcast(sBroadcast);
                    Log.d("MediaButton", "playpluse");
                    break;
                default:
                    break;
            }
        }
    }
}

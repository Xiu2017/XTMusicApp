package com.xiu.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.sdsmdg.tastytoast.TastyToast;
import com.xiu.entity.Msg;
import com.xiu.xtmusic.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by xiu on 2018/1/9.
 */
public class AudioUtil {

    private Context context;

    public AudioUtil(Context context) {
        this.context = context;
    }

    /**
     * 设置铃声
     *
     * @param type  RingtoneManager.TYPE_RINGTONE 来电铃声
     *              RingtoneManager.TYPE_NOTIFICATION 通知铃声
     *              RingtoneManager.TYPE_ALARM 闹钟铃声
     * @param path  下载下来的mp3全路径
     * @param title 铃声的名字
     */
    public void setRing(int type, String path, String title) {

        //权限检测
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            grantWriteSetings();
            return;
        }

        Uri oldRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE); //系统当前  通知铃声
        //Uri oldNotification = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION); //系统当前  通知铃声
        //Uri oldAlarm = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM); //系统当前  闹钟铃声

        File sdfile = new File(path);
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, sdfile.getAbsolutePath());
        values.put(MediaStore.MediaColumns.TITLE, title);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
        values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
        values.put(MediaStore.Audio.Media.IS_ALARM, false);
        values.put(MediaStore.Audio.Media.IS_MUSIC, false);

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(sdfile.getAbsolutePath());
        Uri newUri = null;
        String deleteId = "";
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, MediaStore.MediaColumns.DATA + "=?", new String[]{path}, null);
            if (cursor.moveToFirst()) {
                deleteId = cursor.getString(cursor.getColumnIndex("_id"));
            }

            context.getContentResolver().delete(uri,
                    MediaStore.MediaColumns.DATA + "=\"" + sdfile.getAbsolutePath() + "\"", null);
            newUri = context.getContentResolver().insert(uri, values);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (newUri != null) {

            String ringStoneId = "";
            //String notificationId = "";
            //String alarmId = "";
            if (null != oldRingtoneUri) {
                ringStoneId = oldRingtoneUri.getLastPathSegment();
            }

/*            if (null != oldNotification) {
                notificationId = oldNotification.getLastPathSegment();
            }

            if (null != oldAlarm) {
                alarmId = oldAlarm.getLastPathSegment();
            }*/

            Uri setRingStoneUri;
            //Uri setNotificationUri;
            //Uri setAlarmUri;

            if (type == RingtoneManager.TYPE_RINGTONE || ringStoneId.equals(deleteId)) {
                setRingStoneUri = newUri;

            } else {
                setRingStoneUri = oldRingtoneUri;
            }

/*            if (type == RingtoneManager.TYPE_NOTIFICATION || notificationId.equals(deleteId)) {
                setNotificationUri = newUri;

            } else {
                setNotificationUri = oldNotification;
            }

            if (type == RingtoneManager.TYPE_ALARM || alarmId.equals(deleteId)) {
                setAlarmUri = newUri;

            } else {
                setAlarmUri = oldAlarm;
            }*/

            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, setRingStoneUri);
            //RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION, setNotificationUri);
            //RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM, setAlarmUri);

            switch (type) {
                case RingtoneManager.TYPE_RINGTONE:
                    TastyToast.makeText(context, "设置来电铃声成功", Msg.LENGTH_SHORT, TastyToast.SUCCESS).show();
                    break;
/*                case RingtoneManager.TYPE_NOTIFICATION:
                    Toast.makeText(context.getApplicationContext(), "设置通知铃声成功！", Toast.LENGTH_SHORT).show();
                    break;
                case RingtoneManager.TYPE_ALARM:
                    Toast.makeText(context.getApplicationContext(), "设置闹钟铃声成功！", Toast.LENGTH_SHORT).show();
                    break;*/
            }
        }
    }

    //提示用户授予权限
    private void grantWriteSetings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        TastyToast.makeText(context, "请授予权限再返回进行操作", Msg.LENGTH_SHORT, TastyToast.WARNING).show();
    }
}

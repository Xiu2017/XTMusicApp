package com.xiu.utils;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;

import java.lang.reflect.Method;

import static android.content.Context.STORAGE_SERVICE;

/**
 * Created by xiu on 2017/12/17.
 */

public class StorageUtil {

    private Context context;

    public StorageUtil(Context context) {
        this.context = context;
    }

    // 获取主存储卡路径
    public String getPrimaryStoragePath() {
        try {
            StorageManager sm = (StorageManager) context.getSystemService(STORAGE_SERVICE);
            Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths", new Class[0]);
            String[] paths = (String[]) getVolumePathsMethod.invoke(sm, new Object[]{});
            // first element in paths[] is primary storage path
            return paths[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 获取次存储卡路径,一般就是外置 TF 卡了. 不过也有可能是 USB OTG 设备...
    // 其实只要判断第二章卡在挂载状态,就可以用了.
    public String getSecondaryStoragePath() {
        try {
            StorageManager sm = (StorageManager) context.getSystemService(STORAGE_SERVICE);
            Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths", new Class[0]);
            String[] paths = (String[]) getVolumePathsMethod.invoke(sm, new Object[]{});
            // second element in paths[] is secondary storage path
            return paths.length <= 1 ? null : paths[1];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 获取存储卡的挂载状态. path 参数传入上两个方法得到的路径
    public String getStorageState(String path) {
        try {
            StorageManager sm = (StorageManager) context.getSystemService(STORAGE_SERVICE);
            Method getVolumeStateMethod = StorageManager.class.getMethod("getVolumeState", new Class[]{String.class});
            String state = (String) getVolumeStateMethod.invoke(sm, path);
            return state;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    //获取已挂载内置SD卡路径
    public String innerSDPath() {
        StorageUtil storageUtil = new StorageUtil(context);
        String innerSD = storageUtil.getPrimaryStoragePath();
        if (innerSD != null && storageUtil.getStorageState(innerSD).equals(Environment.MEDIA_MOUNTED)) {
            return innerSD;
        }
        return null;
    }

    //获取已挂载外置SD卡路径
    public String extSDPath() {
        StorageUtil storageUtil = new StorageUtil(context);
        String extSD = storageUtil.getSecondaryStoragePath();
        if (extSD != null && storageUtil.getStorageState(extSD).equals(Environment.MEDIA_MOUNTED)) {
            return extSD;
        }
        return null;
    }
}

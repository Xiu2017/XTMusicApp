package com.xiu.entity;

/**
 * 标记各种广播ID，便于识别
 */

public class Msg {
    public static final int PLAY_COMPLETION = 1002;  //播放完成
    public static final int PLAY_LAST = 1012;  //上一首
    public static final int PLAY_PAUSE = 1003;  //暂停&恢复
    public static final int PLAY_NEXT = 1004;  //下一首
    public static final int PLAY = 1005;  //播放指定歌曲

    public static final int CURRENTTIME = 1006;  //歌曲进度

    public static final int CALL_IDLETOOFFHOOK = 1008;  //接听
    public static final int CALL_RINGING = 1010;  //响铃

    public static final int NOTIFICATION_REFRESH = 1015;  //刷新通知栏

    public static final int TIMER_EXIT = 1016;  //定时退出
    public static final int TIMER_CLEAR = 1018;  //清除定时器

    public static final int WITER_EXTSD = 1019;  //外置SD卡写入权限

    public static final int BUFFERING_UPDATE = 1020;  //缓冲进度
    public static final int SEARCH_RESULT = 1021;  //搜索结果
    public static final int GET_MUSIC_PATH = 1022;  //获取到路径
    public static final int PLAY_KUGOU_MUSIC = 1023;  //无条件插队播放音乐
    public static final int GET_MUSIC_ERROR = 1024;  //获取歌曲失败
    public static final int SEARCH_ERROR = 1025;  //获取列表失败

    public static final int LENGTH_SHORT = 4;  //Toast短提示的时长
    public static final int LENGTH_LONG = 5;  //Toast长提示的时长

    public static final int CLOSE = 1026;  //关闭应用
    public static final int SWITCH_MODE = 1027;  //切换播放模式
    public static final int PLAY_SPEED = 1028;  //播放速度
    public static final int PLAY_PITCH = 1029;  //音调
}

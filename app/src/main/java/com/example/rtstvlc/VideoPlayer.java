package com.example.rtstvlc;

/**
 * Created by visking on 2017/11/2.
 */

public class VideoPlayer {

    public static native int play(String videoPath, Object surface);
    public static native void stop();

    static{
        System.loadLibrary("avcodec-57");
        System.loadLibrary("avfilter-6");
        System.loadLibrary("avformat-57");
        System.loadLibrary("avutil-55");
        System.loadLibrary("swresample-2");
        System.loadLibrary("postproc-54");
        System.loadLibrary("swscale-4");
        System.loadLibrary("avdevice-57");
        System.loadLibrary("videoplayer");
    }
}

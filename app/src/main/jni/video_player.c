//
// Created by visking on 2017/10/31.
//

#include <stdio.h>
#include <time.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include "com_example_rtstvlc_VideoPlayer.h"
#include "include/libavformat/avformat.h"
#include "include/libavcodec/avcodec.h"
#include "include/libswscale/swscale.h"
#include "include/libavutil/log.h"
#include "include/libavutil/imgutils.h"

#include <jni.h>
#include <android/log.h>

#define SYS_LOG_TAG "ffmpeg"
#define LOG_FILE "/storage/emulated/0/indoor_log.txt"

/*
#define FFMPEG_VDEC "VDEC"
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, FFMPEG_VDEC, format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  FFMPEG_VDEC, format, ##__VA_ARGS__)
*/

#define LOGD(format, ...) av_log(NULL, AV_LOG_DEBUG, format, ##__VA_ARGS__);
#define LOGV(format, ...) av_log(NULL, AV_LOG_VERBOSE, format, ##__VA_ARGS__);
#define LOGI(format, ...) av_log(NULL, AV_LOG_INFO, format, ##__VA_ARGS__);
#define LOGW(format, ...) av_log(NULL, AV_LOG_WARNING, format, ##__VA_ARGS__);
#define LOGE(format, ...) av_log(NULL, AV_LOG_ERROR, format, ##__VA_ARGS__);

#define ALOG(level, TAG, ...)    ((void)__android_log_vprint(level, TAG, __VA_ARGS__))

#define RTSP_TIMEOUT 2000000

void GetCurLocalTime(char *buf)
{
    time_t timer;
    struct tm *t;

    timer = time(NULL);
    t = localtime(&timer);
    strcpy(buf, asctime(t));
}

static void syslog_print(void *ptr, int level, const char *fmt, va_list vl)
{
    switch(level) {
    case AV_LOG_DEBUG:
        ALOG(ANDROID_LOG_VERBOSE, SYS_LOG_TAG, fmt, vl);
        break;
    case AV_LOG_VERBOSE:
        ALOG(ANDROID_LOG_DEBUG, SYS_LOG_TAG, fmt, vl);
        break;
    case AV_LOG_INFO:
        ALOG(ANDROID_LOG_INFO, SYS_LOG_TAG, fmt, vl);
        break;
    case AV_LOG_WARNING:
        ALOG(ANDROID_LOG_WARN, SYS_LOG_TAG, fmt, vl);
        break;
    case AV_LOG_ERROR:
        ALOG(ANDROID_LOG_ERROR, SYS_LOG_TAG, fmt, vl);
        break;
    }
    //save_log(fmt);

    //if (level > AV_LOG_WARNING)
    //    return;
#if 0
    char log_buf[256] = {0};
    char curtime[64] = {0};
    char saved_log[512] = {0};
    FILE *logfp = NULL;
    //va_list args;

    logfp = fopen(LOG_FILE, "a+");
    if (logfp == NULL) {
        return;
    }

    GetCurLocalTime(curtime);
    //va_start(args, log_buf);
    vsnprintf(log_buf, 256, fmt, vl);
    //va_end(args);
    strncpy(saved_log, curtime, strlen(curtime) - 1);
    strcat(saved_log, ">>>  ");
    strcat(saved_log, log_buf);
    fwrite(saved_log, 1, strlen(saved_log), logfp);

    fclose(logfp);
#endif
}

static int fill_pic(ANativeWindow *win, ANativeWindow_Buffer *buf, AVFrame *rgba_frame, int height)
{
    if (ANativeWindow_lock(win, buf, 0) != 0) {
        ANativeWindow_release(win);
        return -1;
    }

    // 获取stride
    uint8_t * dst = buf->bits;
    int dstStride = buf->stride * 4;
    uint8_t * src = (uint8_t*) (rgba_frame->data[0]);
    int srcStride = rgba_frame->linesize[0];

    // 由于window的stride和帧的stride不同,因此需要逐行复制
    int h;
    for (h = 0; h < height; h++) {
        memcpy(dst + h * dstStride, src + h * srcStride, srcStride);
    }
    if (ANativeWindow_unlockAndPost(win) != 0) {
        ANativeWindow_release(win);
        return -1;
    }

    return 0;
}

static volatile int playingFlag = 0;
static struct timeval startTime;
static struct timeval endTime;

static int CheckPlaying(void)
{
    return playingFlag;
}

static int PlayInterruptCallBack(void *ctx)
{
    long usedTime = 0;

    gettimeofday(&endTime, NULL);
    usedTime = (endTime.tv_sec - startTime.tv_sec) * 1000000 + (endTime.tv_usec - startTime.tv_usec);
    //LOGE("usedTime:%ld", usedTime);

    if (usedTime > RTSP_TIMEOUT)
        return 1;

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_example_rtstvlc_VideoPlayer_play(JNIEnv *env, jclass clazz, jstring path, jobject surface)
{
AVFormatContext *pFormatCtx = NULL;
    AVCodecContext *pCodecCtx = NULL;
    AVCodec *pCodec = NULL;
    AVFrame *pvFrame = NULL;
    AVFrame *pFrameRGBA = NULL;
    struct SwsContext *sws_ctx = NULL;
    AVPacket packet;

    ANativeWindow *nativeWindow = NULL;
    ANativeWindow_Buffer windowBuffer;
    int videoWidth;
    int videoHeight;
    char videoPath[500] = {0};
    int ret = 0;
    int i = 0;
    int videoindex = 0;
    int resultReadFrame = 0;
    int got_picture = 0;

    sprintf(videoPath,"%s",(*env)->GetStringUTFChars(env,path, NULL));

    av_register_all();
    avformat_network_init();

    //av_log_set_callback(syslog_print);
    //av_log_set_level(AV_LOG_WARNING);

    playingFlag = 1;

    ret = avformat_open_input(&pFormatCtx, videoPath, NULL, NULL);
    if(ret != 0) {
        LOGE("Couldn't open input stream:%s. ret = %d(%s)\n", videoPath, ret, av_err2str(ret));
        return -1;
    }

    LOGE("Open stream:%s successfully !!!\n", videoPath);
    if(avformat_find_stream_info(pFormatCtx,NULL)<0) {
        LOGE("Couldn't find stream information.\n");
        return -1;
    }
    videoindex = -1;
    for(i = 0; i < pFormatCtx->nb_streams; i++) {
        if(pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO){
            videoindex=i;
            break;
        }
    }
    if(videoindex == -1) {
        LOGE("Couldn't find a video stream.\n");
        return -1;
    }

    pCodecCtx = pFormatCtx->streams[videoindex]->codec;
    pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
    if(pCodec == NULL) {
        LOGE("Codec not found.\n");
        return -1; // Codec not found
    }

    if(avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGE("Could not open codec.\n");
        return -1; // Could not open codec
    }
    // 获取native window
    nativeWindow = ANativeWindow_fromSurface(env, surface);

    // 获取视频宽高
    videoWidth = pCodecCtx->width;
    videoHeight = pCodecCtx->height;

    LOGE("video width:%d height:%d", videoWidth, videoHeight);

    // 设置native window的buffer大小,可自动拉伸
    ANativeWindow_setBuffersGeometry(nativeWindow, videoWidth, videoHeight, WINDOW_FORMAT_RGBA_8888);

    // Allocate video frame
    pvFrame = av_frame_alloc();

    // 用于渲染
    pFrameRGBA = av_frame_alloc();
    if (pvFrame == NULL || pFrameRGBA == NULL) {
         LOGE("Could not allocate video frame.\n");
         return -1;
    }

    // Determine required buffer size and allocate buffer
    // buffer中数据就是用于渲染的,且格式为RGBA
    int numBytes=av_image_get_buffer_size(AV_PIX_FMT_RGBA, pCodecCtx->width, pCodecCtx->height, 1);
    uint8_t * buffer=(uint8_t *)av_malloc(numBytes*sizeof(uint8_t));
    av_image_fill_arrays(pFrameRGBA->data, pFrameRGBA->linesize, buffer, AV_PIX_FMT_RGBA,
                         pCodecCtx->width, pCodecCtx->height, 1);

    sws_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt,
                             pCodecCtx->width, pCodecCtx->height, AV_PIX_FMT_RGBA, SWS_BILINEAR,
                             NULL, NULL, NULL);

    pFormatCtx->interrupt_callback.callback = PlayInterruptCallBack;
    pFormatCtx->interrupt_callback.opaque = NULL;
    for (;;) {
        if (!CheckPlaying()) {
            break;
        }
        gettimeofday(&startTime, NULL);
        if ((ret = av_read_frame(pFormatCtx, &packet)) >= 0) {
            if (packet.losspkt_flag) {
                LOGE("av_read_frame: loss pkt\n");
                 av_packet_unref(&packet);
                 continue;
            }
            if(packet.stream_index == videoindex) {
                ret = avcodec_decode_video2(pCodecCtx, pvFrame, &got_picture, &packet);
                if (ret < 0) {
                    av_packet_unref(&packet);
                    LOGE("avcodec_decode_video2 error! ret=%d(%s)\n", ret, av_err2str(ret));
                    break;

                }
                if (got_picture) {
                //if (packet.losspkt_flag == 0 || (pvFrame->key_frame && pvFrame->pict_type == AV_PICTURE_TYPE_I)) {
                    if (pvFrame->key_frame && pvFrame->pict_type == AV_PICTURE_TYPE_I) {
                        sws_scale(sws_ctx, (uint8_t const * const *)pvFrame->data,
                                  pvFrame->linesize, 0, pCodecCtx->height,
                                  pFrameRGBA->data, pFrameRGBA->linesize);
                        if (fill_pic(nativeWindow, &windowBuffer, pFrameRGBA, videoHeight) != 0) {
                            av_frame_unref(pvFrame);
                            av_packet_unref(&packet);
                            break;
                        }
                    }
                }
                av_frame_unref(pvFrame);
            }
            av_packet_unref(&packet);
        } else {
            break;
        }
    }


//EXIT_DEC:
    //ANativeWindow_release(nativeWindow);

    av_free(buffer);
    av_free(pFrameRGBA);

    // Free the YUV frame
    av_free(pvFrame);

    // Close the codecs
    avcodec_close(pCodecCtx);

    // Close the video file
    avformat_close_input(&pFormatCtx);

    if (ret < 0 && ret != AVERROR_EOF) {
        LOGE("Decode failed with error ret:%d(%s)\n", ret, av_err2str(ret));
    }

    playingFlag = 0;

    return 0;
}

JNIEXPORT void JNICALL Java_com_example_rtstvlc_VideoPlayer_stop(JNIEnv *env, jclass clazz)
{
    playingFlag = 0;
}

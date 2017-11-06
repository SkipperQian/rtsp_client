package com.example.thread;

import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * 呼叫监听线程
 * Created by smartBrin on 2017/5/17.
 */
public class CallListenerThread implements Runnable {

    public final static String CALL_IN = "CALLIN";//呼叫
    public final static String HUNG_UP = "HUNGUP";//挂断
    private final static int PORT = 5600;//端口
    private final static String BASE_IP = "192.168.42.125";//底座ip

    private String mResult;//接收的结果
    private Context mContext;
    // private MainAcitvityView mMainActivityView;

    public CallListenerThread(Context context) {
        mContext = context;
    }


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }


    @Override
    public void run() {

        while (true) {
            try {
                Thread.sleep(500);
                //1.创建一个DatagramSocket对象，并指定监听的端口号
                DatagramSocket socket = new DatagramSocket(PORT);
                //2. 创建一个byte数组用于接收
                byte buff[] = new byte[1024];
                //3. 创建一个空的DatagramPackage对象
                DatagramPacket packet = new DatagramPacket(buff, buff.length);

                //4.使用receive方法接收发送方所发送的数据,同时这也是一个阻塞的方法
                socket.receive(packet);
                mResult = new String(packet.getData(), packet.getOffset(), packet.getLength());
                if (CALL_IN.equals(mResult)) {//有呼叫进来
                    //播放视频
                    //  mMainActivityView.playVideo();
                    broadcastUpdate(CALL_IN);
                } else if (HUNG_UP.equals(mResult)) {//通话结束
                    //关闭视频
                    //  mMainActivityView.closeVideo();
                    broadcastUpdate(HUNG_UP);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

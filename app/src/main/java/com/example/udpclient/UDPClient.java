package com.example.udpclient;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by smartLew on 2017/6/13.
 */
public class UDPClient implements Runnable {

    final static int udpPort = 5600;
    final static String hostIp = "192.168.1.4";
    private static DatagramSocket socket = null;
    private static DatagramPacket packetSend, packetRcv;
    private boolean udpLife = true; //udp生命线程
    private byte[] msgRcv = new byte[1024]; //接收消息

    private Context mContext;


    public UDPClient() {
        super();
    }

    public UDPClient(Context context) {
        mContext = context;
    }

    //返回udp生命因子是否存活
    public boolean isUdpLife() {
        if (udpLife) {
            return true;
        }
        return false;
    }

    //设置udp生命因子
    public void setUdpLife(boolean b) {
        udpLife = b;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(udpPort);
            socket.setSoTimeout(5000);//设置超时未5s
        } catch (SocketException e) {
            e.printStackTrace();
        }
        packetRcv = new DatagramPacket(msgRcv, msgRcv.length);
        while (udpLife) {
            try {
                socket.receive(packetRcv);
                String RcvMsg = new String(packetRcv.getData(), packetRcv.getOffset(), packetRcv.getLength());
                if (!TextUtils.isEmpty(RcvMsg)) {
                    updateBroadcast(RcvMsg);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket.close();
    }

    /**
     * 发送广播
     *
     * @param action
     */
    public void updateBroadcast(String action) {
        final Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);

    }
}

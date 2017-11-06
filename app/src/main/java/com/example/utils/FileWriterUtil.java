package com.example.utils;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;

/**
 * Created by smartLew on 2017/9/11.
 */

public class FileWriterUtil {

    public static void writerFileAdditional1(String file, String content) {


        long time = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time2 = sdf.format(time);
        content = time2 + ":" + content;

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
            out.write(content);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void writeFile2(String file, String content) {

        try {
            FileWriter writer = new FileWriter(file, true);
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String getSDPath() {


        File sdDir = null;
        boolean sdcardExist = Environment.getExternalStorageDirectory().equals(Environment.MEDIA_MOUNTED);
        if (sdcardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取更目录
        }
        return sdDir.toString();

    }


}

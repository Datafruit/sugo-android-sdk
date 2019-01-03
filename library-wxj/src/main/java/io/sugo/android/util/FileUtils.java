package io.sugo.android.util;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * create by chenyuyi on 2019/1/3
 */
public class FileUtils {
    private static Map<String,String> cacheMap = new HashMap<String, String>();


    /**
     * 读取assets中的资源文件
     * @param context
     * @param name  assets下的相对路径
     * @param isCache 是否需要使用缓存
     * @return
     */
    public static String getAssetsFileContent(Context context, String name, boolean isCache){
        if (isCache){
            String fileStr = cacheMap.get(name);
            if (fileStr != null&&fileStr.length()>0)
                return fileStr;
        }
        String fileStr=null;
        InputStream inputStream = null;
        try {
            inputStream = context.getClass().getClassLoader().getResourceAsStream("assets/"+name);
            int size = inputStream.available();
            int len = -1;
            byte[] bytes = new byte[size];
            inputStream.read(bytes);
            inputStream.close();
            fileStr = new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (isCache){
            cacheMap.put(name,fileStr);
        }
        return fileStr;
    }
}

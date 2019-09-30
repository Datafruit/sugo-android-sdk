package io.sugo.android.xwalk;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Base64;
import android.util.JsonWriter;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.xwalk.core.XWalkView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.sugo.android.metrics.SugoAPI;
import io.sugo.android.metrics.SugoWebNodeReporter;
import io.sugo.android.viewcrawler.XWalkViewListener;

/**
 * Created by Administrator on 2017/2/8.
 */

public class SugoXWalkViewSupport {

    private static boolean sIsSnapshotXWalkViewEnable = false;

    public static void enableXWalkView(SugoAPI sugoAPI, boolean enable) {
        sIsSnapshotXWalkViewEnable = enable;
        if (sIsSnapshotXWalkViewEnable) {
            sugoAPI.setSnapshotViewListener(new XWalkViewListener() {
                @Override
                public void snapshotSpecialView(JsonWriter j, View view) {
                    try {
                        if (checkXWalkView() && view instanceof XWalkView) {    // 检查是否有 XWalkView 这个类，否则接下来的调用会崩溃
                            SugoWebNodeReporter sugoWebNodeReporter = SugoAPI.getSugoWebNodeReporter(view);
                            if (sugoWebNodeReporter != null) {
                                final XWalkView xWalkView = (XWalkView) view;
                                int oldVersion = sugoWebNodeReporter.version;
                                xWalkView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        xWalkView.load("javascript:if(typeof sugo === 'object' && typeof sugo.reportNodes === 'function'){sugo.reportNodes();}", "");
                                    }
                                });
                                int max_attempt = 40;
                                int count = 0;
                                while (oldVersion == sugoWebNodeReporter.version && count < max_attempt) {
                                    try {
                                        count++;
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        Log.e("snapshotSpecialView", e.getMessage());
                                    }
                                }
                                if (count < max_attempt) {

                                    j.name("htmlPage");

                                    j.beginObject();
                                    j.name("url").value(sugoWebNodeReporter.url);
                                    j.name("clientWidth").value(sugoWebNodeReporter.clientWidth);
                                    j.name("clientHeight").value(sugoWebNodeReporter.clientHeight);
                                    j.name("nodes").value(sugoWebNodeReporter.webNodeJson);
                                    j.name("screenshot").value(bitmapToBase64(captureImage(xWalkView)));
                                    j.endObject();

                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void recyclerXWalkView(Activity activity) {
                    SugoXWalkViewEventListener.cleanUnuseXwalkView(activity);
                }

                @Override
                public void bindEvents(String token, JSONArray eventBindings) {
                    SugoXWalkViewEventListener.bindEvents(token, eventBindings);
                }
            });
        } else {
            sugoAPI.setSnapshotViewListener((XWalkViewListener) null);
        }
    }

    public static void handleXWalkView(SugoAPI sugoAPI, XWalkView xWalkView) {
        if (!isSnapshotXWalkViewEnable()) {
            Log.e("SugoXWalkViewSupport", "如果想要支持 XWalkView ，请调用 SugoXWalkViewSupport.enableXWalkView");
            return;
        }
        xWalkView.addJavascriptInterface(new SugoXWalkViewEventListener(sugoAPI), "sugoEventListener");
        SugoXWalkViewNodeReporter reporter = new SugoXWalkViewNodeReporter();
        xWalkView.addJavascriptInterface(reporter, "sugoWebNodeReporter");
        SugoAPI.setSugoWebNodeReporter(xWalkView, reporter);
    }


    public static boolean isSnapshotXWalkViewEnable() {
        return sIsSnapshotXWalkViewEnable;
    }

    private static boolean checkXWalkView() {
        try {
            Class xwalkViewClass = Class.forName("org.xwalk.core.XWalkView");
            return true;
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    private static String bitmapToBase64(Bitmap bitmap) {

        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private static Bitmap captureImage(XWalkView xWalkView) {

        if (xWalkView != null) {
            Bitmap bitmap = null;

            boolean isCrosswalk = false;
            try {
                Class.forName("org.xwalk.core.XWalkView");
                isCrosswalk = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (isCrosswalk) {
                try {
                    TextureView textureView = findXWalkTextureView(xWalkView);
                    bitmap = textureView.getBitmap();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                bitmap = Bitmap.createBitmap(xWalkView.getWidth(), xWalkView.getHeight(), Bitmap.Config.RGB_565);
                Canvas c = new Canvas(bitmap);
                xWalkView.draw(c);
            }
            return bitmap;
        } else {
            return null;
        }
    }

    private static TextureView findXWalkTextureView(ViewGroup group) {
        int childCount = group.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextureView) {
                String parentClassName = child.getParent().getClass().toString();
                boolean isRightKindOfParent = (parentClassName.contains("XWalk"));
                if (isRightKindOfParent) {
                    return (TextureView) child;
                }
            } else if (child instanceof ViewGroup) {
                TextureView textureView = findXWalkTextureView((ViewGroup) child);
                if (textureView != null) {
                    return textureView;
                }
            }
        }
        return null;
    }

}

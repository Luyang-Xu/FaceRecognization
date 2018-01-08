package com.luyang.myapplication.util;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by luyang on 2018/1/4.
 */

public class DetectUtil {

    public interface Callback {
        public void success(JSONObject jsonObject);

        public void failed(FaceppParseException exception);
    }

    public static void detect(final Bitmap bitmap, final Callback callback) {
        if (callback != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    HttpRequests requests = new HttpRequests(Constants.API_KEY, Constants.API_SECRET, true, true);
                    //截取图像
                    Bitmap bp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    //字节输出流，将bitmap转换为二进制数组传出
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] arr = stream.toByteArray();
                    PostParameters parameters = new PostParameters();
                    parameters.setImg(arr);
                    try {
                        JSONObject jsonObject = requests.detectionDetect(parameters);
                        if (callback != null) {
                            callback.success(jsonObject);
                        }
                    } catch (FaceppParseException e) {
                        if (callback != null) {
                            e.printStackTrace();
                            callback.failed(e);
                        }
                    }


                }
            }).start();
        }
    }
}

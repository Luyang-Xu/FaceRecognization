package com.luyang.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.error.FaceppParseException;
import com.luyang.myapplication.util.Constants;
import com.luyang.myapplication.util.DetectUtil;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_CODE = 1;
    private ImageView mPhoto;
    private Button mDetect;
    private Button mGetImage;
    private TextView mTip;
    private View mWatting;
    private TextView ageAndGender;

    private String imagePath;
    private Bitmap bitmapImage;
    private Paint mpaint;

    private final static int MSG_SUCCESS = 1;
    private final static int MSG_FAILED = 2;


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SUCCESS:
                    mWatting.setVisibility(View.GONE);
                    JSONObject jsonObject = (JSONObject) msg.obj;
                    prepareRsBitmap(jsonObject);
                    mPhoto.setImageBitmap(bitmapImage);
                    break;
                case MSG_FAILED:
                    mWatting.setVisibility(View.GONE);
                    String errMsg = (String) msg.obj;
                    if (TextUtils.isEmpty(errMsg)) {
                        mTip.setText("error");
                    } else {
                        mTip.setText(errMsg);
                    }

                    break;
                default:
                    break;
            }
        }
    };

    private void prepareRsBitmap(JSONObject jsonObject) {
        //在原图上画矩形
        Bitmap localBitmap = Bitmap.createBitmap(bitmapImage.getWidth(), bitmapImage.getHeight(), bitmapImage.getConfig());
        Canvas canvas = new Canvas(localBitmap);
        canvas.drawBitmap(bitmapImage, 0, 0, null);

        try {
            JSONArray faceArray = jsonObject.getJSONArray("faces");
            for (int i = 0; i < faceArray.length(); i++) {
                JSONObject obj = faceArray.getJSONObject(i);
                JSONObject rect = obj.getJSONObject("face_rectangle");
                float x = (float) rect.getInt("left");
                float y = (float) rect.getInt("top");
                float height = (float) rect.getInt("height");
                float width = (float) rect.getInt("width");

                mpaint.setColor(0xffffffff);
                mpaint.setStrokeWidth(3);
                //画矩形
                canvas.drawLine(x, y, x + width, y, mpaint);
                canvas.drawLine(x, y, x, y + height, mpaint);
                canvas.drawLine(x + width, y, x + width, y + height, mpaint);
                canvas.drawLine(x, y + height, x + width, y + height, mpaint);

                //得到年龄和性别
                JSONObject attribute = obj.getJSONObject("attributes");
                int age = attribute.getJSONObject("age").getInt("value");
                String gender = attribute.getJSONObject("gender").getString("value");

                //将textview转换成bitmap显示
                Bitmap ageBitmap = createAgeBitmap(age, "Male".equals(gender));

                int ageWidth = ageBitmap.getWidth();
                int ageHeight = ageBitmap.getHeight();

                if (localBitmap.getWidth() < mPhoto.getWidth() && localBitmap.getHeight() < mPhoto.getHeight()) {
                    float ratio = Math.max(localBitmap.getWidth() * 1.0f / mPhoto.getWidth(), localBitmap.getHeight() * 1.0f / mPhoto.getHeight());
                    ageBitmap = bitmapImage.createScaledBitmap(ageBitmap, (int) (ageWidth * ratio), (int) (ageHeight * ratio), false);

                }

                canvas.drawBitmap(ageBitmap, x + ageBitmap.getWidth() / 2, y, null);


                bitmapImage = localBitmap;

            }


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * 画年龄性别框
     * View组件显示的内容可以通过cache机制保存为bitmap
     *
     * @param age
     * @param isMale
     * @return
     */
    private Bitmap createAgeBitmap(int age, boolean isMale) {
        ageAndGender = mWatting.findViewById(R.id.id_age_and_gender);
        ageAndGender.setText(age + "");
        if (isMale) {
            ageAndGender.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male), null, null, null);
        } else {
            ageAndGender.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female), null, null, null);
        }
        ageAndGender.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(ageAndGender.getDrawingCache());
        ageAndGender.destroyDrawingCache();
        return bitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initEvents();
    }

    private void initViews() {
        mPhoto = findViewById(R.id.id_image);
        mDetect = findViewById(R.id.id_detect);
        mGetImage = findViewById(R.id.id_selectPhoto);
        mTip = findViewById(R.id.id_tip);
        mWatting = findViewById(R.id.id_waiting);
        mpaint = new Paint();
        requestPermission(Constants.PERMISSIONS);
    }

    private void initEvents() {
        mGetImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //选取图片，联系人，视频，音频，Intent.ACTION_GET_CONTENT
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_CODE);
            }
        });


        mDetect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mWatting.setVisibility(View.VISIBLE);
                if (imagePath != null && imagePath.trim().equals("")) {
                    resizephoto();
                } else {
                    bitmapImage = BitmapFactory.decodeResource(getResources(), R.drawable.t4);
                }
                DetectUtil.detect(bitmapImage, new DetectUtil.Callback() {
                    @Override
                    public void success(JSONObject jsonObject) {
                        Log.e("AAA", "SUCCESS");
                        //不要用new的方式来创建message，要用obtain方法从对象池中去取
                        Message message = Message.obtain();
                        message.what = MSG_SUCCESS;
                        message.obj = jsonObject;
                        handler.sendMessage(message);
//                        或者使用该种方式来发送消息
//                        Message m =handler.obtainMessage();
//                        message.what = MSG_SUCCESS;
//                        message.obj = jsonObject;
//                        m.sendToTarget();

                    }

                    @Override
                    public void failed(FaceppParseException exception) {
                        Log.e("AAA", "FAILED");
                        Message message = Message.obtain();
                        message.what = MSG_FAILED;
                        message.obj = exception.getErrorMessage();
                        Log.e("REASON_EMPTY", String.valueOf(message.obj));
                        handler.sendMessage(message);

                    }

                });

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_CODE:
                Uri uri = data.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();
                int id = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                imagePath = cursor.getString(id);
                cursor.close();

                //每张图片的大小不能超过3兆，必须压缩图片
                resizephoto();
                mPhoto.setImageBitmap(bitmapImage);
                mTip.setText("Click Detect ===>");

                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //压缩图片大小
    private void resizephoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //只获得尺寸，不加载图片
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        double ratio = Math.max(options.outWidth * 1.0f / 1024f, options.outHeight * 1.0f / 1024f);
        //压缩尺寸
        options.inSampleSize = (int) Math.ceil(ratio);
        options.inJustDecodeBounds = false;
        bitmapImage = BitmapFactory.decodeFile(imagePath, options);
    }

    //检查权限：
    public boolean isPermitted(String... permissions) {
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void requestPermission(String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isPermitted(permissions)) {
                ActivityCompat.requestPermissions(MainActivity.this, permissions, Constants.PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.PERMISSION_CODE:
                for (int i : grantResults) {
                    if (i != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(MainActivity.this, "您没有授予必要的权限", Toast.LENGTH_LONG).show();
                        //finish();
                    }
                }

                break;
            default:
                break;

        }
    }
}

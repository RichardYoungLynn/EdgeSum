package com.example.edgesum.page.objectdetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.edgesum.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import wseemann.media.FFmpegMediaMetadataRetriever;

import static android.os.Environment.getExternalStorageDirectory;

public class ObjectDetectionActivity extends AppCompatActivity {

    public static int YOLOV5 = 1;
    public static int SQUEEZENET = 2;
    public static int STYLETRANSFER = 3;
    public static int MOBILENETSSD = 4;
    public static int MTCNN = 5;

    public static int USE_MODEL = YOLOV5;

    private static final int SELECT_IMAGE = 1;
    private static final int SELECT_VIDEO = 2;

    private ImageView detectedImageView;
    private TextView detectInfo;
    private Button buttonImage;
    private Button buttonVideo;
    private Button buttonDetect;
    private Button buttonDetectGPU;

    private Bitmap bitmap = null;
    private Bitmap detectedImage = null;

    private YoloV5Ncnn yolov5ncnn = new YoloV5Ncnn();

    FFmpegMediaMetadataRetriever mmr;

    protected long videoCurFrameLoc = 0;
    private long startTime = 0;
    private long endTime = 0;
    private int width;
    private int height;

    double total_fps = 0;
    int fps_count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_detection);

        initModel();
        initViewID();
        initViewListener();
    }

    private void initModel() {
        if (USE_MODEL == YOLOV5) {
            boolean ret_init = yolov5ncnn.Init(getAssets());
            if (!ret_init) {
                Log.e("ObjectDetectionActivity", "yolov5ncnn Init failed");
            }
        } else if (USE_MODEL == SQUEEZENET) {
            boolean ret_init = yolov5ncnn.Init(getAssets());
            if (!ret_init) {
                Log.e("ObjectDetectionActivity", "squeezenet Init failed");
            }
        } else if (USE_MODEL == STYLETRANSFER) {
            boolean ret_init = yolov5ncnn.Init(getAssets());
            if (!ret_init) {
                Log.e("ObjectDetectionActivity", "styletransfer Init failed");
            }
        } else if (USE_MODEL == MOBILENETSSD) {
            boolean ret_init = yolov5ncnn.Init(getAssets());
            if (!ret_init) {
                Log.e("ObjectDetectionActivity", "mobilenetssd Init failed");
            }
        } else if (USE_MODEL == MTCNN) {
            boolean ret_init = yolov5ncnn.Init(getAssets());
            if (!ret_init) {
                Log.e("ObjectDetectionActivity", "mtcnn Init failed");
            }
        }
    }

    private void initViewID() {
        detectedImageView = (ImageView) findViewById(R.id.detectedImageView);
        detectInfo = (TextView) findViewById(R.id.detectInfo);
        buttonImage = (Button) findViewById(R.id.buttonImage);
        buttonVideo = (Button) findViewById(R.id.buttonVideo);
        buttonDetect = (Button) findViewById(R.id.buttonDetect);
        buttonDetectGPU = (Button) findViewById(R.id.buttonDetectGPU);
    }

    private void initViewListener() {
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, SELECT_IMAGE);
            }
        });
        buttonVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("video/*");
                startActivityForResult(i, SELECT_VIDEO);
            }
        });
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (detectedImage == null)
                    return;
                YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(detectedImage, false);
                showObjects(objects);
            }
        });
        buttonDetectGPU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (detectedImage == null)
                    return;
                YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(detectedImage, true);
                showObjects(objects);
            }
        });
    }

    private void showObjects(YoloV5Ncnn.Obj[] objects) {
        if (objects == null) {
            detectedImageView.setImageBitmap(bitmap);
            return;
        }

        // draw objects on bitmap
        Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        final int[] colors = new int[] {
                Color.rgb( 54,  67, 244),
                Color.rgb( 99,  30, 233),
                Color.rgb(176,  39, 156),
                Color.rgb(183,  58, 103),
                Color.rgb(181,  81,  63),
                Color.rgb(243, 150,  33),
                Color.rgb(244, 169,   3),
                Color.rgb(212, 188,   0),
                Color.rgb(136, 150,   0),
                Color.rgb( 80, 175,  76),
                Color.rgb( 74, 195, 139),
                Color.rgb( 57, 220, 205),
                Color.rgb( 59, 235, 255),
                Color.rgb(  7, 193, 255),
                Color.rgb(  0, 152, 255),
                Color.rgb( 34,  87, 255),
                Color.rgb( 72,  85, 121),
                Color.rgb(158, 158, 158),
                Color.rgb(139, 125,  96)
        };

        Canvas canvas = new Canvas(rgba);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        Paint textbgpaint = new Paint();
        textbgpaint.setColor(Color.WHITE);
        textbgpaint.setStyle(Paint.Style.FILL);

        Paint textpaint = new Paint();
        textpaint.setColor(Color.BLACK);
        textpaint.setTextSize(26);
        textpaint.setTextAlign(Paint.Align.LEFT);

        for (int i = 0; i < objects.length; i++) {
            paint.setColor(colors[i % 19]);

            canvas.drawRect(objects[i].x, objects[i].y, objects[i].x + objects[i].w, objects[i].y + objects[i].h, paint);

            // draw filled text inside image
            {
                String text = objects[i].label + " = " + String.format("%.1f", objects[i].prob * 100) + "%";

                float text_width = textpaint.measureText(text);
                float text_height = - textpaint.ascent() + textpaint.descent();

                float x = objects[i].x;
                float y = objects[i].y - text_height;
                if (y < 0)
                    y = 0;
                if (x + text_width > rgba.getWidth())
                    x = rgba.getWidth() - text_width;

                canvas.drawRect(x, y, x + text_width, y + text_height, textbgpaint);

                canvas.drawText(text, x, y - textpaint.ascent(), textpaint);
            }
        }

        detectedImageView.setImageBitmap(rgba);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && null != data) {
            Uri uri = data.getData();

            try {
                if (requestCode == SELECT_IMAGE) {
                    bitmap = decodeUri(uri);
                    detectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    detectedImageView.setImageBitmap(bitmap);
                }
                else if (requestCode == SELECT_VIDEO) {
                    detectOnVideo(uri.getPath());
                }
            }
            catch (FileNotFoundException e) {
                Log.e("MainActivity", "FileNotFoundException");
                return;
            }
        }
    }

    private void detectOnVideo(final String path) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mmr = new FFmpegMediaMetadataRetriever();
                mmr.setDataSource(path);
                String dur = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);  // ms
                String sfps = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE);  // fps
                String sWidth = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);  // w
                String sHeight = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);  // h
                String rota = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);  // rotation
                int duration = Integer.parseInt(dur);
                float fps = Float.parseFloat(sfps);
                float rotate = 0;
                if (rota != null) {
                    rotate = Float.parseFloat(rota);
                }
                float frameDis = 1.0f / fps * 1000 * 1000;
                videoCurFrameLoc = 0;
                while (videoCurFrameLoc < duration * 1000) {
                    videoCurFrameLoc = (long) (videoCurFrameLoc + frameDis);
                    final Bitmap b1 = mmr.getFrameAtTime(videoCurFrameLoc, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
                    if (b1 == null) {
                        continue;
                    }
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotate);
                    width = b1.getWidth();
                    height = b1.getHeight();
                    final Bitmap b2 = Bitmap.createBitmap(b1, 0, 0, width, height, matrix, false);
                    startTime = System.currentTimeMillis();
                    bitmap = b2;
                    detectedImage = b2.copy(Bitmap.Config.ARGB_8888, true);
                    detectAndDraw(detectedImage);
                    frameDis = 1.0f / fps * 1000 * 1000;
                }
                mmr.release();
            }
        }, "video detect");
        thread.start();
    }

    protected void detectAndDraw(Bitmap image) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(image, true);
                showObjects(objects);
                endTime = System.currentTimeMillis();
                long dur = endTime - startTime;
                float fps = (float) (1000.0 / dur);
                total_fps = (total_fps == 0) ? fps : (total_fps + fps);
                fps_count++;
                detectInfo.setText(String.format(Locale.CHINESE,
                        "%s\nSize: %dx%d\nTime: %.3f s\nFPS: %.3f\nAVG_FPS: %.3f",
                        "yolov5", height, width, dur / 1000.0, fps, (float) total_fps / fps_count));
            }
        });
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 640;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

        // Rotate according to EXIF
        int rotate = 0;
        try {
            ExifInterface exif = new ExifInterface(getContentResolver().openInputStream(selectedImage));
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        }
        catch (IOException e) {
            Log.e("MainActivity", "ExifInterface IOException");
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

}
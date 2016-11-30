package com.example.pierre.mycamera;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener{

    //keep track of camera capture intent
    final int REQUEST_IMAGE_CAPTURE = 1;
    Uri photoURI; //the Uri for the captured image
    ImageView mImageView;
    Canvas canvas;
    Paint paint;
    Context mContext;
    File photoFile;
    String mCurrentPhotoPath;
    float downx = 0, downy = 0, upx = 0, upy = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView)findViewById(R.id.picture);
        // mContext = MainActivity.this;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id){
            case R.id.action_settings:
                return true;
            case R.id.capture_btn:
                try {
                    Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    // Ensure that there's a camera activity to handle the intent
                    ComponentName cameraApp = captureIntent.resolveActivity(getPackageManager());
                    if (cameraApp != null) {
                        // Create the File where the photo should go
                        photoFile = null;
                        try {
                            photoFile = createImageFile();
                        } catch (IOException ex) {

                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            photoURI = FileProvider.getUriForFile(this, "com.example.pierre.mycamera.fileprovider", photoFile);
                            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            //The default Android camera application returns a non-null
                            // intent only when passing back a thumbnail in the return Intent.
                            grantUriPermission(cameraApp.getPackageName(), photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivityForResult(captureIntent, REQUEST_IMAGE_CAPTURE);
                        }
                    }
                }
                catch(ActivityNotFoundException anfe){
                    //display an error message
                    String errorMessage = "Whoops - your device doesn't support capturing images!";
                    Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
                    toast.show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            //user is returning from capturing an image using the camera
            if(requestCode == REQUEST_IMAGE_CAPTURE){
                // Photo has been taken and saved onto storage. Loading it into an ImageView...
                setPic();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timestamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void displayLayers(Bitmap bmp) {
        Resources r = getResources();
        Drawable d = new BitmapDrawable(r,bmp);// converting bitmap to drawable
        Drawable[] layers = new Drawable[3];
        layers[0] = d;
        layers[1] = r.getDrawable(R.drawable.orangeline);
        // layers[1].setBounds(0,0,20,50);
        // ColorDrawable leftBorder = new ColorDrawable(Color.RED);
        TextView tv = new TextView(getApplicationContext());
        tv.setText("hello");
        // layers[2] = tv;
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        // layerDrawable.setLayerInset(0,0,0,15,0);
        mImageView.setImageDrawable(layerDrawable);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(mutableBitmap);
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(10);
        mImageView.setImageBitmap(mutableBitmap);
        mImageView.setOnTouchListener(this);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downx = event.getX();
                downy = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                upx = event.getX();
                upy = event.getY();
                canvas.drawLine(downx, downy, upx, upy, paint);
                fillArrow(canvas,downx,downy,upx,upy);
                mImageView.invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }
        return true;
    }

    private void fillArrow(Canvas canvas, float x0, float y0, float x1, float y1) {

        paint.setStyle(Paint.Style.FILL);

        float deltaX = x1 - x0;
        float deltaY = y1 - y0;
        double distance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
        float frac = (float) (1 / (distance / 30));

        float point_x_1 = x0 + (float) ((1 - frac) * deltaX + frac * deltaY);
        float point_y_1 = y0 + (float) ((1 - frac) * deltaY - frac * deltaX);

        float point_x_2 = x1;
        float point_y_2 = y1;

        float point_x_3 = x0 + (float) ((1 - frac) * deltaX - frac * deltaY);
        float point_y_3 = y0 + (float) ((1 - frac) * deltaY + frac * deltaX);

        float x_4 = x0 - deltaX + (float) ((1 - frac) * deltaX + frac * deltaY);
        float y_4 = y0 - deltaY + (float) ((1 - frac) * deltaY - frac * deltaX);
        float point_x_4 = x0 + (x0 - x_4);
        float point_y_4 = y0 + (y0 - y_4);

        float point_x_5 = x0;
        float point_y_5 = y0;

        float x_6 = x0 - deltaX + (float) ((1 - frac) * deltaX - frac * deltaY);
        float y_6 = y0 - deltaY + (float) ((1 - frac) * deltaY + frac * deltaX);
        float point_x_6 = x0 + (x0 - x_6);
        float point_y_6 = y0 + (y0 - y_6);

        Path path = new Path();
        Path path2 = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path2.setFillType(Path.FillType.EVEN_ODD);

        path.moveTo(point_x_1, point_y_1);
        path.lineTo(point_x_2, point_y_2);
        path.lineTo(point_x_3, point_y_3);
        path.close();

        path2.moveTo(point_x_6,point_y_6);
        path2.lineTo(point_x_5,point_y_5);
        path2.lineTo(point_x_4, point_y_4);
        path2.close();

        canvas.drawPath(path, paint);
        canvas.drawPath(path2, paint);
    }
}

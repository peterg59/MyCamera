package com.example.pierre.mycamera;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    //keep track of camera capture intent
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private File photoFile;
    private Uri photoURI; //the Uri for the captured image
    private String mCurrentPhotoPath;
    private ImageView mImageView;
    private Canvas canvas;
    private Paint paint;
    private float downx = 0, downy = 0, upx = 0, upy = 0;
    private String distanceLine;
    private EditText editText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.picture);
        editText = (EditText) findViewById(R.id.editText);
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
        switch (id) {
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
                            //display an error message
                            String errorMessage = "Whoops - we cannot create an image file!";
                            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            photoURI = FileProvider.getUriForFile(this, "com.example.pierre.mycamera.fileprovider", photoFile);
                            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            /// This line is essential for this code to work on API < 24.
                            // This grants permission to the Camera app to write to the specified URI,
                            // i.e. the folder where pictures are saved for your application.
                            grantUriPermission(cameraApp.getPackageName(), photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivityForResult(captureIntent, REQUEST_IMAGE_CAPTURE);
                        }
                    }
                } catch (ActivityNotFoundException anfe) {
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            //user is returning from capturing an image using the camera
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Photo has been taken and saved onto storage. Loading it into an ImageView...
                setPic();
            }
        }
    }

    public File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timestamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void setPic() {
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
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        // Resize the Bitmap to have the same dimensions as the ImageView
        bitmap = getResizedBitmap(bitmap, targetW, targetH);
        // Make the Bitmap mutable
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        // Create a Canvas with including the Bitmap
        canvas = new Canvas(mutableBitmap);
        // Define the Paint object with specific features
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5 / 2);
        paint.setTextSize(16);
        // We insert the Bitmap on the ImageView object
        mImageView.setImageBitmap(mutableBitmap);
        mImageView.setOnTouchListener(this);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            // A pressed gesture has started, the motion contains the initial starting location.
            case MotionEvent.ACTION_DOWN:
                //Getting the coordinates of the line's bottom
                downx = event.getX();
                downy = event.getY();
                break;

            // A pressed gesture has finished, the motion contains the final release location.
            case MotionEvent.ACTION_UP:
                //Getting the coordinates of the line's top
                upx = event.getX();
                upy = event.getY();
                //We draw the line thanks to the coordinates and the Paint object
                canvas.drawLine(downx, downy, upx, upy, paint);
                // We draw double-ended arrows at each end of the line
                fillArrow(canvas, downx, downy, upx, upy);
                // We add the dimension of the line
                insertText(downx, downy, upx, upy, paint);
                mImageView.invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }
        return true;
    }

    public void fillArrow(Canvas canvas, float x0, float y0, float x1, float y1) {

        paint.setStyle(Paint.Style.FILL);

        float deltaX = x1 - x0;
        float deltaY = y1 - y0;
        double distance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
        float frac = (float) (1 / (distance / 15));

        float point_x_1 = x0 + ((1 - frac) * deltaX + frac * deltaY);
        float point_y_1 = y0 + ((1 - frac) * deltaY - frac * deltaX);

        float point_x_2 = x1;
        float point_y_2 = y1;

        float point_x_3 = x0 + ((1 - frac) * deltaX - frac * deltaY);
        float point_y_3 = y0 + ((1 - frac) * deltaY + frac * deltaX);

        float x_4 = x0 - deltaX + ((1 - frac) * deltaX + frac * deltaY);
        float y_4 = y0 - deltaY + ((1 - frac) * deltaY - frac * deltaX);
        float point_x_4 = x0 + (x0 - x_4);
        float point_y_4 = y0 + (y0 - y_4);

        float point_x_5 = x0;
        float point_y_5 = y0;

        float x_6 = x0 - deltaX + ((1 - frac) * deltaX - frac * deltaY);
        float y_6 = y0 - deltaY + ((1 - frac) * deltaY + frac * deltaX);
        float point_x_6 = x0 + (x0 - x_6);
        float point_y_6 = y0 + (y0 - y_6);

        Path path = new Path();
        Path path2 = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path2.setFillType(Path.FillType.EVEN_ODD);

        // put the borders of the arrows
        path.moveTo(point_x_1, point_y_1);
        path.lineTo(point_x_2, point_y_2);
        path.lineTo(point_x_3, point_y_3);
        path.close();

        path2.moveTo(point_x_6, point_y_6);
        path2.lineTo(point_x_5, point_y_5);
        path2.lineTo(point_x_4, point_y_4);
        path2.close();

        // draw the arrow on the top of the line
        canvas.drawPath(path, paint);
        // draw the arrow on the bottom of the line
        canvas.drawPath(path2, paint);
    }

    public void insertText(float startX, float startY, float stopX, float stopY, Paint paint) {
        float deltaX = stopX - startX;
        float deltaY = stopY - startY;
        float x = startX + deltaX / 2;
        float y = startY + deltaY / 2;
        // Getting the dimension from the EditText object
        distanceLine = editText.getText().toString();
        // Draw the text on the coordinates taken
        canvas.drawText(distanceLine + "cm", x, y, paint);
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        // get the ratios between the ImageView dimensions and the Bitmap's
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix
        Matrix matrix = new Matrix();
        // scale the matrix accroding to the widht and height scaled
        matrix.postScale(scaleWidth, scaleHeight);
        //resized the Bitmap wanted
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }
}

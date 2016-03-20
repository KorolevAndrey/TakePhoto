package example.freakybyte.takephoto;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by xkokushox on 20/03/16.
 */

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private final static int REQUEST_TAKE_PHOTO = 1001;
    private final static int REQUEST_CHOOSE_PHOTO = 1002;
    private final static int CAMERA_PERMISSION = 3;

    //TODO enable if you want a Thumb Size set it false
    private final boolean bImageFullSize = true;

    private final static String TAG_URI = "file_uri";
    private final static String TAG_PATH = "file_path";

    private ImageView mImagePreview;

    private Uri uriCurrentImage;
    private String sImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            try {
                uriCurrentImage = savedInstanceState.getParcelable(TAG_URI);
                sImagePath = savedInstanceState.getString(TAG_PATH);
                getImagePreview().setImageURI(uriCurrentImage);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_TAKE_PHOTO) {
                onCaptureImageResult(data);
            } else if (requestCode == REQUEST_CHOOSE_PHOTO)
                onSelectFromGalleryResult(data);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d(TAG, "User Cancel Photo action");
        } else {
            Toast.makeText(MainActivity.this, "Image could not be retrieved", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePhoto();
                } else {
                    Toast.makeText(this, "Please grant camera permission to use the camera", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    private void dispatchTakePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (bImageFullSize) {
            File photoFile = getOutputMediaFile();
            if (photoFile != null) {
                uriCurrentImage = Uri.fromFile(photoFile);
                sImagePath = photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriCurrentImage);
            }
        }
        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
    }

    public void onTakePhotoClick(View v) {

        final CharSequence[] items = {"Take Photo", "Choose from Library", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION);
                    } else
                        dispatchTakePhoto();
                } else if (items[item].equals("Choose from Library")) {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_CHOOSE_PHOTO);
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {
        uriCurrentImage = data.getData();
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = managedQuery(uriCurrentImage, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        cursor.moveToFirst();

        sImagePath = cursor.getString(column_index);

        Bitmap bm;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(sImagePath, options);
        final int REQUIRED_SIZE = 200;
        int scale = 1;
        while (options.outWidth / scale / 2 >= REQUIRED_SIZE && options.outHeight / scale / 2 >= REQUIRED_SIZE)
            scale *= 2;
        //TODO enable if you want to scale it
        //options.inSampleSize = scale;
        options.inJustDecodeBounds = false;
        bm = BitmapFactory.decodeFile(sImagePath, options);

        getImagePreview().setImageBitmap(bm);
    }

    private void onCaptureImageResult(Intent data) {
        final Bitmap thumbnail;
        try {
            if (bImageFullSize) {
                int targetW = getImagePreview().getWidth();
                int targetH = getImagePreview().getHeight();

                // Get the dimensions of the bitmap
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(sImagePath, bmOptions);
                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;

                // Determine how much to scale down the image
                int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

                // Decode the image file into a Bitmap sized to fill the View
                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = scaleFactor;
                bmOptions.inPurgeable = true;

                thumbnail = BitmapFactory.decodeFile(sImagePath, bmOptions);
            } else {
                thumbnail = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

                File destination = new File(Environment.getExternalStorageDirectory(), "TakePhoto_" + System.currentTimeMillis() + ".jpg");

                FileOutputStream fo;

                destination.createNewFile();
                fo = new FileOutputStream(destination);
                fo.write(bytes.toByteArray());
                fo.close();
                uriCurrentImage = Uri.fromFile(destination);
                sImagePath = destination.getAbsolutePath();

            }
            getImagePreview().setImageBitmap(thumbnail);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public File getOutputMediaFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TakePhoto_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        try {
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            return image;
        } catch (Exception ex) {
            Log.e("FileUtil", ex.getMessage());
            Toast.makeText(MainActivity.this, "There was a problem creting the file", Toast.LENGTH_LONG);
            return null;
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelable(TAG_URI, uriCurrentImage);
        savedInstanceState.putString(TAG_PATH, sImagePath);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        uriCurrentImage = savedInstanceState.getParcelable(TAG_URI);
        sImagePath = savedInstanceState.getString(TAG_PATH);
    }

    private ImageView getImagePreview() {
        if (mImagePreview == null)
            mImagePreview = (ImageView) findViewById(R.id.img_photo_preview);
        return mImagePreview;
    }
}

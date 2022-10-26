package com.example.travelblog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;

public class AddPostActivity extends AppCompatActivity {


    EditText title_blog, description_blog;
    Button upload;
    ImageView blog_image;
    Uri image_uri = null;
    ProgressDialog pd;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        createNotificationChannel();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Add post");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);


        permission();
        title_blog = findViewById(R.id.title_blog);
        description_blog = findViewById(R.id.description_blog);

        upload = findViewById(R.id.upload);
        Intent notificationsIntent= new Intent(this, HomeActivity.class);
        notificationsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,notificationsIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "M")
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("NOVI POST")
                .setContentText("Upravo je izbacen novi post." )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE);

        final NotificationManagerCompat notificationManager= NotificationManagerCompat.from(this);

        blog_image = findViewById(R.id.post_image_blog);

        pd = new ProgressDialog(this);
        auth = FirebaseAuth.getInstance();


        blog_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePickDialog();
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String title = title_blog.getText().toString();
                String description = description_blog.getText().toString();

                if (TextUtils.isEmpty(title)) {
                    title_blog.setError("Tittle is required");
                } else if (TextUtils.isEmpty(description)) {
                    description_blog.setError("Description is required");
                }else if (blog_image.getDrawable() == null){
                    title_blog.setError("Image is required");
                } else {
                    uploadData(title, description);
                    notificationManager.notify(100, builder.build());
                }

            }
        });

    }

    private void uploadData(final String title, final String description) {

        pd.setMessage("publishing post");
        pd.show();

        final String timeStamp = String.valueOf(System.currentTimeMillis());

        String filepath = "Posts/" + "post_" + timeStamp;

        if (blog_image.getDrawable() != null) {

            Bitmap bitmap = ((BitmapDrawable) blog_image.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            StorageReference reference = FirebaseStorage.getInstance().getReference().child(filepath);
            reference.putBytes(data)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();

                            while (!uriTask.isSuccessful()) ;

                            String downloadUri = uriTask.getResult().toString();

                            if (uriTask.isSuccessful()) {
                                //uri is received post is published to database

                                FirebaseUser user = auth.getCurrentUser();

                                HashMap<String, Object> hashMap = new HashMap<>();

                                hashMap.put("uid", user.getUid());
                                hashMap.put("uEmail", user.getEmail());
                                hashMap.put("pid", timeStamp);
                                hashMap.put("pTitle", title);
                                hashMap.put("pImage", downloadUri);
                                hashMap.put("pDescription", description);
                                hashMap.put("pTime", timeStamp);

                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                ref.child(timeStamp).setValue(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                pd.dismiss();
                                                Toast.makeText(AddPostActivity.this, "Post published", Toast.LENGTH_SHORT).show();
                                                title_blog.setText("");
                                                description_blog.setText("");
                                                blog_image.setImageURI(null);
                                                image_uri = null;

                                                //when post is published user must go to home activity means main dashboard
                                                startActivity(new Intent(AddPostActivity.this, HomeActivity.class));
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        pd.dismiss();
                                        Toast.makeText(AddPostActivity.this, "" +e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(AddPostActivity.this, "" +e.getMessage(), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });

        }
    }

    private void imagePickDialog() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose image from ");

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    cameraPick();
                }
                if (which == 1) {
                    galleryPick();
                }
            }
        });

        builder.create().show();

    }

    private void cameraPick() {
        //here we will do this for camera
        ContentValues cv = new ContentValues();

      Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        startActivityForResult(takePicture, 0);
    }

    private void galleryPick() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, 1);
    }

    private void permission() {
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {/* ... */}

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
        }).check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    Bundle extras = imageReturnedIntent.getExtras();
                    Bitmap imageBitmap =(Bitmap) extras.get("data");
                    blog_image.setImageBitmap(imageBitmap);
                }

                break;
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    blog_image.setImageURI(selectedImage);
                }
                break;
        }
    }

     public void createNotificationChannel(){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                CharSequence name = "Travel blog";
                String desc= "Channel for notifications";
                int importance= NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel("M", name,importance);
                channel.setDescription(desc);

                NotificationManager notificationManager= getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);

            }

     }
}

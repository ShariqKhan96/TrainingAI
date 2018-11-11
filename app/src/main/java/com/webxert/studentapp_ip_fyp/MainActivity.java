package com.webxert.studentapp_ip_fyp;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.microsoft.projectoxford.face.contract.Person;
import com.microsoft.projectoxford.face.contract.PersonGroup;
import com.microsoft.projectoxford.face.contract.TrainingStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private FaceServiceRestClient faceServiceRestClient = new FaceServiceRestClient("https://westcentralus.api.cognitive.microsoft.com/face/v1.0", "7c73377533b04d21a99310908f9e008e");

    ProgressDialog dialog;
    PersonGroup group;

    Button pick_image;

    EditText personName;

    TextView images_count_tv;
    public static final int PICK_IMAGE = 1;

    static final String GROUP_ID = "1d";


    ByteArrayInputStream byteArrayInputStream;

    Bitmap[] bitmaps;

    final AsyncTask<Void, String, CreatePersonResult> createPersonResult = new AsyncTask<Void, String, CreatePersonResult>() {

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected void onPostExecute(CreatePersonResult createPersonResult) {
            dialog.dismiss();
            Log.e("personId", String.valueOf(createPersonResult.personId));
            if (createPersonResult != null) {

                Log.e(MainActivity.class.getSimpleName(), "Person id is " + createPersonResult.personId);
                new DetectAndRegister(createPersonResult).execute();

            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            publishProgress(values[0]);
        }

        @Override
        protected CreatePersonResult doInBackground(Void... voids) {
            try {
                publishProgress("Creating person in group");
                //dialog.dismiss();
                return faceServiceRestClient.createPerson(GROUP_ID, personName.getText().toString(), null);

            } catch (Exception e) {
                e.printStackTrace();
                dialog.dismiss();
                return null;
            }
        }
    };


    final AsyncTask<Void, String, Boolean> createPersonGroup = new AsyncTask<Void, String, Boolean>() {
        @Override
        protected void onProgressUpdate(String... values) {
            dialog.setMessage(values[0]);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                //updating old group. If you want to create a new group change to createPerson Group....
                faceServiceRestClient.updatePersonGroup("1d", "UBIT Students", null);
            } catch (Exception e) {
                Log.e("Client/IO Exception", e.getMessage());
                publishProgress("Exception caught");
                e.printStackTrace();
                dialog.dismiss();
                return false;
            }


            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            dialog.dismiss();
            try {
                getPersonResult.execute();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("UpdatingError", e.getMessage());

            }
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            if (data.getClipData() != null && data.getClipData().getItemCount() > 1) {
                //MULTIPICK

                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                ClipData clipData = data.getClipData();

                images_count_tv.setText(String.format("%s images are picked!", data.getClipData().getItemCount() + " "));


                /// / ArrayList<Uri> arrayList = new ArrayList<>();

                bitmaps = new Bitmap[clipData.getItemCount()];
                for (int i = 0; i < clipData.getItemCount(); i++) {

                    ClipData.Item item = clipData.getItemAt(i);

                    Uri uri = item.getUri();

                    Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    try {
                        //Log.e("imageUri", cursor.getString(columnIndex));
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        bitmaps[i] = bitmap;
                        cursor.close();


                    } catch (IOException e) {

                        Log.e(TAG, e.getMessage());
                    }

                }

            } else {
                Uri uri = data.getData();
                bitmaps = new Bitmap[3];
                for (int i = 0; i < 3; i++) {

                    try {
                        bitmaps[i] = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    } catch (IOException e) {
                        Log.e(TAG, "ImageDecodeException");
                    }
                }
            }
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        personName = findViewById(R.id.person_name);

        pick_image = findViewById(R.id.pick_images);
        images_count_tv = findViewById(R.id.image_selected_number);

        pick_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(intent, PICK_IMAGE);
            }
        });

        //here create function which returns bitmap

//        bitmaps[0] = BitmapFactory.decodeResource(getResources(), R.drawable.zayn);
//        bitmaps[1] = BitmapFactory.decodeResource(getResources(), R.drawable.zayn_2);
//        bitmaps[2] = BitmapFactory.decodeResource(getResources(), R.drawable.zayn_3);


        findViewById(R.id.press_me).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPersonGroup.execute();

            }
        });


        dialog = new ProgressDialog(this);
        dialog.setTitle("Identifying and Training..");


    }


    private class DetectAndRegister extends AsyncTask<InputStream, String, Boolean> {

        CreatePersonResult createPersonResult;


        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        public DetectAndRegister(CreatePersonResult createPersonResult) {
            this.createPersonResult = createPersonResult;
        }

        @Override
        protected Boolean doInBackground(InputStream... inputStreams) {



            //creating function which returns compressed bitmap

            try {

                for (Bitmap bitmap : bitmaps) {

//                    ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
//                    progressDialog.setMessage("Uploading images");
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                    byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                    publishProgress("Training....");
                    try {
                      //  progressDialog.show();

                        Log.e("Here", "Here");

                        AddPersistedFaceResult faceResult = faceServiceRestClient.addPersonFace(GROUP_ID, createPersonResult.personId, byteArrayInputStream, null, null);
                        if (faceResult != null) {
                           // progressDialog.dismiss();
                            publishProgress("Training....");
                            Log.e("persistedFaceId", String.valueOf(faceResult.persistedFaceId));

                        }
                    } catch (Exception e) {

                        //progressDialog.dismiss();
                        publishProgress(e.getLocalizedMessage());
                        Log.e("ImageTrainingException", e.getMessage());
                    }

                    //for loop to give 3 pictures
//
//                    Thread thread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//
//                            }
//
//                        }
//                    });
//                    thread.start();

                    byteArrayOutputStream.close();
                    byteArrayInputStream.close();
                }
            } catch (Exception e) {

                Log.e("AddingFaceException", e.getMessage());
                e.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            dialog.dismiss();

            if (bool)
                new TrainPersonGroup().execute();
            else Log.e("TrainingResult", String.valueOf(bool));

        }
    }


    final AsyncTask<String, String, Boolean> getPersonResult = new AsyncTask<String, String, Boolean>() {


        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(String... voids) {
            try {
                group = faceServiceRestClient.getPersonGroup(GROUP_ID);
                publishProgress("GettingPersonResult");
            } catch (Exception e) {
                e.printStackTrace();
                publishProgress("Eception: " + e.getMessage());
                Log.e("GettingpersonError", e.getMessage());
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            dialog.dismiss();
            if (group != null) {
                Log.e("personCreated", String.valueOf(bool));
                if (bool) {
                    createPersonResult.execute();
                }
            }
        }
    };

    private class TrainPersonGroup extends AsyncTask<Void, String, Void> {


        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dialog.dismiss();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            dialog.setMessage(values[0]);
        }

        @Override
        protected Void doInBackground(Void... voids) {


            try {
                publishProgress("Fetching training status");
                faceServiceRestClient.trainPersonGroup(GROUP_ID);
                TrainingStatus trainingStatus = null;

                while (true) {
                    trainingStatus = faceServiceRestClient.getPersonGroupTrainingStatus(GROUP_ID);
                    Log.e("TrainingStatus", trainingStatus.status + " ");
                    if (trainingStatus.status != TrainingStatus.Status.Running) {
                        publishProgress("Current training status is " + trainingStatus.status);
                        break;

                    }

                    Thread.sleep(1000);
                }
                Log.e("TrainingAI", "Training Completed !");
              //  finish();


            } catch (Exception e) {
                Log.d("Training Error", e.getMessage());
                e.printStackTrace();
            }

            return null;
        }
    }

}


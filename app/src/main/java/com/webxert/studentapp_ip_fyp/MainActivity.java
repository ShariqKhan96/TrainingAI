package com.webxert.studentapp_ip_fyp;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.microsoft.projectoxford.face.contract.PersonGroup;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.microsoft.projectoxford.face.rest.ClientException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private FaceServiceRestClient faceServiceRestClient = new FaceServiceRestClient("https://westcentralus.api.cognitive.microsoft.com/face/v1.0", "fb5b93f74a464f19992f53dce8e24af0");

    ProgressDialog dialog;
    PersonGroup group;

    Bitmap bitmap;
    ByteArrayInputStream byteArrayInputStream;

    Bitmap[] bitmaps = new Bitmap[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //here create function which returns bitmap

        bitmaps[0] = BitmapFactory.decodeResource(getResources(), R.drawable.zayn);
        bitmaps[1] = BitmapFactory.decodeResource(getResources(), R.drawable.zayn2);
        bitmaps[2] = BitmapFactory.decodeResource(getResources(), R.drawable.zayn3);


        final AsyncTask<Void, String, CreatePersonResult> createPersonResult = new AsyncTask<Void, String, CreatePersonResult>() {

            @Override
            protected void onPostExecute(CreatePersonResult createPersonResult) {
                Log.e("personId", String.valueOf(createPersonResult.personId));
                if (createPersonResult != null) {

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
                    return faceServiceRestClient.createPerson("students", "shariq", null);

                } catch (Exception e) {
                    Log.e("ExceptionCreatingPerson", "EXCEPTION");
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
                    faceServiceRestClient.createPersonGroup("students", "UBIT students", null);
                } catch (Exception e) {
                    Log.e("Client/IO Exception", e.getMessage());
                    publishProgress("Exception caught");
                    dialog.dismiss();
                    return false;
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean bool) {
                dialog.dismiss();
                try {
                    group = faceServiceRestClient.getPersonGroup("students");
                    if (group != null) {
                        Log.e("personCreated", String.valueOf(bool));
                        if (bool)
                            createPersonResult.execute();

                    }


                } catch (Exception e) {
                    Log.e("GettingGroupException", e.getMessage());
                }
            }
        };

        findViewById(R.id.press_me).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPersonGroup.execute();

            }
        });


        dialog = new ProgressDialog(this);
        dialog.setTitle("Dectecting and Identifying..");


    }


    private class DetectAndRegister extends AsyncTask<InputStream, String, Boolean> {

        CreatePersonResult createPersonResult;

        public DetectAndRegister(CreatePersonResult createPersonResult) {
            this.createPersonResult = createPersonResult;
        }

        @Override
        protected Boolean doInBackground(InputStream... inputStreams) {

            //creating function which returns compressed bitmap

            try {

                for (Bitmap bitmap : bitmaps) {

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                    byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

                    //for loop to give 3 pictures

                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                AddPersistedFaceResult faceResult = faceServiceRestClient.addPersonFace("students", createPersonResult.personId, byteArrayInputStream, null, null);
                                if (faceResult != null) {
                                    Log.e("persistedFaceId", String.valueOf(faceResult.persistedFaceId));

                                }
                            } catch (Exception e) {

                                Log.e("ImageTrainingException", e.getMessage());
                            }

                        }
                    });
                    thread.start();
                }
            } catch (Exception e) {
                Log.e("AddingFaceException", e.getMessage());
                return false;
            }


            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (bool)
                new TrainPersonGroup().execute();
            else Log.e("TrainingResult", String.valueOf(bool));

        }
    }


    private class TrainPersonGroup extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {


            try {
                faceServiceRestClient.trainPersonGroup("students");
                TrainingStatus trainingStatus = null;

                while (true) {
                    trainingStatus = faceServiceRestClient.getPersonGroupTrainingStatus("students");
                    if (trainingStatus.status != TrainingStatus.Status.Running)
                        break;
                    Thread.sleep(1000);
                }
                Log.e("TrainingAI", "Training Completed !");


            } catch (Exception e) {
                Log.e("TrainingException", e.getMessage());
            }

            return null;
        }
    }

}


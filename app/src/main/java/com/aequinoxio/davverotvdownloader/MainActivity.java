package com.aequinoxio.davverotvdownloader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.aequinoxio.davverotvdownloader.DownloadLogic.ParseVideoPage;
import com.aequinoxio.davverotvdownloader.DownloadLogic.UpdateEvent;
import com.aequinoxio.davverotvdownloader.DownloadLogic.VideoDetails;
import com.aequinoxio.davverotvdownloader.AndroidLogic.VideoDetailsParcelable;
import com.aequinoxio.davverotvdownloader.DownloadLogic.VideoUrlAndQuality;
import com.aequinoxio.davverotvdownloader.DownloadLogic.WorkerUpdateCallback;
import com.google.android.material.snackbar.Snackbar;

import java.net.URL;
import java.util.List;

import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG;

public class MainActivity extends AppCompatActivity {

    private static final String READDETAILSFROMBUNDLE = "READDETAILSFROMBUNDLE";
    private VideoDetailsParcelable videoDetailsParcelable;
    boolean spinnerInitialization=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            VideoDetailsParcelable videoDetailsParcelable = savedInstanceState.getParcelable(READDETAILSFROMBUNDLE);
            populateTree(videoDetailsParcelable);
            //((TextView)findViewById(R.id.txtLogView)).setText("");
        }
        //// DEBUG
        ((EditText) findViewById(R.id.editTextTextUrl)).setText("https://www.davvero.tv/tgbyoblu24/videos/tg-byoblu24-26-febbraio-2021-edizione-19-00");
        ////

        Button button = findViewById(R.id.btnStart);

        button.setOnClickListener((View.OnClickListener) v -> {
            ParserAsyncTask parserAsyncTask = new ParserAsyncTask();
            parserAsyncTask.execute();
            ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
            ((ProgressBar) findViewById(R.id.progressBarCircular)).setVisibility(View.VISIBLE);
            findViewById(R.id.btnCopy).setEnabled(false);
            button.setEnabled(false);
            ((TextView) findViewById(R.id.txtLogView)).setText("");
        });

        Spinner spinner = findViewById(R.id.spinner);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerInitialization){
                    spinnerInitialization=false;
                    return;
                }
                ((TextView) findViewById(R.id.txtLogView)).append(((VideoUrlAndQuality) parent.getSelectedItem()).getQuality()+"\n");
                ((TextView) findViewById(R.id.txtLogView)).append(((VideoUrlAndQuality) parent.getSelectedItem()).getVideoUrl() + "\n\n");

                //((TextView) findViewById(R.id.txtLogView)).setMovementMethod(new ScrollingMovementMethod());

                ((ScrollView)findViewById(R.id.scrollView2)).fullScroll(View.FOCUS_DOWN);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Button buttonCopy = findViewById(R.id.btnCopy);
        buttonCopy.setOnClickListener(v -> {

            String videoUrl = ((VideoUrlAndQuality) (spinner.getSelectedItem())).getVideoUrl();
            String videoQuality = ((VideoUrlAndQuality) (spinner.getSelectedItem())).getQuality();

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("VideoUrl", videoUrl);
            clipboard.setPrimaryClip(clip);

            CharSequence msg = "Copiato l'url video per la qualità " + videoQuality;

            //Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            Snackbar.make(v, msg, LENGTH_LONG).show();

        });

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(READDETAILSFROMBUNDLE,videoDetailsParcelable);
        super.onSaveInstanceState(outState);
    }

    private void populateTree(VideoDetailsParcelable videoDetails) {
        if (videoDetails == null) {
            return;
        }
        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<VideoUrlAndQuality> arrayAdapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_list_item_1);

        for (VideoUrlAndQuality videoUrlAndQuality : videoDetails.getVideoUrlQualityList()) {
            arrayAdapter.add(videoUrlAndQuality);
        }

        spinner.setAdapter(arrayAdapter);

        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
        ((ProgressBar) findViewById(R.id.progressBarCircular)).setVisibility(View.INVISIBLE);
        findViewById(R.id.btnCopy).setEnabled(true);
        findViewById(R.id.btnStart).setEnabled(true);

        videoDetailsParcelable = videoDetails;
    }


    class ParserAsyncTask extends AsyncTask<URL, WorkerUpdateCallback.UpdateInfo, VideoDetailsParcelable> {
        final WorkerUpdateCallback workerUpdateCallback = (updateEvent, message) -> {
            //detailsTextArea.append(String.format("%s, - %s",updateEvent,message));
            publishProgress(new WorkerUpdateCallback.UpdateInfo(updateEvent, message));
        };

//        public ParserAsyncTask() {
//            this.workerUpdateCallback
//        }

        @Override
        protected VideoDetailsParcelable doInBackground(URL... urls) {

            String urlText = ((EditText) findViewById(R.id.editTextTextUrl)).getText().toString().trim();

            ParseVideoPage parseVideoPage = new ParseVideoPage(workerUpdateCallback);
            VideoDetails videoDetails = parseVideoPage.start(urlText);

            if (videoDetails == null) {
                workerUpdateCallback.update(UpdateEvent.Error, "Errore nel download / parsing");
                videoDetailsParcelable = null;
                return null;
            }

            videoDetailsParcelable = new VideoDetailsParcelable(videoDetails);

            List<VideoUrlAndQuality> videoMP4UrlList = videoDetails.getVideoUrlQualityList();

            workerUpdateCallback.update(UpdateEvent.VideoDownloadCanStart,
                    String.format("%s - %s", videoDetails.getTitle(), videoDetails.getMainUrl()));

            for (VideoUrlAndQuality videoUrlAndQuality : videoMP4UrlList) {
                workerUpdateCallback.update(UpdateEvent.VideoDownloadCanStart,
                        String.format("%s - %s",
                                videoUrlAndQuality.getQuality(),
                                videoUrlAndQuality.getVideoUrl()
                        ));
            }

            return videoDetailsParcelable;

        }

        @Override
        protected void onProgressUpdate(WorkerUpdateCallback.UpdateInfo... values) {
            int progressValue = 0;
            ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setMin(0);
            progressBar.setMax(4);

            super.onProgressUpdate(values);
            for (WorkerUpdateCallback.UpdateInfo value : values) {
                ((TextView) findViewById(R.id.txtLogView)).append(value.getMessage() + "\n");
                switch (value.getUpdateEvent()) {
                    case StartLoadingPage:
                        progressValue = 0;
                        break;
                    case FirstPageLoaded:
                        progressValue = 1;
                        break;
                    case SecondPageLoading:
                        progressValue = 2;
                        break;

                    case SecondPageLoaded:
                        progressValue = 3;
                        break;

                    case VideoDownloadCanStart:
                        progressValue = 4;
                        break;
                }
                progressBar.setProgress(progressValue);
            }

        }

        @Override
        protected void onPostExecute(VideoDetailsParcelable videoDetails) {
            super.onPostExecute(videoDetails);
            populateTree(videoDetails);
        }
    }
}
package com.aequinoxio.davverotvdownloader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import org.w3c.dom.Text;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLOutput;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG;

public class MainActivity extends AppCompatActivity {

    private static final String READDETAILSFROMBUNDLE = "READDETAILSFROMBUNDLE";
    private static final int SCHEDULE_PERIOD = 1000; // IN MILLIS
    private VideoDetailsParcelable videoDetailsParcelable;
    boolean spinnerInitialization = true;
    private long downloadManagerID;
    private int PERMISSION_REQ_CODE;
    private boolean PERMISSION_WRITE_GRANTED;
    private BroadcastReceiver downloadReceiver;
    private TimerTask timerTask;
    Timer timer = new Timer();

    //////////////////////////////////////////////////////////////////////////
    // Permission check
    // Thanx to: https://stackoverflow.com/a/40514787
    public boolean haveStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //Log.e("Permission error","You have permission");
                PERMISSION_WRITE_GRANTED = true;
                return PERMISSION_WRITE_GRANTED;
            } else {

                // Log.e("Permission error","You have asked for permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQ_CODE);
                PERMISSION_WRITE_GRANTED = false; // TODO: FORSE è un errore
                return PERMISSION_WRITE_GRANTED;

            }
        } else { //you dont need to worry about these stuff below api level 23
            // Log.e("Permission error","You already have the permission");
            PERMISSION_WRITE_GRANTED = true;
            return PERMISSION_WRITE_GRANTED;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PERMISSION_WRITE_GRANTED = true;
                } else {
                    PERMISSION_WRITE_GRANTED = false;
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        haveStoragePermission();

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
            findViewById(R.id.btnDownloadVideo).setEnabled(false);
            button.setEnabled(false);
            ((TextView) findViewById(R.id.txtLogView)).setText("");
        });

        Spinner spinner = findViewById(R.id.spinner);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerInitialization) {
                    spinnerInitialization = false;
                    return;
                }
                ((TextView) findViewById(R.id.txtLogView)).append(((VideoUrlAndQuality) parent.getSelectedItem()).getQuality() + "\n");
                ((TextView) findViewById(R.id.txtLogView)).append(((VideoUrlAndQuality) parent.getSelectedItem()).getVideoUrl() + "\n\n");

                //((TextView) findViewById(R.id.txtLogView)).setMovementMethod(new ScrollingMovementMethod());

                ((ScrollView) findViewById(R.id.scrollView2)).fullScroll(View.FOCUS_DOWN);
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

        Button buttonDownloadVideo = findViewById(R.id.btnDownloadVideo);
        buttonDownloadVideo.setOnClickListener(v -> {
            String videoUrl;
            String videoQuality;
            String videoTitle;

            if (!PERMISSION_WRITE_GRANTED) {
                return;
            }
            // DEBUG
            // Check se non sono null
//            if (!BuildConfig.DEBUG) {
                if (spinner.getSelectedItem() == null) {
                    return;
                }
                videoUrl = ((VideoUrlAndQuality) (spinner.getSelectedItem())).getVideoUrl();
                videoQuality = ((VideoUrlAndQuality) (spinner.getSelectedItem())).getQuality();
                videoTitle = videoDetailsParcelable.getTitle();
//            } else {
//                videoUrl = "https://open.tube/download/videos/013d5ceb-edf3-4cbc-9ff3-deca630ccf25-360.mp4";
//                videoQuality = "258";
//                videoTitle = "test";
//            }
//            System.out.println(videoUrl);
//            ////////

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(videoUrl));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDescription("Download quality: " + videoQuality);
            request.setTitle(videoTitle + ".mp4");
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    videoTitle + ".mp4");

            downloadManagerID = ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);

            // Feedback utente.
            // TODO: provare a mostrare una dialog con lo stato di avanzamento
            ((ProgressBar) findViewById(R.id.progressBarCircular)).setVisibility(View.VISIBLE);
            findViewById(R.id.btnDownloadVideo).setEnabled(false);

            ProgressBar progressBar =  findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setMin(0);
            progressBar.setMax(100);

            timer.cancel(); // Per sicurezza
            timer = new Timer();
            timerTask = new TimerTask() {
                final ProgressBar pb = findViewById(R.id.progressBar);
                final TextView textViewLog=findViewById(R.id.txtLogView);
                @Override
                public void run() {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadManagerID);
                    Cursor cursor = ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).query(query);

                    if (cursor.moveToFirst()) {
                        int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        double progresso = 100.0F * ((bytes_downloaded * 1.0F) / (double) bytes_total);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pb.setProgress((int) progresso);
                                textViewLog.append(String.format("Scaricati %d di %d bytes\n", bytes_downloaded, bytes_total));
                            }
                        });
                    }
                }
            };

            timer.schedule(timerTask,0,SCHEDULE_PERIOD);

        });

        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    return;
                }

                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0) != downloadManagerID) {
                    return;
                }

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadManagerID);
                Cursor cursor = ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).query(query);
                String messaggio = "";
                if (cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL:
                            messaggio="Download completato\n";
                            break;
                        case DownloadManager.STATUS_FAILED:
                            messaggio = "Download in errore\n";

                            break;
                        default:
                            messaggio = "Non so cosa sia accaduto\n";
                            //((TextView)findViewById(R.id.txtLogView)).append("Download in errore\n");
                    }

                    timer.cancel();
                } else {
                    messaggio = "Assumo che il download sia stato cancellato\n";
                }
                String finalMessaggio = messaggio;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) findViewById(R.id.txtLogView)).append(finalMessaggio);
                        ((ProgressBar) findViewById(R.id.progressBarCircular)).setVisibility(View.INVISIBLE);
                        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
                        findViewById(R.id.btnDownloadVideo).setEnabled(true);
                    }
                });
            }
        };

        registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(READDETAILSFROMBUNDLE, videoDetailsParcelable);
        super.onSaveInstanceState(outState);
        unregisterReceiver(downloadReceiver);
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
        findViewById(R.id.btnDownloadVideo).setEnabled(true);
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
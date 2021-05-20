package com.aequinoxio.davverotvdownloader;

import androidx.annotation.NonNull;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
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
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG;

public class MainActivity extends AppCompatActivity {

    private static final String READDETAILSFROMBUNDLE = "READDETAILSFROMBUNDLE";
    private static final int SCHEDULE_PERIOD = 500; // IN MILLIS
    private VideoDetailsParcelable videoDetailsParcelable;
    boolean spinnerInitialization = true;
    private long downloadManagerID;                 // Handle del servizio di sistema per il download via HTTP
    private final int PERMISSION_REQ_CODE = 12987; // Valore casuale per l'applicazione
    private boolean PERMISSION_WRITE_GRANTED;
    private BroadcastReceiver downloadReceiver;
    private TimerTask timerTask;
    Timer timer = new Timer();

    //////////////////////////////////////////////////////////////////////////
    // Permission check
    // Thanx to: https://stackoverflow.com/a/40514787
    public boolean haveStoragePermission() {
        if (Build.VERSION.SDK_INT >= 29) { // Con Android Q non serve in quanto uso il Download manager integrato di Android
            PERMISSION_WRITE_GRANTED=true;

        } else {

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //Log.e("Permission error","You have permission");
                PERMISSION_WRITE_GRANTED = true;

            } else {

                // Log.e("Permission error","You have asked for permission");
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, PERMISSION_REQ_CODE);
                PERMISSION_WRITE_GRANTED = false; // TODO: FORSE è un errore

            }
        }
        return PERMISSION_WRITE_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0) {
                PERMISSION_WRITE_GRANTED = grantResults[0] == PackageManager.PERMISSION_GRANTED;
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
        //((EditText) findViewById(R.id.editTextTextUrl)).setText("https://www.davvero.tv/tgbyoblu24/videos/tg-byoblu24-26-febbraio-2021-edizione-19-00");
        ////

        Button button = findViewById(R.id.btnStart);

        Button finalButton = button;
        button.setOnClickListener(v -> {

            String urlText = ((EditText)findViewById(R.id.editTextTextUrl)).getText().toString();

            if (!httpUrlIsValid(urlText)){
                Snackbar.make(this,v,"Immettere un'url valida", LENGTH_LONG).show();
                return;
            }

            ParserAsyncTask parserAsyncTask = new ParserAsyncTask();
            parserAsyncTask.execute();
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            findViewById(R.id.progressBarCircular).setVisibility(View.VISIBLE);
            findViewById(R.id.btnCopy).setEnabled(false);
            findViewById(R.id.btnDownloadVideo).setEnabled(false);


            finalButton.setEnabled(false);
            findViewById(R.id.btnEsempio).setEnabled(false);

            ((TextView) findViewById(R.id.txtLogView)).setText("");
        });

        button = findViewById(R.id.btnEsempio);
        button.setOnClickListener(v -> {
            ((EditText)findViewById(R.id.editTextTextUrl)).setText(getString(R.string.UrlEsempio));
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

        // Check url durante la digitazione
        EditText editText = findViewById(R.id.editTextTextUrl);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length()>0 && httpUrlIsValid(s.toString())){
                    findViewById(R.id.btnStart).setEnabled(true);
                } else{
                    findViewById(R.id.btnStart).setEnabled(false);
                }
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

//            if (!PERMISSION_WRITE_GRANTED) {
//                return;
//            }

            if(!haveStoragePermission()){
                Snackbar.make(v,"Permessi non assegnati", LENGTH_LONG).show();
                return;
            }

            // Check se non sono null
            if (spinner.getSelectedItem() == null) {
                return;
            }

            videoUrl = ((VideoUrlAndQuality) (spinner.getSelectedItem())).getVideoUrl();
            videoQuality = ((VideoUrlAndQuality) (spinner.getSelectedItem())).getQuality();
            videoTitle = videoDetailsParcelable.getTitle();

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
            findViewById(R.id.progressBarCircular).setVisibility(View.VISIBLE);
            findViewById(R.id.btnDownloadVideo).setEnabled(false);

            ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setMin(0);
            progressBar.setMax(100);

            // Scrivo il file di ausilio .title con le informazioni sull'origine del video
            String titleFileAbsPath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+
                    "/"+videoTitle+".title";
            ((TextView) findViewById(R.id.txtLogView)).append("Salvo il file in: "+titleFileAbsPath+"\n");

            // TODO: Sostituire con Scoped Storage
//            try(BufferedWriter bw = new BufferedWriter(new FileWriter(titleFileAbsPath))
//            ){
//                bw.write("Titolo:");bw.write(videoTitle);bw.newLine();
//                bw.write("Url:");bw.write(videoDetailsParcelable.getMainUrl());bw.newLine();
//
//                // DEBUG
//                //Snackbar.make(v,titleFileAbsPath, BaseTransientBottomBar.LENGTH_INDEFINITE).show();
//
//                /////////
//            } catch (IOException e) {
//                ((TextView) findViewById(R.id.txtLogView)).append("*** Errore "+
//                e.toString()+" ***\n");
//                e.printStackTrace();
//                //Snackbar.make(v,"*** ERRORE Creazione file .title ***\n"+e.toString(), LENGTH_LONG).show();
//            }

            // Avvio il download
            timer.cancel(); // Per sicurezza
            timer = new Timer();
            timerTask = new TimerTask() {
                long latestPrintedDownload = 0;
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

                        if (bytes_total<=0){
                            pb.setIndeterminate(true);
                        } else {
                            pb.setIndeterminate(false);
                            // Aggiorno solo se i bytes scaricati sono aumentati, evito il doppio aggiornamento
                            if (latestPrintedDownload != bytes_downloaded) {
                                latestPrintedDownload = bytes_downloaded;
                                runOnUiThread(() -> {
                                    pb.setProgress((int) progresso);
                                    textViewLog.append(String.format(Locale.getDefault(),"Scaricati %d di %d bytes - %d %%\n", bytes_downloaded, bytes_total, (int) progresso));
                                });
                            }
                        }
                    }
                }
            };

            timer.schedule(timerTask,0,SCHEDULE_PERIOD);

        });

        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

              //  System.out.println("*** action:"+action);
                if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    return;
                }

                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0) != downloadManagerID) {
                    return;
                }

              //  System.out.println("*** action 2:"+action);

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

                } else {
                    messaggio = "Assumo che il download sia stato cancellato\n";
                }
                timer.cancel();
                String finalMessaggio = messaggio;
                runOnUiThread(() -> {
                 //   System.out.println("*** on runuithread:");
                    ((TextView) findViewById(R.id.txtLogView)).append(finalMessaggio);
                    findViewById(R.id.progressBarCircular).setVisibility(View.INVISIBLE);
                    findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                    findViewById(R.id.btnDownloadVideo).setEnabled(true);
                });
            }
        };

        registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    }

    // See: https://www.oracle.com/webfolder/technetwork/tutorials/obe/java/httpThreadLink/HttpClient.html
    // TODO: migliorare la regexp
    private boolean httpUrlIsValid(String urlText) {
        String urlRegex = "^(http|https)://[-a-zA-Z0-9+&@#/%?=~_|,!:.;]*[-a-zA-Z0-9+@#/%=&_|]";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher m = pattern.matcher(urlText);
        if (m.matches()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(READDETAILSFROMBUNDLE, videoDetailsParcelable);
        super.onSaveInstanceState(outState);
        unregisterReceiver(downloadReceiver);
    }

    private void populateTree(VideoDetailsParcelable videoDetails) {
        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<VideoUrlAndQuality> arrayAdapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_list_item_1);

        if (videoDetails == null) {
            resetUI();
            spinner.setAdapter(arrayAdapter); // Azzero il selettore della qualità
            return;
        }

        // Aggiorno il selettore della qualità
        for (VideoUrlAndQuality videoUrlAndQuality : videoDetails.getVideoUrlQualityList()) {
            arrayAdapter.add(videoUrlAndQuality);
        }

        spinner.setAdapter(arrayAdapter);

        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.progressBarCircular).setVisibility(View.INVISIBLE);
        findViewById(R.id.btnCopy).setEnabled(true);
        findViewById(R.id.btnDownloadVideo).setEnabled(true);
        findViewById(R.id.btnStart).setEnabled(true);
        findViewById(R.id.btnEsempio).setEnabled(true);

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

    private void resetUI() {
        findViewById(R.id.progressBarCircular).setVisibility(View.INVISIBLE);
        findViewById(R.id.btnCopy).setEnabled(false);
        findViewById(R.id.btnDownloadVideo).setEnabled(false);

        findViewById(R.id.btnStart).setEnabled(true);
        findViewById(R.id.btnEsempio).setEnabled(true);

    }
}
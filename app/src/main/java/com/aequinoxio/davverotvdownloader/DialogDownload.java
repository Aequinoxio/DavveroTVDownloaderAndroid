package com.aequinoxio.davverotvdownloader;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.loader.content.AsyncTaskLoader;

import com.aequinoxio.davverotvdownloader.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

class DialogDownload extends Dialog {

    URL downloadUrl;
    File saveFile;
    private AsyncDownload asyncDownload;

    public DialogDownload(@NonNull Context context, String fileName, String downloadUrl) throws MalformedURLException {
        super(context);
        this.downloadUrl=new URL(downloadUrl);
        this.saveFile=new File(fileName);
    }
/*


//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            ViewGroup viewGroup = v.findViewById(android.R.id.content);
//            View dialogView = LayoutInflater.from(v.getContext()).inflate(R.layout.dialog_download,viewGroup,false);
//            builder.setView(dialogView);
//            final AlertDialog alertDialog = builder.create();
//            //alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//            alertDialog.show();

 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_download);

//        // Allargo le dimensioni fino alla massima larghezza (sembra che sia indispensabile per visualizzare correttamente la dialog)
//        Window window = getWindow();
//        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Button buttonStart = findViewById(R.id.btnStartDownloadFile);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                asyncDownload = new AsyncDownload(saveFile,downloadUrl);
                asyncDownload.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        asyncDownload.cancel(true);
    }

    public static class AsyncDownload extends AsyncTask<URL,Long,Long> {

        private long downloadFileSize=-1;
        private final URL urlVideo;
        private final File saveFile;

        public AsyncDownload(File saveFile,URL urlVideo) {
            this.urlVideo = urlVideo;
            this.saveFile = saveFile;
        }

        @Override
        protected Long doInBackground(URL... urls) {
            long res = 0;
           // updateMessage("Inizio il download");

            String filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            // TODO: Recuperare il nome del file dall'oggetto che dovrà essere passato
            File file = new File(filepath+File.separator+saveFile.getAbsolutePath());
            if (!file.exists()) {
                try {
                    if (!file.createNewFile()){
                        // TODO: Gestire l'errore di creazione file
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                HttpURLConnection httpConnection = (HttpURLConnection) urlVideo.openConnection();
                httpConnection.setRequestMethod("HEAD");
                downloadFileSize = httpConnection.getContentLengthLong();
                publishProgress(downloadFileSize);
            } catch (IOException e) {
                e.printStackTrace();
            }

           // PER DEBUG LO DISABILITO res = scaricaVideo();

            //updateMessage("Finito il download");

            return res;

        }

        public long scaricaVideo() {
            long bytesWrited = 0;
            final int MAXBUFF = 10240;
            try (BufferedInputStream in = new BufferedInputStream(urlVideo.openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(saveFile)) {

                byte[] dataBuffer = new byte[MAXBUFF];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, MAXBUFF)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                    bytesWrited += bytesRead;
                    publishProgress( (long) ((bytesWrited * 100.0F) / downloadFileSize));
//                    updateProgress(bytesWrited, downloadFileSize);
//                    updateMessage(String.format("%3d%%", (int) ((bytesWrited * 100.0F) / downloadFileSize)));
                    //System.out.println("Letti: " + bytesRead + " - Scritti totali: " + bytesWrited);
                    if (isCancelled()){
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                // handle exception
            }
            return bytesWrited;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
        }
    }
}

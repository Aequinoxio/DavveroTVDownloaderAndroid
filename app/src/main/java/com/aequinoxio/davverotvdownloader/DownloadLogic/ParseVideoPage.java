package com.aequinoxio.davverotvdownloader.DownloadLogic;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.json.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ParseVideoPage {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/86.0" ;

    final WorkerUpdateCallback workerUpdateCallback;
    private static String Indent="\t\t\t";

    public ParseVideoPage(WorkerUpdateCallback workerUpdateCallback) {
        this.workerUpdateCallback = workerUpdateCallback;
    }

    public VideoDetails start (String urlText){
        if(workerUpdateCallback!=null){
            workerUpdateCallback.update(UpdateEvent.StartLoadingPage,"Start Loading Page");
        }

        String jsonPage = loadPage(urlText);

        if (jsonPage!=null) {
            if(workerUpdateCallback!=null){
                workerUpdateCallback.update(UpdateEvent.FirstPageLoaded, Indent+"First Page Loaded");
            }
        }else {
            if(workerUpdateCallback!=null){
                workerUpdateCallback.update(UpdateEvent.Error, "*** Error loading first page ***");
            }
            return null;
        }

        VideoDetails videoDetails =  parsePage(jsonPage);

        if (videoDetails!=null) {
            if(workerUpdateCallback!=null){
                workerUpdateCallback.update(UpdateEvent.VideoDownloadCanStart, "\nVideo Download Can Start\n");
            }
        } else {
            if(workerUpdateCallback!=null) {
                workerUpdateCallback.update(UpdateEvent.Error, "*** Error parsing page finding video url ***");
            }
            return null;
        }

        return videoDetails;
    }

    private VideoDetails parsePage(String jsonPage) {

        VideoDetails tempDetails;

        JsonReader jsonReader = Json.createReader(new StringReader(jsonPage));
        JsonObject joMain = jsonReader.readObject();
        jsonReader.close();

        String title = joMain.getJsonObject("video").getString("title");
        String mainUrl = joMain.getJsonObject("video").getString("url");

        tempDetails = new VideoDetails(title,mainUrl);

        String secondPageUrl = joMain.getString("config_url").replaceAll("\u0026","&");

        try {
            if(workerUpdateCallback!=null){
                workerUpdateCallback.update(UpdateEvent.SecondPageLoading,Indent+"Second Page is Loading");
            }

            String secondPageContent = Jsoup.connect(secondPageUrl)
                    .userAgent(USER_AGENT)
                    .ignoreContentType(true).get().text();

            if(workerUpdateCallback!=null){
                workerUpdateCallback.update(UpdateEvent.SecondPageLoaded,Indent+"Second Page Loaded");
            }

            JsonReader jsonReader1 = Json.createReader(new StringReader(secondPageContent));
            JsonObject joMain1 = jsonReader1.readObject();
            jsonReader1.close();

            // PARSING per recuperare l'url del video
            // Path: ROOT.request.files.progressive
            JsonArray progressiveArray = joMain1.getJsonObject("request").getJsonObject("files").getJsonArray("progressive");

            String quality;
            String videoUrl;

            List<VideoUrlAndQuality> tempVideoUrlAndQualityList = new ArrayList<>();

            for(JsonValue arrayItem : progressiveArray){
                quality=arrayItem.asJsonObject().getString("quality");
                videoUrl=arrayItem.asJsonObject().getString("url");

                //tempDetails.addVideoUrlQuality(quality,videoUrl);
                tempVideoUrlAndQualityList.add(new VideoUrlAndQuality(quality,videoUrl));
            }

            tempVideoUrlAndQualityList.sort((o1, o2) -> {
                int v1 = Integer.parseInt(o1.getQuality().replace("p", ""));
                int v2 = Integer.parseInt(o2.getQuality().replace("p", ""));
                boolean retVal = v1 < v2;
                return retVal ? -1 : 1;
                //o1.getQuality().compareToIgnoreCase(o2.getQuality());
            });

            for (VideoUrlAndQuality videoUrlAndQualityTemp : tempVideoUrlAndQualityList){
                tempDetails.addVideoUrlQuality(videoUrlAndQualityTemp);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        return tempDetails;
    }

    private String loadPage(String urlText) {
        String retVal=null;
        try {
            // Check per protocollo HTTP o HTTPS
            URL tempUrl = new URL(urlText);
            if (!(tempUrl.getProtocol().equalsIgnoreCase("HTTP") || tempUrl.getProtocol().equalsIgnoreCase("HTTPS"))){
                return null;
            }

            Document pageDocument = Jsoup.connect(urlText).userAgent(USER_AGENT).get();

            // Seleziona l'iframe con lo script contenente l'url e lo carica
            // Se c'è un errore viene trattato come se la pagina non si caricasse
            String iframeSrc = pageDocument.select("iframe").first().attr("src");
            Document iframeDocument = Jsoup.connect(iframeSrc).userAgent(USER_AGENT).get();

            // Parsing per recuperare il json contenente le varie url dei video
            Elements scriptElements = iframeDocument.getElementsByTag("script");
            for(Element scriptElement: scriptElements){
                String innerHTML =scriptElement.html() ;

                // TODO: Punto di modifica in caso variasse lo schema della pagina
                if (innerHTML.contains("window.OTTData")) {
                    // Prendo il primo che trovo -> non ce ne dovrebbero essere altri
                    retVal = innerHTML.split("=",2)[1].trim();
                    break;
                }
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return retVal;
    }

}

package org.wordgap;

import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 */
public class ServerCommunicator {

    private static final String TAG = "wordgap - ServerCommunicator";
    private String ipaddress;
    private static final String port = "8000";
    //Emulator
    //public static final String IPADDRESS_EM = "http://10.0.0.2/";
    private static String urlPost;
    // Emulator
    //public static final String URL_POST_EM = IPADDRESS_EM + "post/";

    private static String urlWordlist;
    private static byte[] sBuffer = new byte[512];

    public ServerCommunicator(String ipaddress) {

        this.ipaddress = "http://" + ipaddress;
        urlPost = ipaddress + "/post/";
        urlWordlist = ipaddress + "/wordlist/";
    }

    /**
     * Beispiel für JSON String:
     * [["Chapter 8 I soon learned to", "this flower better. On the little prince's planet the flowers had always been very simple.", "know", ["move", "live", "rumple"]], ...
     *
     * @param json
     * @return Exercise als Liste von Sent Objekten (Sätze)
     */
    public ArrayList<Sent> parseJSONEx(String json) {

        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(json);
        }
        catch(JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "JSON Parse error bei:" + json);
        }
        ArrayList<Sent> ex = new ArrayList<Sent>();
        int noSents = jsonArray.length();
        for(int i = 0; i < noSents; i++) {
            try {
                JSONArray jsonSent = jsonArray.getJSONArray(i);
                Sent sent = new Sent();
                sent.wordsbefore = jsonSent.getString(0);
                sent.wordsafter = jsonSent.getString(1);
                sent.token = jsonSent.getString(2);
                JSONArray jsonDis = (JSONArray) jsonSent.get(3);
                ArrayList<String> dis = new ArrayList<String>();
                dis.add(jsonDis.getString(0));
                dis.add(jsonDis.getString(1));
                dis.add(jsonDis.getString(2));
                sent.dis = dis;
                //Log.i(TAG, "neuer Satz: " + sent.toString());
                ex.add(sent);
            }
            catch(JSONException e) {
                Log.e(TAG, "JSON Parse error bei Satz: " + i);
                e.printStackTrace();
            }
        }
        Log.i(TAG, "geparst wurden " + ex.size() + " Sätze!");
        return ex;
    }

    public ArrayList<String> parseJSONWordlist(String json) {
        //[["appear", "give a certain impression or have a certain outward aspect; \"She seems to be sleeping\"; \"This appears to be a very difficult problem\"; \"This project looks fishy\"; \"They appeared like people who had not eaten or slept for a long time\""]
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(json);
        }
        catch(JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "JSON Parse error bei:" + json);
        }
        ArrayList<String> wordlist = new ArrayList<String>();
        for(int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONArray jsonEntry = jsonArray.getJSONArray(i);
                String lemma = jsonEntry.getString(0);
                String definition = jsonEntry.getString(1);
                wordlist.add(lemma + "\n\n" + definition);
            }
            catch(JSONException e) {
                Log.e(TAG, "JSON Parse error bei Nr: " + i);
                e.printStackTrace();
            }
        }
        Log.i(TAG, "geparst wurden " + wordlist.size() + " Definitionen!");
        ;
        return wordlist;
    }

    public String getJSONWordlist(ArrayList<String> markedWordsList, String pos) throws IOException {

        urlWordlist = this.ipaddress + "/wordlist/";
        HttpPost httpPost = new HttpPost(urlWordlist);
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < markedWordsList.size(); i++) {
            builder.append(markedWordsList.get(i) + "\n\n");
        }
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("wordlist", builder.toString()));
        nameValuePairs.add(new BasicNameValuePair("pos", pos));
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        Log.i(TAG, "httpPost: " + httpPost.toString());
        String serverResponse = "";
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 2000);
        // waiting for data could take a long time when the text is very long (limit 30 seconds)
        HttpConnectionParams.setSoTimeout(params, 30000);
        HttpClient client = new DefaultHttpClient(params);
        builder = new StringBuilder();
        HttpResponse response = client.execute(httpPost);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        Log.i(TAG, "statusCode: " + statusCode);
        if(statusCode == HttpURLConnection.HTTP_OK) {
            HttpEntity entity = response.getEntity();
            InputStream content = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line);
            }
            serverResponse = builder.toString();
            //Log.i(TAG, "serverResponse: " + serverResponse);
        }
        else {
            throw new IOException("Invalid response from server: " + statusCode);
        }
        return serverResponse;
    }

    /**
     * Receive exercise as JSON
     *
     * @param text text that the user wants to be transformed into a multiple-choice cloze exercise
     * @param pos  requested part-of-speech tag, i.e. nouns, verbs, adjectives or prepositions
     * @return
     * @throws IOException
     * @throws ServerErrorException
     */

    public String getJSONEx(String text, String pos) throws IOException {

        urlPost = this.ipaddress + "/post/";
        HttpPost httpPost = new HttpPost(urlPost);
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
        nameValuePairs.add(new BasicNameValuePair("text", text));
        nameValuePairs.add(new BasicNameValuePair("pos", pos));
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        String serverResponse = "";
        StringBuilder builder = new StringBuilder();
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 2000);
        // waiting for data could take a long time when the text is very long
        HttpConnectionParams.setSoTimeout(params, 30000);
        HttpClient client = new DefaultHttpClient(params);
        HttpResponse response = client.execute(httpPost);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        Log.i(TAG, "statusCode: " + statusCode);
        if(statusCode == HttpURLConnection.HTTP_OK) {
            HttpEntity entity = response.getEntity();
            InputStream content = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line);
            }
            serverResponse = builder.toString();
            Log.i(TAG, "serverResponse: " + serverResponse);
        }
        else {
            throw new IOException("Invalid response from server: " + statusCode);
        }
        return serverResponse;
    }
    // wird im Moment nicht gebraucht, ist implementiert worden, bevor die WebExAct.
    // JSoup zum Laden der URL nutzte

    public String getRawHTML(String url) throws IOException, ServerErrorException {

        HttpGet request = new HttpGet(url);
        HttpClient client = new DefaultHttpClient();
        try {
            HttpResponse response = client.execute(request);
            StatusLine status = response.getStatusLine();
            if(status.getStatusCode() != 200) {
                throw new ServerErrorException("Invalid response from server: " + status.toString());
            }
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            int readBytes = 0;
            while((readBytes = inputStream.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }
            return new String(content.toByteArray());
        }
        catch(IOException e) {
            throw new ServerErrorException("Problem communicating with Server");
        }
    }

    public class ServerErrorException extends Exception {

        public ServerErrorException(String s) {

        }
    }
}

package org.wordgap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;

/**
 * @author: Susanne Knoop
 * <p/>
 * This activity will be initiated when the user clicks "Share webpage" -> "WordGap" in a browser.
 * Loads the URL content, parses the HTML with JSoup and creates a new exercise from the website's text.
 * 
 */
public class WebExActivity extends Activity {
    private static final String TAG = "wordgap - WebExActivity";
    private String address;
    private String subject;
    private ProgressDialog progress;
    private WordgapApplication app;
    private LoadPageTask task;
    private String text;
    private Activity thisActivity;
    ServerCommunicator sc;
    private static final int DIALOG_POS = 0;
    private String pos;
    private static final int DIALOG_NO_NETWORK = 1;
    private static final int DIALOG_NO_TEXT = 2;
    private static final int DIALOG_NO_SERVER = 3;
    private IOException io;
    private IllegalArgumentException iae;
    private String title;
    private static final int DIALOG_NO_VALID_URL = 4;
    private SharedPreferences prefs;
    private String ipaddress;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // get URL and title of the webpage that was submitted with the Intent
        Intent intent = getIntent();
        app = (WordgapApplication) getApplication();
        task = new LoadPageTask();
        prefs = getSharedPreferences(getString(R.string.wordgap), MODE_PRIVATE);
        ipaddress = prefs.getString(getString(R.string.ip), getString(R.string.default_ip));
        this.sc = new ServerCommunicator(ipaddress);
        if(savedInstanceState == null && intent != null) {
            if(intent.getAction().equals(Intent.ACTION_SEND)) {
                // Address of the URL
                this.address = intent.getStringExtra(Intent.EXTRA_TEXT);
                Log.i(TAG, "address " + address);
                // title of the URL
                this.subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                Log.i(TAG, "subject  " + subject);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);
        progress = new ProgressDialog(this);
        thisActivity = this;
        Matcher matcher = Patterns.WEB_URL.matcher(address);
        if(matcher.find()) {
            showDialog(DIALOG_POS);
        }
        else {
            showDialog(DIALOG_NO_VALID_URL);
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        Log.i(TAG, "onPause");
        // verhindert Absturz, wenn das Handy gedreht wird
        // prevents crashing when phone is turned 
        if(progress.isShowing()) {
            progress.dismiss();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.error));
        switch(id) {
            case DIALOG_POS:
                builder.setTitle(getString(R.string.choose_pos));
                String[] names = new String[4];
                names[0] = getString(R.string.verbs);
                names[1] = getString(R.string.nouns);
                names[2] = getString(R.string.adjectives);
                names[3] = getString(R.string.prepositions);

                //builder.setCancelable(true);
                // will crash otherwise
                builder.setCancelable(false);
                builder.setItems(names, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        Log.i(TAG, "chosen POS: " + which);
                        if(which == 0) {
                            pos = "v";
                        }
                        else if(which == 1) {
                            pos = "a";
                        }
                        else if(which == 2) {
                            pos = "n";
                        }
                        else {
                            pos = "p";
                        }
                        dialog.dismiss();
                        if(!isOnline()) {
                            showDialog(DIALOG_NO_NETWORK);
                        }
                        // wenn Netzwerkzugang vorhanden:
                        else {
                            task.execute();
                        }
                    }
                });
                return builder.show();
            case DIALOG_NO_NETWORK:
                builder.setMessage(getString(R.string.network_deactivated));
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        moveTaskToBack(true);
                    }
                });
                return builder.show();
            case DIALOG_NO_TEXT:
                builder.setMessage(R.string.error_text);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        moveTaskToBack(true);
                    }
                });
                return builder.show();
            case DIALOG_NO_SERVER:
                builder.setMessage(R.string.error_server);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        moveTaskToBack(true);
                    }
                });
                return builder.show();

            case DIALOG_NO_VALID_URL:
                builder.setMessage(R.string.error_url);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        moveTaskToBack(true);

                    }
                });
                return builder.show();
        }
        return null;
    }

    // geklaut von Stackoverflow
    public boolean isOnline() {

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if(netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    protected class LoadPageTask extends AsyncTask<String, Void, ArrayList<Sent>> {

        @Override
        protected void onPreExecute() {

            progress.setIndeterminate(true);
            progress.setTitle(getString(R.string.loading_webpage));
            progress.setMessage(getString(R.string.connecting_to_server));
            progress.show();
            Log.i(TAG, "show Progress Bar");
        }

        @Override
        protected ArrayList<Sent> doInBackground(String... params) {
            // Adresse aufrufen, html laden
            Document doc = null;
            try {
                doc = Jsoup.connect(address).get();
            }
            catch(IllegalArgumentException e){
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                iae = e;
                return  null;
            }
            catch(IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                io = e;
                return null;
            }
            String serverResponse = null;
            // html parsen
            text = doc.body().text();
            title = subject;
            app.setTitle(subject);
            app.setText(text);
            ArrayList<Sent> ex = new ArrayList<Sent>();
            try {
                serverResponse = sc.getJSONEx(text, pos);
            }
            catch(IOException e) {
                io = e;
                e.printStackTrace();
                return null;
            }
            ex = sc.parseJSONEx(serverResponse);
            String filename = title + "_" + pos + ".json";
            FileOutputStream fos = null;
            try {
                fos = openFileOutput(filename, Context.MODE_PRIVATE);
            }
            catch(FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                fos.write(serverResponse.getBytes());
                Log.i(TAG, "File " + filename + " saved in internal memory");
            }
            catch(IOException e) {
                Log.e(TAG, "Could not save exercise.");
                e.printStackTrace();
            }
            finally {
                try {
                    fos.close();
                }
                catch(IOException e) {
                    Log.e(TAG, "Could not save exercise.");
                    e.printStackTrace();
                }
            }
            return ex;
        }

        @Override
        protected void onPostExecute(ArrayList<Sent> ex) {

            progress.dismiss();
            if(io == null && ex != null) {
                app.setEx(ex);
                app.setPos(pos);
                Intent i = new Intent(thisActivity, GameActivity.class);
                startActivity(i);
            }

            else if (iae != null){
                showDialog(DIALOG_NO_VALID_URL);
            }
            else if (io != null){
                showDialog(DIALOG_NO_SERVER);
            }

        }
    }
}
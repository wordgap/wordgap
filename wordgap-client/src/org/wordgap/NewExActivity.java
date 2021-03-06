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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;



/**
 * @author: Susanne Knoop
 */
public class NewExActivity extends Activity {
    private static final String TAG = "wordgap - NewExActivity";
    private static Activity thisActivity = null;
    private ProgressDialog progress;
    private LoadExTask task;
    private ServerCommunicator sc;
    private IOException io;
    private String title;
    private String text;
    private String pos;
    private WordgapApplication app;
    private static final int DIALOG_POS = 0;
    private static final int DIALOG_NO_NETWORK = 1;
    private static final int DIALOG_NO_TEXT = 2;
    private static final int DIALOG_NO_SERVER = 3;
    private SharedPreferences prefs;
    private String ipaddress;

    private ArrayList<Sent> ex;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        app = (WordgapApplication) getApplication();
        task = new LoadExTask();
        progress = new ProgressDialog(this);
        prefs = getSharedPreferences(getString(R.string.wordgap), MODE_PRIVATE);
        ipaddress = prefs.getString(getString(R.string.ip), getString(R.string.default_ip));
        this.sc = new ServerCommunicator(ipaddress);
        sc = new ServerCommunicator(ipaddress);
        title = app.getTitle();
        text = app.getText();
        // Log gibt nur die ersten Sätze aus
        //System.out.println(text);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);
        thisActivity = this;
        if(text.split(" ").length < 300) {
            showDialog(DIALOG_NO_TEXT);
        }
        showDialog(DIALOG_POS);
    }

    // von Stackoverflow
    public boolean isOnline() {

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if(netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    @Override
    protected void onPause() {

        super.onPause();
        Log.i(TAG, "onPause");
        // verhindert Absturz, wenn das Handy gedreht wird
        if(progress.isShowing()) {
            progress.dismiss();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch(id) {
            case DIALOG_POS:
                builder.setTitle(R.string.choose_pos);
                String[] names = new String[4];
                names[0] = getString(R.string.verbs);
                names[1] = getString(R.string.nouns);
                names[2] = getString(R.string.adjectives);
                names[3] = getString(R.string.prepositions);
                // nur zu Testzwecken!
                builder.setCancelable(true);
                // builder.setCancelable(false);
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
                        if(text != "") {
                            if(!isOnline()) {
                                showDialog(DIALOG_NO_NETWORK);
                            }
                            // wenn Netzwerkzugang vorhanden:
                            else {
                                task.execute();
                            }
                        }
                        else {
                            showDialog(DIALOG_NO_TEXT);
                        }
                    }
                });
                return builder.show();
            case DIALOG_NO_NETWORK:
                builder.setTitle(R.string.error);
                builder.setMessage(R.string.network_deactivated);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent i = new Intent(thisActivity, WordgapMainMenuActivity.class);
                        startActivity(i);
                    }
                });
                return builder.show();
            case DIALOG_NO_TEXT:
                builder.setTitle(R.string.error);
                builder.setMessage(R.string.error_text);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent i = new Intent(thisActivity, WordgapMainMenuActivity.class);
                        startActivity(i);
                    }
                });
                return builder.show();
            case DIALOG_NO_SERVER:
                builder.setTitle(R.string.error);
                builder.setMessage(R.string.error_server);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent i = new Intent(thisActivity, WordgapMainMenuActivity.class);
                        startActivity(i);
                    }
                });
                return  builder.show();
        }
        return null;
    }

    protected class LoadExTask extends AsyncTask<String, Void, ArrayList<Sent>> {

        @Override
        protected void onPreExecute() {

            progress.setIndeterminate(true);
            progress.setTitle(R.string.creating_ex);
            progress.setMessage(getString(R.string.connecting_to_server));
            progress.setCancelable(true);
            progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {

                    dialog.dismiss();
                    Intent i = new Intent(thisActivity, WordgapMainMenuActivity.class);
                    startActivity(i);
                }
            });
            progress.show();
            Log.i(TAG, "show Progress Bar");
        }

        @Override
        protected ArrayList<Sent> doInBackground(String... params) {

            String serverResponse = null;
            try {
                serverResponse = sc.getJSONEx(text, pos);
            }
            catch(IOException e) {
                io = e;
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                return null;
            }
            ArrayList<Sent> ex;
            ex = sc.parseJSONEx(serverResponse);
            String filename = title + "_" + pos + ".json";
            FileOutputStream fos = null;
            try {
                fos = openFileOutput(filename, Context.MODE_APPEND);
            }
            catch(FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                fos.write(serverResponse.getBytes());
                Log.i(TAG, "File " + filename + " saved in internal memory.");
            }
            catch(IOException e) {
                Log.e(TAG, "Could not save file!");
                e.printStackTrace();
            }
            finally {
                try {
                    fos.close();
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }
            return ex;
        }

        @Override
        protected void onPostExecute(ArrayList<Sent> ex) {

            progress.dismiss();
            if(io == null) {
                app.setText(text);
                app.setTitle(title);
                app.setPos(pos);
                app.setEx(ex);
                Intent i = new Intent(thisActivity, GameActivity.class);
                startActivity(i);
            }
            else {
                showDialog(DIALOG_NO_SERVER);
            }
        }
    }
}
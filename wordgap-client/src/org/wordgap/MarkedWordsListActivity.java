package org.wordgap;

import android.app.*;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;



/**
 * @author: Susanne Knoop
 * <p/>
 * Zeigt direkt nach einer Übung die in dieser Übung zu "Gemerkte Wörter" hinzugefügten
 * Vokabeln an und bei Klick deren WordNet Definition, die zuvor vom Server geholt wird
 */
public class MarkedWordsListActivity extends ListActivity {
    private ServerCommunicator sc;
    private WordgapApplication app;
    private ArrayAdapter<String> adapter;
    private HashSet<String> markedWords = new HashSet<String>();
    private ArrayList<String> markedWordsList = new ArrayList<String>();
    private ArrayList<String> definitions = new ArrayList<String>();
    private String pos;
    private ProgressDialog progress;
    private Activity thisActivity;
    private LoadWordsTask task;
    String currentDefinition;
    IOException io;
    private static final String TAG = "wordgap - MarkedWordsListActivity";
    private static final int DIALOG_DEFINITION = 0;
    private String wordsfile = getString(R.string.wordslist_filename);
    private static final int DIALOG_NO_SERVER = 1;
    private static final int DIALOG_NO_MARKED_WORDS = 2;
    private SharedPreferences prefs;
    private String ipaddress;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(getString(R.string.wordgap), MODE_PRIVATE);
        ipaddress = prefs.getString(getString(R.string.ip), getString( R.string.default_ip));
        this.sc = new ServerCommunicator(ipaddress);
        this.app = (WordgapApplication) getApplication();
        markedWords = app.getMarkedWords();
        pos = app.getPos();
        Log.i(TAG, "Starting " + TAG);
        Log.i(TAG, markedWords.size() + " words added to word list.");
        Log.i(TAG, "pos: " + pos);
//        for(int i = 0; i < markedWords.size(); i++) {
//
//        }
        if(markedWords != null && markedWords.size() > 0) {
            markedWordsList = new ArrayList<String>(markedWords);
            Collections.sort(markedWordsList);
            adapter = new ArrayAdapter<String>(this, R.layout.list_item, markedWordsList);
        }
        else {
            adapter = new ArrayAdapter<String>(this, R.layout.list_item, new ArrayList<String>());
        }
        getListView().setAdapter(adapter);
        //adapter.setNotifyOnChange(true);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);
        progress = new ProgressDialog(this);
        thisActivity = this;
        task = new LoadWordsTask();
        if(markedWordsList.size() > 0 && pos != "p") {
            task.execute();
        }
        else {
            showDialog(DIALOG_NO_MARKED_WORDS);
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        //updateWords();
    }

    @Override
    protected void onPause() {

        super.onPause();
        Log.i(TAG, "onPause");
        // preventing crash when phone orientation is changing
        if(progress.isShowing()) {
            progress.dismiss();
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        String lemma = markedWordsList.get(position);
        if(definitions != null && definitions.size() > 0) {
            try {
                currentDefinition = definitions.get(position);
            }
            catch(IndexOutOfBoundsException e) {
                e.printStackTrace();
                Log.e(TAG, "Error when loading definition of " + lemma + ", position " + position);
                // show only the word without definition
                currentDefinition = lemma;
            }
        }
        else {
            currentDefinition = lemma;
        }
        // necessary because Android is caching dialogs for performance reasons.
        removeDialog(DIALOG_DEFINITION);
        showDialog(DIALOG_DEFINITION);
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if(id == DIALOG_DEFINITION) {
            builder.setTitle(R.string.definition);
            builder.setMessage(currentDefinition);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    dialog.dismiss();
                }
            });
            return builder.show();
        }
        else if(id == DIALOG_NO_SERVER) {
            builder.setTitle(R.string.error);
            builder.setMessage(R.string.error_server);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    dialog.dismiss();
                }
            });
            return builder.show();
        }
        else if(id == DIALOG_NO_MARKED_WORDS) {
            builder.setTitle(R.string.empty_wordlist);
            builder.setMessage(R.string.no_words_added_to_wordlist);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    dialog.dismiss();
                }
            });
            return builder.show();
        }
        return null;
    }

    private void updateList() {

        getListView().setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }

    private class LoadWordsTask extends AsyncTask<String, Void, ArrayList<String>> {

        @Override
        protected void onPreExecute() {

            progress.setIndeterminate(true);
            progress.setTitle(R.string.loading_def);
            progress.setMessage(getString(R.string.connecting_to_server));
            progress.show();
            Log.i(TAG, "show Progress Bar");
        }

        @Override
        protected ArrayList<String> doInBackground(String... params) {

            ArrayList<String> wordlist = null;
            try {
                String serverResponse = sc.getJSONWordlist(markedWordsList, pos);
                if(serverResponse != null && serverResponse != "") {
                    wordlist = sc.parseJSONWordlist(serverResponse);
                    for(int i = 0; i < wordlist.size(); i++) {
                        Log.i(TAG, "wordlist: " + wordlist.get(i));
                    }
                }
            }
            catch(IOException e) {
                io = e;
                e.printStackTrace();
            }
            return wordlist;
        }

        @Override
        protected void onPostExecute(ArrayList<String> wordlist) {

            progress.dismiss();
            if(io == null && wordlist != null) {
                definitions = wordlist;
                if(definitions.size() != markedWordsList.size()) {
                    Log.e(TAG, "Error! Lists should have same size: " +
                            definitions.size() + " " + markedWordsList.size());
                    Log.e(TAG, "File will NOT be saved!");
                    return;
                }
                // save lemmas in list
                for(int i = 0; i < markedWordsList.size(); i++) {
                    String[] split = definitions.get(i).split("\n\n");
                    if(split.length == 2) {
                        // replace token by type
                        markedWordsList.set(i, split[0]);
                    }
                }

                // save in File
                BufferedWriter bufferedWriter = null;
                try {
                    // true in constructor of FileWriter assures that lines are appended to the file instead of overwriting it
                    bufferedWriter = new BufferedWriter(new FileWriter(new File(getFilesDir()+File.separator+wordsfile), true));
                    for(int i = 0; i < markedWordsList.size(); i++) {

                        bufferedWriter.append(markedWordsList.get(i) + "\t" + definitions.get(i).replace("\n", " "));
                        bufferedWriter.newLine();
                    }

                    bufferedWriter.close();
                }
                catch(IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, wordsfile + " could not be saved.");
                }

            }
            else {
                Log.e(TAG, "Could not retrieve definitions from server!");
                showDialog(DIALOG_NO_SERVER);
            }
            updateList();
        }
    }
}
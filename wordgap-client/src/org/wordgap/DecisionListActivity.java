package org.wordgap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.*;


/**
 * @author: Susanne Knoop
 */
public class DecisionListActivity extends ListActivity {

    ArrayAdapter<String> adapter;
    String[] decision = new String[3];
    boolean share = false;
    private String wordslistFilename = "markedwords.csv";
    private File wordslistFile;
    private static final String TAG = "wordgap - DecisionListActivity";
    private static final int DIALOG_NO_MARKED_WORDS = 0;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        share = getIntent().getBooleanExtra("share", false);

        if(share) {
            decision = new String[3];
            decision[0] =            decision[0] = getString(R.string.my_exercises);
            decision[1] = getString(R.string.default_exercises);
            decision[2] = getString(R.string.marked_words);
        }
        else{
        	decision = new String[2];
            decision[0] = getString(R.string.my_exercises);
            decision[1] = getString(R.string.default_exercises);
        }
        adapter = new ArrayAdapter<String>(this, R.layout.list_item, decision);
        getListView().setAdapter(adapter);
        //getListView().setBackgroundColor(getResources().getColor(R.color.mistygreen));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        //v.setBackgroundColor(getResources().getColor(R.color.apple));
        Intent i = new Intent(this, OpenExActivity.class);
        i.putExtra(getString(R.string.share), share);
        if(position == 0) {
            i.putExtra(getString(R.string.own), true);
            startActivity(i);
        }
        else if(position == 1) {
            i.putExtra(getString(R.string.own), false);
            startActivity(i);
        }
        else if(position == 2) {
             // export word list
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new FileReader(new File(getFilesDir() + File.separator + wordslistFilename)));
            }
            catch(FileNotFoundException e) {
                e.printStackTrace();
                showDialog(DIALOG_NO_MARKED_WORDS);
            }
            String read;
            StringBuilder builder = new StringBuilder();
            try {
                while((read = bufferedReader.readLine()) != null) {
                    builder.append(read + "\n\n");
                    Log.i(TAG, "read Line in wordslistFile: " + read);
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            String content = builder.toString();
            Log.d("wordslist content: ", content);
            try {
                bufferedReader.close();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            if(content.length() == 0) {
                showDialog(DIALOG_NO_MARKED_WORDS);
            }
            else {
            }
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, content);
            //sendIntent.setType("text/comma-separated-values");
            sendIntent.setType("application/csv");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.choose_app_wordlist)));
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if(id == DIALOG_NO_MARKED_WORDS) {
            builder.setTitle(getString(R.string.empty_wordlist));
            builder.setMessage(getString(R.string.no_words_added_to_wordlist));
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
}

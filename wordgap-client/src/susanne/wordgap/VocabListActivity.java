package susanne.wordgap;

import android.app.*;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: Susanne Knoop
 */
public class VocabListActivity extends ListActivity {

    private WordgapApplication app;
    private ArrayAdapter adapter;
    //private HashSet<String> markedWords = new HashSet<>();
    private ArrayList<String> markedWordsList = new ArrayList<>();
    private Map<String, String> definitions = new HashMap<>();
    private Activity thisActivity;
    String currentDefinition;
    IOException io;
    private static final String TAG = "wordgap - VocabListActivity";

    private String wordslistFilename = "markedwords.csv";
    private File wordslistFile;
    private static final int DIALOG_DEFINITION = 0;
    private static final int DIALOG_NO_FILE = 1;
    private static final int DIALOG_NO_MARKED_WORDS = 2;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.app = (WordgapApplication) getApplication();
        markedWordsList = new ArrayList<String>();
        Log.i(TAG, "Starting " + TAG);
        adapter = new ArrayAdapter(this, R.layout.list_item, markedWordsList);
        getListView().setAdapter(adapter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);
        wordslistFile = new File(getFilesDir() + File.separator + wordslistFilename);
        if(wordslistFile.exists()) {
            Log.i(TAG, "wordslistFile exists!");
            try {
                updateList();
            }
            catch(IOException io1) {
                Log.e(TAG, io1.getStackTrace().toString());
                showDialog(DIALOG_NO_FILE);
                try {
                    updateList();
                }
                catch(IOException io2) {
                    Log.e(TAG, io2.getStackTrace().toString());
                    showDialog(DIALOG_NO_FILE);
                }
            }
        }
        else {
            showDialog(DIALOG_NO_MARKED_WORDS);
            Log.e(TAG, "wordslistFile does not exist!");
        }
    }

    private void updateList() throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(getFilesDir() + File.separator + wordslistFilename)));
        String read;
        StringBuilder builder = new StringBuilder();
        while((read = bufferedReader.readLine()) != null) {
            builder.append(read);
            Log.i(TAG, "read Line in wordslistFile: " + read);
            String[] split = read.split("\t");
            if(split.length == 2) {
                // avoid duplicates
                if(!markedWordsList.contains(split[0])) {
                    markedWordsList.add(split[0]);
                    definitions.put(split[0], split[1]);
                }

            }
        }
        Log.d("wordslist content: ", builder.toString());
        bufferedReader.close();
        if(markedWordsList.size() == 0) {
            showDialog(DIALOG_NO_MARKED_WORDS);
        }
        else {
            Collections.sort(markedWordsList);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        String lemma = markedWordsList.get(position);
        if(definitions != null && definitions.size() > 0) {
            try {
                currentDefinition = definitions.get(lemma);
            }
            catch(IndexOutOfBoundsException e) {
                e.printStackTrace();
                Log.e(TAG, "Fehler beim Laden der Definition von " + lemma + ", Position " + position);
                // Nur Wort selbst anzeigen
                currentDefinition = lemma;
            }
        }
        else {
            currentDefinition = lemma;
        }
        removeDialog(DIALOG_DEFINITION);
        showDialog(DIALOG_DEFINITION);
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if(id == DIALOG_DEFINITION) {
            builder.setTitle("Definition des gemerkten Wortes");
            builder.setMessage(currentDefinition);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    dialog.dismiss();
                }
            });
            return builder.show();
        }
        else if(id == DIALOG_NO_FILE) {
            builder.setTitle("Fehler");
            builder.setMessage("Die Definitionen können nicht angezeigt werden, weil die Datei nicht geladen werden konnte.");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    dialog.dismiss();
                }
            });
            return builder.show();
        }
        else if(id == DIALOG_NO_MARKED_WORDS) {
            builder.setTitle("Wortliste ist leer!");
            builder.setMessage("Du hast noch keine Wörter zur Wortliste hinzugefügt!");
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

    @Override
    protected void onResume() {

        super.onResume();
        try {
            updateList();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}
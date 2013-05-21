package susanne.wordgap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * @author: Susanne Knoop
 */
public class OpenExActivity extends ListActivity {

    private static final String TAG = "wordgap - OpenExActivity";
    boolean share;
    boolean own;
    AssetManager manager;
    String[] assetFiles;
    String[] userFiles;
    ServerCommunicator sc;
    WordgapApplication app = (WordgapApplication) getApplication();
    private static final int DIALOG_PLAY = 0;
    private static final int DIALOG_DELETE = 1;
    String filename = "";
    private ArrayList<Sent> ex = new ArrayList<>();
    String title = "";
    String text = "";
    String pos;
    private final ArrayList<String> allFiles = new ArrayList<>();
    ArrayAdapter<String> adapter;
    private SharedPreferences prefs;
    private String ipaddress;

    @Override
	public void onCreate(Bundle savedInstanceState) {

        share = getIntent().getBooleanExtra("share", false);
        own = getIntent().getBooleanExtra("own", false);
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("wordgap", MODE_PRIVATE);
        ipaddress = prefs.getString("IP", "192.168.178.27");
        sc = new ServerCommunicator(ipaddress);
        manager = getAssets();
        app = (WordgapApplication) getApplication();
        adapter = new ArrayAdapter(this, R.layout.list_item, allFiles);

        //getListView().setBackgroundColor(getResources().getColor(R.color.mistygreen));
//        ListView lv = getListView();
//        LayoutInflater inflater = getLayoutInflater();
//        ViewGroup header = (ViewGroup)inflater.inflate(R.layout.header, lv, false);
//        lv.addHeaderView(header, null, false);
        getListView().setSmoothScrollbarEnabled(true);
        //getListView().setBackgroundColor(getResources().getColor(R.color.candywhite));
        getListView().setAdapter(adapter);
        adapter.setNotifyOnChange(true);
        adapter.setNotifyOnChange(true);

        updateFilenames();
        if(allFiles.size() == 0) {
            Log.i(TAG, "Keine Dateien vorhanden!");
        }
    }

    private void updateFilenames() {

        allFiles.clear();
        if(own) {
            // listet Dateien aus internem Speicher auf
            //userFiles = getApplicationContext().fileList();
            // oder:
            userFiles = getFilesDir().list();
            String savedFilesString = "";
            for(int i = 0; i < userFiles.length; i++) {
                savedFilesString += " " + userFiles[i];
            }
            Log.i(TAG, "userFiles :" + savedFilesString);
            for(int i = 0; i < userFiles.length; i++) {
                final String filename = userFiles[i];
                if(filename.endsWith(".json")) {
                    allFiles.add(filename.replace(".json", ""));
                }
            }
        }
        else {
            try {
                // listet Dateien aus "/assets/* auf
                assetFiles = manager.list("");
            }
            catch(final IOException e) {
                e.printStackTrace();
            }
            for(int i = 0; i < assetFiles.length; i++) {
                Log.i(TAG, assetFiles[i]);
            }
            for(int i = 0; i < assetFiles.length; i++) {
                final String filename = assetFiles[i];
                if(filename.endsWith(".json")) {
                    allFiles.add(filename.replace(".json", ""));
                }
            }
        }
        Collections.sort(allFiles);
        String allFilesString = "";
        for(int i = 0; i < allFiles.size(); i++) {
            allFilesString += " " + allFiles.get(i);
        }
        Log.i(TAG, "allFiles: " + allFilesString);
        //String[] allFilesArray = (String[]) allFiles.toArray();
        adapter.notifyDataSetChanged();
        //getListView().setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);
        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
			public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {

                Log.i(TAG, "onItemLongClick");
                if(own) {
                    Log.i(TAG, "onItemLongClick");
                    filename = allFiles.get(position);
                    Log.i(TAG, filename);
                    showDialog(DIALOG_DELETE);
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {

        super.onResume();
        updateFilenames();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        Log.i(TAG, "onListItemClick");
        //v.setBackgroundColor(getResources().getColor(R.color.apple));
        // position - 1 weil merkwürdigerweise der Header mitgezählt wird
//        if(position == 0){
//            return;
//        }
        filename = allFiles.get(position) + ".json";
        Log.i(TAG, filename);
        if(share) {
            filename = allFiles.get(position);
            ex = openJSONFile(filename + ".json");
            final StringBuilder sb = new StringBuilder();
            for(int i = 0; i < ex.size(); i++) {
                final Sent s = ex.get(i);
                sb.append(s.toString());
                sb.append("\n\n");
            }
            final Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Wähle eine App aus, an die du " +
                    "die Übung senden möchtest:"));
        }
        else {
            filename = allFiles.get(position);
            removeDialog(DIALOG_PLAY);
            showDialog(DIALOG_PLAY);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch(id) {
            case DIALOG_PLAY:
                builder.setTitle("Übung öffnen");
                builder.setMessage("Möchtest du die Übung " + filename + " jetzt durchführen?");
                builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        ex = openJSONFile(filename + ".json");
                        pos = filename.substring(filename.length() - 1);
                        Log.i(TAG, "POS: " + pos);
                        app.setEx(ex);
                        app.setText(text);
                        app.setTitle(title);
                        app.setPos(pos);
                        Log.i(TAG, "pos: " + app.getPos());
                        final Intent i = new Intent(OpenExActivity.this, GameActivity.class);
                        startActivity(i);
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });
                return builder.show();
            case DIALOG_DELETE:
                builder.setTitle("");
                builder.setMessage("Lösche Übung: " + filename);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        final File dir = getFilesDir();
                        final File file = new File(dir, filename + ".json");
                        final boolean deleted = file.delete();
                        updateFilenames();
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });
                return builder.show();
        }
        return null;
    }

    private ArrayList<Sent> openJSONFile(String filename) {

        ArrayList<Sent> openedEx = null;
        try {
            final StringBuilder sb = new StringBuilder();
            if(own) {
                final FileInputStream in = openFileInput(filename);
                final BufferedInputStream bis = new BufferedInputStream(in);
                while(bis.available() > 0) {
                    sb.append((char) bis.read());
                }
            }
            else {
                final InputStream in = manager.open(filename);
                final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            final String json = sb.toString();
            openedEx = sc.parseJSONEx(json);
        }
        catch(final IOException e) {
            Log.i(TAG, "Kann Datei " + filename + " nicht öffnen");
            e.printStackTrace();
        }
        return openedEx;
    }
}
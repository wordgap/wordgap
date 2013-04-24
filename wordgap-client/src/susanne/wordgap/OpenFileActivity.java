package susanne.wordgap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
/**
 *
 * Quelle: http://android-er.blogspot.de/2010/01/implement-simple-file-explorer-in.html
 *   und http://eat-sleep-and-code.blogspot.de/2010/11/open-file-dialog-in-android.html
 *
 *   selbst geschrieben: readTextFile()
 */
import java.io.*;

import java.util.ArrayList;

import java.util.List;

import android.app.AlertDialog;

import android.app.ListActivity;

import android.content.DialogInterface;

import android.os.Bundle;

import android.util.Log;
import android.view.View;

import android.widget.ArrayAdapter;

import android.widget.ListView;

import android.widget.TextView;

public class OpenFileActivity extends ListActivity {

    private List<String> item = null;

    private List<String> path = null;

    private String root = "/";

    private TextView myPath;
    // nur Textdateien anzeigen 
    private String[] supportedFileExtensions = {"txt"};
    private static final String TAG = "wordgap - OpenFileActivity";

    /**
     * Called when the activity is first created.
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_file);
        myPath = (TextView) findViewById(R.id.path);
        getDir(root);
    }

    private void readTextFile(String path) {

        File file = new File(path);
//Read text from file
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            String title = file.getName().replace(".txt", "");
            Log.i(TAG, "Title: " + title);
            //Log.i(TAG, "Text: " + text);
            WordgapApplication app = (WordgapApplication) getApplication();
            app.setTitle(title);
            app.setText(text.toString());
            Intent i = new Intent(this, NewExActivity.class);
            startActivity(i);
        }
        catch(IOException e) {
            new AlertDialog.Builder(this).setTitle("Die Datei kann nicht gelesen werden!").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    dialog.dismiss();
                }
            }).show();
        }
    }

    private void getDir(String dirPath) {

        myPath.setText("Ordner: " + dirPath);
        item = new ArrayList<String>();
        path = new ArrayList<String>();
        File f = new File(dirPath);
        File[] files = f.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if(pathname.isHidden()) {
                    return false;
                }
                if(!pathname.canRead()) {
                    return false;
                }
                if(pathname.isDirectory()) {
                    return true;
                }
                String fileName = pathname.getName();
                String fileExtension;
                int mid = fileName.lastIndexOf(".");
                fileExtension = fileName.substring(mid + 1, fileName.length());
                for(String s : supportedFileExtensions) {
                    if(s.contentEquals(fileExtension)) {
                        return true;
                    }
                }
                return false;
            }
        });
        if(!dirPath.equals(root)) {
            item.add(root);
            path.add(root);
            item.add("../");
            path.add(f.getParent());
        }
        for(int i = 0; i < files.length; i++) {
            File file = files[i];
            path.add(file.getPath());
            if(file.isDirectory()) {
                item.add(file.getName() + "/");
            }
            else {
                item.add(file.getName());
            }
        }
        ArrayAdapter<String> fileList = new ArrayAdapter<String>(this, R.layout.row, item);
        setListAdapter(fileList);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        File file = new File(path.get(position));
        if(file.isDirectory()) {
            if(file.canRead()) {
                getDir(path.get(position));
            }
            else {
                new AlertDialog.Builder(this)
                        .setTitle("Der Ordner " + file.getName() + " kann nicht gelesen werden!").setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                
                    }
                }).show();
            }
        }
        // wenn lesbare Datei:
        else {
            final String filepath = file.getAbsolutePath();
            new AlertDialog.Builder(this)
                    .setTitle("Neue Übung erstellen aus " + file.getName() + "?").setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).setPositiveButton("Ja", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                    readTextFile(filepath);
                }
            }).show();
        }
    }
}

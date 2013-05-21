package org.wordgap;

import android.app.Application;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author: Susanne Knoop
 */
public class WordgapApplication extends Application {

    private String title;
    private String text;
    private ArrayList<Sent> ex;
    private HashSet<String> markedWords;
    private String pos;

    public String getPos() {

        return pos;
    }

    public void setPos(String pos) {

        this.pos = pos;
    }

    public HashSet<String> getMarkedWords() {

        return markedWords;
    }

    public void setMarkedWords(HashSet<String> markedWords) {

        this.markedWords = markedWords;
    }

    public ArrayList<Sent> getEx() {
        return ex;
    }

    public void setEx(ArrayList<Sent> ex) {
        this.ex = ex;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

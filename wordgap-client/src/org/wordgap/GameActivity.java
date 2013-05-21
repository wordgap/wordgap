package org.wordgap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;


/**
 * @author: Susanne Knoop
 * Activity, die für die eigentliche Durchführung der Übung zuständig ist.
 * Nacheinander wird jeder Satz mit den Distraktoren angezeigt.
 * Am Ende oder nach Klick auf "Abbrechen" wird das Ergebnis angezeigt.
 * 
 * Activity for displaying the exercise. Sentences are displayed sequentially with their target
 * words and distractors. The result is displayed at the end or after tapping "Cancel". 
 */
public class GameActivity extends Activity {

    ArrayList<Sent> ex = new ArrayList<Sent>();
    private static final String TAG = "wordgap - GameActivity";
    private int sentenceNo;
    private Sent currentSentence;
    TextView textView;
    Button b1, b2, b3, b4;
    boolean solved = false;
    boolean end = false;
    int wrong = 0;
    int right = 0;
    HashSet<String> markedWords = new HashSet<String>();
    ArrayList<Button> wrongButtons = new ArrayList<Button>();
    SharedPreferences prefs;
    WordgapApplication app;
    String pos = "";
    private static final int DIALOG_NO_WORDS = 0;
    private Activity thisActivity;
    private boolean backPressedOnce = false;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_layout);
        app = (WordgapApplication) getApplication();
        this.ex = app.getEx();
        textView = (TextView) findViewById(R.id.text);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setScrollbarFadingEnabled(false);
        prefs = getSharedPreferences(getString(R.string.wordgap), MODE_PRIVATE);
        b1 = (Button) findViewById(R.id.b1);
        b2 = (Button) findViewById(R.id.b2);
        b3 = (Button) findViewById(R.id.b3);
        b4 = (Button) findViewById(R.id.b4);
        pos = app.getPos();
        Log.i(TAG, "pos: "+ pos);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);
        thisActivity = this;
        if(this.ex == null && this.ex.size() == 0) {
            Log.e(TAG, "Current App Exercise is null!");
            // TODO Dialog anzeigen
        }
        sentenceNo = 0;
        currentSentence = ex.get(0);
        solved = false;
        end = false;
        updateText();
        updateButtons();
    }

    private void updateText() {

        Spanned textFormatted;
        String text = "<b> " + getString(R.string.sentence) + " " + (sentenceNo+1) + " " + getString(R.string.of) + " " + ex.size() 
        		+ "</b><br><br>";


        if(solved) {

            text += currentSentence.wordsbefore + " " + "<b>" +
                    currentSentence.token + "</b> " + currentSentence.wordsafter;
        }
        else {
            text += currentSentence.wordsbefore + " _____ " + currentSentence.wordsafter;
        }
        textFormatted = Html.fromHtml(text);
        textView.setText(textFormatted);
    }

    private void updateButtons() {

        if(solved) {
            // alle Buttons wieder grün
            greenButtons();
            b1.setText(R.string.back);
            b2.setText(R.string.next);
            b3.setText(R.string.cancel);
            b4.setText(R.string.mark_word);
        }
        else {
            if(wrongButtons.size() == 0) {
                greenButtons();
                ArrayList<String> candidates = new ArrayList<String>(currentSentence.dis);
                candidates.add(currentSentence.token);
                Collections.shuffle(candidates);
                b1.setText(candidates.get(0));
                b2.setText(candidates.get(1));
                b3.setText(candidates.get(2));
                b4.setText(candidates.get(3));
            }
        }
    }

    private void greenButtons() {

        b1.setBackgroundColor(getResources().getColor(R.color.hellblau));
        b2.setBackgroundColor(getResources().getColor(R.color.hellblau));
        b3.setBackgroundColor(getResources().getColor(R.color.hellblau));
        b4.setBackgroundColor(getResources().getColor(R.color.hellblau));
    }

    public void disClick(View v) {

        Button b = (Button) v;
        if(end) {
            handleFinalChoice(v);
        }
        if(solved) {
            // Ein Satz weiter

            Log.i(TAG, "current sentence: " + sentenceNo);
            // end of exercise?
            if(sentenceNo+1 > ex.size() - 1) {
                end = true;
                showResults();
            }
            else {

                if(b.getId() == b1.getId()) {
                    // gehe einen Satz zurück
                    Log.i(TAG, "back");
                    if(sentenceNo > 0) {
                        sentenceNo--;
                    }
                    // wenn beim ersten Satz: abbrechen
                    else {
                        handleFinalChoice(b1);
                    }
                }

                else if(b.getId() == b3.getId()) {
                    Log.i(TAG, getString(R.string.cancel));
                    end = true;
                    showResults();
                    return;
                }

                else if(b.getId() == b4.getId()) {
                    if(pos.equals("p")) {
                        Toast toast = Toast.makeText(this, R.string.prep_not_available, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                    else{
                        markedWords.add(currentSentence.token);
                        app.setMarkedWords(markedWords);
                        Log.i(TAG, currentSentence.token + " marked");
                        Toast toast = Toast.makeText(this, currentSentence.token + getString(R.string.added_to_wordlist), Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }

                // b2 = weiter
                else {

                    sentenceNo++;
                    solved = false;

                }
                // nächster Satz, wenn b2 oder b4 gewählt
                // letzter Satz, wenn b3 gewählt
                currentSentence = ex.get(sentenceNo);

                updateText();
                updateButtons();
            }
        }
        else {
            String clicked = b.getText().toString();
            if(clicked.equals(currentSentence.token)) {
                solved = true;
                right++;
                Log.i(TAG, "right: " + right);
                Toast toast = Toast.makeText(this, currentSentence.token + getString(R.string.correct), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                wrongButtons.clear();
                updateText();
                updateButtons();
            }
            else {
                solved = false;
                if(!wrongButtons.contains(b)) {
                    wrong++;
                    b.setBackgroundColor(getResources().getColor(R.color.apple));
                    Toast toast = Toast.makeText(this, clicked + getString(R.string.incorrect), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    wrongButtons.add(b);
                }
                Log.i(TAG, "wrong: " + wrong);
            }
        }
    }

    private void handleFinalChoice(View v) {

        Button b = (Button) v;

        // Abbrechen
        if(b.getId() == b1.getId()) {

            Intent i = new Intent(this, WordgapMainMenuActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
        }
        else if(b.getId() == b2.getId()) {

                StringBuilder sb = new StringBuilder();
                for(int i = 0; i < this.ex.size(); i++) {
                    Sent s = ex.get(i);
                    sb.append(s.toString());
                    sb.append("\n\n");
                }
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.choose_app_exercise)));

        }
        else if(b.getId() == b3.getId()) {
            Log.i(TAG, markedWords.toString());
            Log.i(TAG, markedWords.size() + " words on word list");
            if(markedWords.size() == 0) {
                showDialog(DIALOG_NO_WORDS);
            }
            else {
                app.setMarkedWords(markedWords);
                Intent i = new Intent(this, MarkedWordsListActivity.class);
                startActivity(i);
            }
        }
        else if(b.getId() == b4.getId()) {
            this.moveTaskToBack(true);
        }
    }

    private void showResults() {

        greenButtons();
        end = true;
        int percent = (int) (((float) right / (float) (right + wrong)) * 100);
        String text = right + " of " + (right + wrong) +
                " attempts were correct.<br><br>That is <b>" + percent + "</b> " + getString(R.string.percent) + ".<br><br>";
        int totalRight = prefs.getInt("totalRight", 0);
        int totalWrong = prefs.getInt("totalWrong", 0);
        if(totalRight > 0 || totalWrong > 0) {
            int totalPercentOld = (int) (((float) totalRight / (float) (totalRight + totalWrong)) * 100);
            int totalPercentNew = (int) (((float) (totalRight + right) / (float) (totalRight + right + totalWrong + wrong)) * 100);
            if(totalPercentOld < totalPercentNew) {
                text += getString(R.string.total_score) + " increased by " + (totalPercentNew - totalPercentOld) + " " + getString(R.string.percent) +
                        " to <b>" + totalPercentNew + "</b> " + getString(R.string.percent) + "!";
            }
            else if(totalPercentOld > totalPercentNew) {
                text += getString(R.string.total_score) + " decreased by " + (totalPercentOld - totalPercentNew)  + " " + getString(R.string.percent)  +
                        " to <b>" + totalPercentNew + "</b> " + getString(R.string.percent) + ".";
            }
            else {
                text += getString(R.string.total_score) + " remains at <b>" + totalPercentNew + "</b> " + getString(R.string.percent) + ".";
            }
        }
        // update prefs
        totalRight += right;
        totalWrong += wrong;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("totalRight", totalRight);
        editor.putInt("totalWrong", totalWrong);
        editor.commit();

        textView.setText(Html.fromHtml(text));
        b1.setText(R.string.new_);
        b2.setText(R.string.export);
        b3.setText(R.string.word_list);
        b4.setText(R.string.quit);
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch(id) {
            case DIALOG_NO_WORDS:
                builder.setMessage(R.string.no_words_added_to_wordlist);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                        Intent i = new Intent(thisActivity, WordgapMainMenuActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(i);
                    }
                });
                return builder.show();

        }
        return null;
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart");
        Intent i = new Intent(this, WordgapMainMenuActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }

    @Override
    protected void onResume() {

        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    public void onBackPressed() {

        if (backPressedOnce) {
            Log.i(TAG, "Exercise aborted with Back-Key");
            handleFinalChoice(b1);
        } else {
            backPressedOnce = true;
            Toast toast = Toast
                    .makeText(this, R.string.press_again, Toast.LENGTH_SHORT);
            toast.show();

            CountDownTimer countdownTimer = new CountDownTimer(2000,2000) {

                @Override
                public void onFinish() {

                    backPressedOnce = false;
                    Log.i(TAG, "Timer ended, backPressedOnce false again");

                }

                @Override
                public void onTick(long arg0) {


                }

            };

            countdownTimer.start();

        }

    }

}

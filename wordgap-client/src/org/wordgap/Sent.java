package org.wordgap;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author: Susanne Knoop
 *
 */
public class Sent {

    String wordsbefore;
    String wordsafter;
    String token;
    ArrayList<String> dis;


    @Override
    public String toString() {

        ArrayList<String> disAndToken = new ArrayList<String>();
        disAndToken.addAll(dis);
        disAndToken.add(token);
        Collections.shuffle(disAndToken);
        String disString = disAndToken.get(0) + ", " + disAndToken.get(1) + ", "
                + disAndToken.get(2) + ", " + disAndToken.get(3);
        return this.wordsbefore + " _____ " + this.wordsafter +
                " ("  + disString + ")\n\n";
    }
}

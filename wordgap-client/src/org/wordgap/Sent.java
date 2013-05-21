package susanne.wordgap;

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

        final ArrayList<String> disAndToken = new ArrayList<>();
        disAndToken.addAll(dis);
        disAndToken.add(token);
        Collections.shuffle(disAndToken);
        final String disString = disAndToken.get(0) + ", " + disAndToken.get(1) + ", "
                + disAndToken.get(2) + ", " + disAndToken.get(3);
        return wordsbefore + " _____ " + wordsafter +
                " ("  + disString + ")\n\n";
    }
}

package susanne.wordgap;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * author: Susanne Knoop Will be started first after the start of the app
 * 
 */

public class WordgapMainMenuActivity extends Activity {
	private static final String TAG = "wordgap - WordgapMainMenu";
	private static final int DIALOG_IP = 0;
	private Activity thisActivity;
	SharedPreferences prefs;
	// private EditText editText;
	private CharSequence oldIP;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu);
		// get previously saved IP
		prefs = getSharedPreferences("wordgap", MODE_PRIVATE);
		oldIP = prefs.getString("IP", "192.168.178.27");
		Log.i(TAG, "oldIP: " + oldIP);

		// editText = (EditText) findViewById(R.id.ip);
		// editText = (EditText) findViewById(R.id.ip);
		// editText.setText(oldIP);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {

		super.onPostCreate(savedInstanceState);
		thisActivity = this;
		showDialog(DIALOG_IP);
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_IP:
			final LayoutInflater inflater = getLayoutInflater();
			final View dialogIPView = inflater
					.inflate(R.layout.dialog_ip, null);
			final EditText editText = (EditText) dialogIPView
					.findViewById(R.id.ip);
			editText.setText(oldIP);
			builder.setView(dialogIPView);
			builder.setTitle("Bitte IP des Servers eingeben");
			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {

							final Editable newIP = editText.getText();
							Log.i(TAG, "new IP: " + newIP.toString());
							if (!newIP.toString().equals("")) {
								if (!oldIP.equals(newIP.toString())) {
									final SharedPreferences.Editor editor = prefs
											.edit();
									editor.putString("IP", newIP.toString());
									editor.commit();
								}
								dialog.dismiss();
							}
						}
					});
			builder.setNegativeButton("Abbrechen",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {

							dialog.dismiss();
							thisActivity.moveTaskToBack(true);
						}
					});
			return builder.show();
		}
		return null;
	}

	public void imageClick(View v) {

		Intent i;
		switch (v.getId()) {
		case R.id.one:
			i = new Intent(this, OpenFileActivity.class);
			Log.i(TAG, "OpenFileActivity startet jetzt");
			startActivity(i);
			break;
		case R.id.two:
			i = new Intent(this, DecisionListActivity.class);
			startActivity(i);
			break;
		case R.id.three:
			i = new Intent(this, VocabListActivity.class);
			startActivity(i);
			break;
		case R.id.four:
			i = new Intent(this, DecisionListActivity.class);
			i.putExtra("share", true);
			startActivity(i);
		default:
		}
	}
}
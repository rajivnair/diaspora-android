package de.denschub.diaspora;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Starts a Service that carries out the http POST.
 * 
 * @author Gardner <gardner@invulnerable.org>
 * 
 */
public class Login extends Activity {
	private static final String TAG = "Diaspora";
	public static boolean isLoggedIn = false;
	private Button login; 
	private EditText username;
	private EditText password;
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 == Activity.RESULT_OK) {
				isLoggedIn = true;
				Toast.makeText(Login.this, getString(R.string.auth_success), Toast.LENGTH_SHORT).show();
				finish();
			} else {
				isLoggedIn = false;
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(200);
				Toast.makeText(Login.this, getString(R.string.auth_fail), Toast.LENGTH_LONG).show();
				login.setEnabled(true);
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		login = (Button) findViewById(R.id.LoginButton);
		username = (EditText) findViewById(R.id.username);
		password = (EditText) findViewById(R.id.password);

		login.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				Toast.makeText(getApplicationContext(), "Authenticating...", Toast.LENGTH_SHORT).show();
				login.setEnabled(false);
				Log.d(TAG, "Login button clicked. Starting service.");
				// Runs in the bg to keep the UI thread free.
				Intent i = new Intent(getApplicationContext(), DiasporaService.class);				
				i.putExtra(DiasporaService.EXTRA_MESSENGER, new Messenger(handler));
				i.setAction(DiasporaService.ACTION_LOGIN);
				i.putExtra("username", username.getText().toString());
				i.putExtra("password", password.getText().toString());
				startService(i);
			}
		});

	}

	public void onStop() {
		super.onStop();
		if (Login.isLoggedIn) {
			SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
			prefs.edit().putString("username", username.getText().toString());
			prefs.edit().putString("password", password.getText().toString());
		}
	}

	public void onResume() {
		super.onResume();
		login.setEnabled(true);
		if (!Login.isLoggedIn) {
			SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);

			username.setText(prefs.getString("username", "gardner"));
			password.setText(prefs.getString("password", "changeme"));
		}

	}
}
package de.denschub.diaspora;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class HomeActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    public void onResume() {
    	super.onResume();
        if (!Login.isLoggedIn) {
        	// show the login activity
			Intent login = new Intent(this.getApplicationContext(), Login.class);
			login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			login.addCategory(Intent.CATEGORY_LAUNCHER);
			login.setAction(Intent.ACTION_MAIN);
			startActivity(login);		
        }
    }
}
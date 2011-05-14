/**
 * 
 */
package de.denschub.diaspora;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Starts a Service that carries out the http POST.
 * Based on a blog post by Vikas Patel. Thanks for posting your code Vikas!
 * @link https://vikaskanani.wordpress.com/2011/01/29/android-image-upload-activity/
 * 
 * @author Gardner <gardner@invulnerable.org>
 * 
 */
public class UploadImage extends Activity {
	private Context context;
	private Activity uploadactivity;
	public static final String TAG = "Diaspora";
	private ImageView imgView;
	private Button upload;
	private EditText caption;
	private ProgressDialog dialog;
	private Uri uri = null;
	private LinkedList<String> aspect_ids = new LinkedList<String>();
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (dialog.isShowing())
				dialog.dismiss();
		}
	};
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uploadimage);
		
		Log.d(TAG, "UploadImage.onCreate()");
		if (null != savedInstanceState) {
			for (String key : savedInstanceState.keySet()) {
				Object x = savedInstanceState.get(key);
				Log.d(TAG, "onCreate Object: " + x.toString());
				Log.d(TAG, key + " = " + x.getClass().getCanonicalName());
			}
		}
		
		imgView = (ImageView) findViewById(R.id.ImageView);
		upload = (Button) findViewById(R.id.Upload);
		caption = (EditText) findViewById(R.id.Caption);
		upload.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				if (uri == null) {
					Toast.makeText(getApplicationContext(),
							"Please select image", Toast.LENGTH_SHORT).show();
				} else {
					dialog = ProgressDialog.show(UploadImage.this, "Uploading",
							"Please wait...", true);
					Log.d(TAG, "Login button clicked. Starting service.");
					// Runs in the bg to keep the UI thread free.
					Intent i = new Intent(getApplicationContext(), DiasporaService.class);
					i.setData(uri);
					i.putExtra(DiasporaService.EXTRA_MESSENGER, new Messenger(handler));
					i.setAction(DiasporaService.ACTION_UPLOAD);
					startService(i);

				}
			}
		});
	}
	
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		Log.d(TAG, "UploadImage.onRestart()");
		Bundle extras = this.getIntent().getExtras();
		if (null != extras) {
			for (String key : extras.keySet()) {
				Object x = extras.get(key);
				Log.d(TAG, "onRestart Object: " + x.toString());
				Log.d(TAG, key + " = " + x.getClass().getCanonicalName());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
		Log.d(TAG, "UploadImage.onRestoreInstanceState()");
		if (null != savedInstanceState) {
			for (String key : savedInstanceState.keySet()) {
				Object x = savedInstanceState.get(key);
				Log.d(TAG, "onRestore Object: " + x.toString());
				Log.d(TAG, key + " = " + x.getClass().getCanonicalName());
			}
		}
	}
	
	

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
//		setContentView(R.layout.uploadimage);
	}

	public void onStart() {
		super.onStart();
		context = getApplicationContext();
		// check to make sure that we are logged in
		Log.d(TAG, "UploadImage.onStart()");
		Bundle extras = this.getIntent().getExtras();
		if (null != extras) {			
			uri = (Uri) extras.get("android.intent.extra.STREAM");
			this.imgView.setImageURI(uri);
			
			if (!Login.isLoggedIn) {
				// Show the login Activity
				Intent login = new Intent(context, Login.class);
				login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				login.addCategory(Intent.CATEGORY_LAUNCHER);
				login.setAction(Intent.ACTION_MAIN);
				startActivityForResult(login, 31337);				
			}
		}

	}	
}

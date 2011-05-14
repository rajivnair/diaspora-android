/**
 * 
 */
package de.denschub.diaspora;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.Activity;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/**
 * @author Gardner <gardner@invulnerable.org>
 * 
 */
public class DiasporaService extends IntentService {
	public static final String EXTRA_MESSENGER = "de.denschub.diaspora.EXTRA_MESSENGER";
	public static final String ACTION_UPLOAD = "de.denschub.diaspora.ACTION_UPLOAD";
	public static final String ACTION_LOGIN = "de.denschub.diaspora.ACTION_LOGIN";
	private static final String TAG = "Diaspora";
	private AndroidHttpClient http;
	private HttpContext localContext;
	private CookieStore cookieStore;
	private String username;
	private String password;
	private String token;
	private LinkedList<String> aspects_ids = new LinkedList<String>();

	public DiasporaService(String name) {
		super(name);
		Log.d(TAG, "IntentService constructor: " + name);
	}

	public DiasporaService() {
		super("DiasporaService");
		Log.d(TAG, "IntentService constructor.");
	}

	private void init() {
		Log.d(TAG, "init()");
		http = AndroidHttpClient.newInstance("Diaspora Android App https://github.com/denschub/diaspora-android");
		localContext = new BasicHttpContext();
		cookieStore = new BasicCookieStore();

		HttpHost proxy = new HttpHost("192.168.1.23", 8888);
		http.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

		Context context = getApplicationContext();
		SharedPreferences prefs = context.getSharedPreferences("http_sess", MODE_PRIVATE);
		for (String key : prefs.getAll().keySet()) {
			if (prefs.getString(key, "").length() > 0) {
				BasicClientCookie c = new BasicClientCookie(key, prefs.getString(key, ""));
				Log.d(TAG, "Loading cookie: " + key + " = " + prefs.getString(key, ""));
				cookieStore.addCookie(c);
			}
		}
		token = prefs.getString("token", token);

		localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

	}

	@Override
	public void onDestroy() {
		// save all the cookies
		SharedPreferences prefs = getApplicationContext().getSharedPreferences("http_sess", MODE_PRIVATE);
		for (int i = 0; i < cookieStore.getCookies().size(); i++) {
			Cookie c = cookieStore.getCookies().get(i);
			if (c.getValue().length() < 1) {
				continue; // do not write blank cookies
			}
			Log.d(TAG, "Saving cookie: " + c.getName() + " = " + c.getValue());
			prefs.edit().putString(c.getName(), c.getValue());
		}
		if (token != null && token.length() > 0) {
			prefs.edit().putString("token", token);
		}

		Log.d(TAG, "onDestroy()");
		http.getConnectionManager().shutdown();
		super.onDestroy();
	}

	/**
	 * I would like to see this be a generic method that we can add intents to
	 * and have the extras map 1:1 with json values.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent()");
		init();
		Bundle extras = intent.getExtras();
		if (null == extras) {
			Log.w(TAG, "extras is null inside onHandleIntent()");
		}
		String action = intent.getAction();
		if (null == action) {
			Log.w(TAG, "action is null inside onHandleIntent()");
		}
		Messenger messenger = (Messenger) extras.get(EXTRA_MESSENGER);
		Message msg = Message.obtain();
		// Whatever RESULT_OK is not
		msg.arg1 = Activity.RESULT_OK ^ 0xFFFFFFFF;
		Log.d(TAG, "IntentService handling: " + intent.getAction());
		if (null == extras) {
			Log.e(TAG, "Intent has null extras while trying to login");
		}
		if (action.equalsIgnoreCase("de.denschub.diaspora.ACTION_LOGIN")) {
			username = extras.getString("username");
			password = extras.getString("password");
			if (login(username, password)) {
				msg.arg1 = Activity.RESULT_OK;
			}
		} else if (action.equals(ACTION_UPLOAD)) {
			if (uploadFile(intent.getData())) {
				msg.arg1 = Activity.RESULT_OK;

			}
		}
		try {
			messenger.send(msg);
		} catch (android.os.RemoteException e1) {
			Log.w(getClass().getName(), "Exception sending message", e1);
		}

	}

	private boolean login(String username, String password) {
		Log.d(TAG, "login()");
		InputStream in = null;
		String html = this.getUrlAsString("/users/sign_in");
		Log.d(TAG, html);
		token = parseXSSToken(html);

		try {
			HttpPost post = new HttpPost(getString(R.string.WebServiceURL) + "/users/sign_in");
			List<NameValuePair> nameValuePairs = new LinkedList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("utf8", "&#x2713;"));
			nameValuePairs.add(new BasicNameValuePair("authenticity_token", token));
			Log.d(TAG, "Token is: " + token);
			nameValuePairs.add(new BasicNameValuePair("user[username]", username));
			nameValuePairs.add(new BasicNameValuePair("user[password]", password));
			nameValuePairs.add(new BasicNameValuePair("commit", "Sign in"));
			nameValuePairs.add(new BasicNameValuePair("user[remember_me]", "1"));
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = http.execute(post, localContext);
			HttpEntity entity = response.getEntity();
			in = entity.getContent();
			html = IOUtils.toString(in);
			if (html.contains("aspect_ids[]\" type=\"hidden\" value=\"")) {
				Log.d(TAG, "html contains aspect_ids directly after login");
			}
			if (response.getStatusLine().getStatusCode() == 302) {
				return true;
			}
			Log.d(TAG, html);
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Error while attempting login", e);
		} catch (IOException e) {
			Log.e(TAG, "Error while attempting login", e);
		} finally {
			IOUtils.closeQuietly(in);
		}

		return false;
	}

	private String parseXSSToken(String html) {
		// TODO reduce number of string copies
		if (html.contains("authenticity_token")) {
			html = html.substring(html.indexOf("name=\"csrf-token\" content=\""));
			// chomp off strings
			html = html.substring(html.indexOf("content="));
			// this is really inefficient
			html = html.substring(html.indexOf("\"") + 1);
			return html.substring(0, html.indexOf("\""));
		} else {
			Log.w(TAG, "Unable to find authenticity_token in html");
			return "";
		}
	}

	private String getUrlAsString(String uri) {
		InputStream in = null;
		try {
			HttpGet get = new HttpGet(getString(R.string.WebServiceURL) + uri);
			HttpResponse response = http.execute(get, localContext);
			HttpEntity entity = response.getEntity();
			in = entity.getContent();
			return IOUtils.toString(in);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private boolean uploadFile(Uri uri) {
		HttpPost httpPost = new HttpPost(getString(R.string.WebServiceURL)
				+ "/photos?photo[pending]=true&qqfile=filenamegoeshere");
		ContentResolver cr = getApplicationContext().getContentResolver();
		String mimeType = cr.getType(uri);
		MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		try {
			entity.addPart("qqfile", new InputStreamBody(cr.openInputStream(uri), mimeType, "filenamegoeshere2"));
			httpPost.setEntity(entity);
			HttpResponse response = http.execute(httpPost, localContext);
			String result = IOUtils.toString(response.getEntity().getContent());
			Log.d(TAG, result);
			return true;
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Error while uploading file: ", e);
			return false;
		} catch (IOException e) {
			Log.e(TAG, "Error while uploading file: ", e);
			return false;
		}
	}

}

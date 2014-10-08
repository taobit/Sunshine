package tao.com.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MyActivity extends ActionBarActivity {

	private String TAG = MyActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new ForecastFragment())
					.commit();
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.my, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void openPreferredLocationInMap() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String location = preferences.getString(getString(R.string.pref_key_location_name),
				getString(R.string.pref_default_location_name));
		Uri geoLocation = Uri.parse("geo:0, 0?").buildUpon().appendQueryParameter("q", location).build();
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(geoLocation);
		if(intent.resolveActivity(getPackageManager()) != null)
			startActivity(intent);
		else
			Log.d(TAG, "can't call location" + location);
	}

}

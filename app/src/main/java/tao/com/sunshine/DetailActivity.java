package tao.com.sunshine;

import android.content.Intent;

import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.ShareActionProvider;

import tao.com.sunshine.R;

public class DetailActivity extends ActionBarActivity {

	public static final String DATE_KEY = "forecast_date";
	private static final String LOCATION_KEY = "location";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_detail);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new DetailFragment())
					.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.detail, menu);
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

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class DetailFragment extends Fragment {

		private static final String LOG_TAG = DetailFragment.class.getSimpleName();

		private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";

		public static final String DATE_KEY = "forecast_date";

		private ShareActionProvider mShareActionProvider;
		private String mLocation;
		private String mForecast;

		private static final int DETAIL_LOADER = 0;


		public DetailFragment() {
			setHasOptionsMenu(true);
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			outState.putString(LOCATION_KEY, mLocation);
			super.onSaveInstanceState(outState);
		}


		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
		                         Bundle savedInstanceState) {
			return inflater.inflate(R.layout.fragment_detail, container, false);
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			Log.v(LOG_TAG, "in onCreateOptionsMenu");
			// Inflate the menu; this adds items to the action bar if it is present.
			inflater.inflate(R.menu.detailfragment, menu);

			// Retrieve the share menu item
			MenuItem menuItem = menu.findItem(R.id.action_share);

			// Get the provider and hold onto it to set/change the share intent.
			mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

			// If onLoadFinished happens before this, we can go ahead and set the share intent now.
			if (mForecast != null) {
				mShareActionProvider.setShareIntent(createShareForecastIntent());
			}
		}

		private Intent createShareForecastIntent() {
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG);
			return shareIntent;
		}

	}
}
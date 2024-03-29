package tao.com.sunshine;

import android.support.v4.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

	private ArrayAdapter<String> adapter;

	public ForecastFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);    // can handle menu items
	}

	private void updateWeather() {
		WeatherTask task = new WeatherTask();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String location = preferences.getString(getString(R.string.pref_key_location_name), getString(R.string.pref_default_location_name));
		task.execute(location);
	}

	@Override
	public void onStart() {
		super.onStart();
		updateWeather();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_my, container, false);
		adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_text_view, new ArrayList<String>());
		ListView listView = (ListView) rootView.findViewById(R.id.list_view_forecast);
		updateWeather();
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String forecast = adapter.getItem(position);
				Toast.makeText(getActivity(), forecast, Toast.LENGTH_LONG).show();
				startActivity(new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, forecast));
			}
		});
		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.forcastfragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// handle action bar item clicks
		int id = item.getItemId();
		if (id == R.id.action_refresh) {
			updateWeather();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public class WeatherTask extends AsyncTask<String, Void, String[]> {

		private final String LOG_TAG = WeatherTask.class.getSimpleName();

		/* The date/time conversion code is going to be moved outside the asynctask later,
	 * so for convenience we're breaking it out into its own method now.
	 */
		private String getReadableDateString(long time) {
			// Because the API returns a unix timestamp (measured in seconds),
			// it must be converted to milliseconds in order to be converted to valid date.
			Date date = new Date(time * 1000);
			SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
			return format.format(date);
		}

		/**
		 * Prepare the weather high/lows for presentation.
		 */
		private String formatHighLows(double high, double low) {
			// For presentation, assume the user doesn't care about tenths of a degree.
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
			String units = preferences.getString(getString(R.string.pref_units_key),
					getString(R.string.pref_units_metric));

			if (units.equals("imperial")) {
				high = (high * 9) / 5 + 32;
				low = (low * 9) / 5 + 32;
			}
			long roundedHigh = Math.round(high);
			long roundedLow = Math.round(low);

			String highLowStr;
			highLowStr = roundedHigh + "/" + roundedLow;

			return highLowStr;
		}

		/**
		 * Take the String representing the complete forecast in JSON Format and
		 * pull out the data we need to construct the Strings needed for the wireframes.
		 * <p/>
		 * Fortunately parsing is easy:  constructor takes the JSON string and converts it
		 * into an Object hierarchy for us.
		 */
		private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
				throws JSONException {

			// These are the names of the JSON objects that need to be extracted.
			final String OWM_LIST = "list";
			final String OWM_WEATHER = "weather";
			final String OWM_TEMPERATURE = "temp";
			final String OWM_MAX = "max";
			final String OWM_MIN = "min";
			final String OWM_DATETIME = "dt";
			final String OWM_DESCRIPTION = "main";

			JSONObject forecastJson = new JSONObject(forecastJsonStr);
			JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

			String[] resultStrs = new String[numDays];
			for (int i = 0; i < weatherArray.length(); i++) {
				// For now, using the format "Day, description, hi/low"
				String day;
				String description;
				String highAndLow;

				// Get the JSON object representing the day
				JSONObject dayForecast = weatherArray.getJSONObject(i);

				// The date/time is returned as a long.  We need to convert that
				// into something human-readable, since most people won't read "1400356800" as
				// "this saturday".
				long dateTime = dayForecast.getLong(OWM_DATETIME);
				day = getReadableDateString(dateTime);

				// description is in a child array called "weather", which is 1 element long.
				JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
				description = weatherObject.getString(OWM_DESCRIPTION);

				// Temperatures are in a child object called "temp".  Try not to name variables
				// "temp" when working with temperature.  It confuses everybody.
				JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
				double high = temperatureObject.getDouble(OWM_MAX);
				double low = temperatureObject.getDouble(OWM_MIN);

				highAndLow = formatHighLows(high, low);
				resultStrs[i] = day + " - " + description + " - " + highAndLow;
			}

			return resultStrs;
		}

		@Override
		protected String[] doInBackground(String... params) {

			// These two need to be declared outside the try/catch
			// so that they can be closed in the finally block
			HttpURLConnection urlConnection = null;
			BufferedReader reader = null;

			// Will contain the raw JSON response as a string
			String forecastJsonStr = null;

			String format = "json";
			String units = "metric";
			int numDays = 7;

			try {
				// Construct the URL for the OpenWeatherMap Query
				// Possible parameters are available at OWM's forecast API page, at
				// http://openweather.org/API#forecast

				Uri builtUri = Uri.parse(CommonUtils.FORECAST_BASE_URL).buildUpon()
						.appendQueryParameter(CommonUtils.QUERY_PARAM, params[0])
						.appendQueryParameter(CommonUtils.FORMAT_PARAM, format)
						.appendQueryParameter(CommonUtils.UNITS_PARAM, units)
						.appendQueryParameter(CommonUtils.DAYS_PARAM, Integer.toString(numDays))
						.build();


				URL url = new URL(builtUri.toString());

				// Create the request to OpenWeatherMap, and open the connection
				urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestMethod("GET");
				urlConnection.connect();

				// Read the input stream into a String
				InputStream inputStream = urlConnection.getInputStream();
				StringBuilder buffer = new StringBuilder();
				if (inputStream == null) return null;
				reader = new BufferedReader(new InputStreamReader(inputStream));

				String line;
				while ((line = reader.readLine()) != null) {
					// Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
					// But it does make debugging a *lot* easier if you print out the completed
					// buffer for debugging.
					buffer.append(line).append("\n");
				}

				if (buffer.length() == 0) return null;
				forecastJsonStr = buffer.toString();
			} catch (IOException e) {
				Log.e(LOG_TAG, "Error", e);
				// If the code didn't successfully get the weather data, there's no point in attempting
				// to parse it.
				return null;
			} finally {
				if (urlConnection != null) {
					urlConnection.disconnect();
				}
				if (reader != null) {
					try {
						reader.close();
					} catch (final IOException e) {
						Log.e(LOG_TAG, "Error closing stream", e);
					}
				}
			}

			try {
				return getWeatherDataFromJson(forecastJsonStr, numDays);
			} catch (JSONException e) {
				Log.e(LOG_TAG, e.getMessage(), e);
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(String[] result) {
			if (result != null) {
				adapter.clear();
				for (String dayForecastStr : result) {
					adapter.add(dayForecastStr);
				}
			}
		}
	}
}

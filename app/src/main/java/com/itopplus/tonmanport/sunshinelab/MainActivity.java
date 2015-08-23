package com.itopplus.tonmanport.sunshinelab;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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

public class MainActivity extends AppCompatActivity {

    private ForeCastObjAdapter AdapterforecastEntry;
    private final String LOG_TAG = "SunshineTAG";
    private void RefreshData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        String location = prefs.getString(getString(R.string.location_key), getString(R.string.default_label_location));
        String units = prefs.getString(getString(R.string.pref_units_key),getString(R.string.pref_units_metric));

        FetchWeatherTask weatherTask = new FetchWeatherTask();
        String[] params ={
                location,units
        };
        weatherTask.execute(params);
    }

    @Override
    protected void onStart() {
        super.onStart();
        RefreshData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mainframent, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingIntent = new Intent();
            settingIntent.setClass(getApplicationContext(),SettingsActivity.class);
            startActivity(settingIntent);
            return true;
        }else if (id == R.id.action_refresh){
            RefreshData();
            return true;
        }
        else if (id == R.id.action_map)
        {
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationInMap() {
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        String location = sharedPrefs.getString(
                getString(R.string.location_key),
                getString(R.string.default_label_location));

        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q", location)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + location + ", no receiving apps installed!");
        }
    }

    public class FetchWeatherTask extends AsyncTask<String,Void,ForeCastObj []>{
        private final String ActivityTag = MainActivity.class.getSimpleName();
        private final String format = "json";
        private final int numDays = 7;
        private String Unit = "metric";

        @Override
        protected void onPostExecute(final ForeCastObj[] results) {
            AdapterforecastEntry = new ForeCastObjAdapter(getApplicationContext(),R.layout.activity_main_list_item_forecast,results);
            ListView listview = (ListView)findViewById(R.id.listview_forecast);
            listview.setAdapter(AdapterforecastEntry);
            Log.v(ActivityTag, "Enter");
            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //AdapterforecastEntry.getItem(position);
                    Log.v(ActivityTag, "Clicked");
                    //Toast.makeText(getApplicationContext(),results[position],Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.setClass(getApplicationContext(), DetailActivity.class);
                    intent.putExtra(Intent.EXTRA_TEXT, results[position].day);
                    startActivity(intent);
                }
            });

            TextView txtToday = (TextView)findViewById(R.id.txtToday);
            TextView txtHigh = (TextView)findViewById(R.id.txtHigh);
            TextView txtLow = (TextView)findViewById(R.id.txtLow);
            TextView txtCloud = (TextView)findViewById(R.id.txtCloud);
            ImageView imageView = (ImageView)findViewById(R.id.imageDetail);
            txtToday.setText(results[0].day);
            txtHigh.setText(results[0].hight);
            txtLow.setText(results[0].low);
            txtCloud.setText(results[0].description.toUpperCase());


                switch(results[0].description){
                    case "Clouds":
                        imageView.setImageResource(R.drawable.cloud_day);
                        break;
                    case "Rain":
                        imageView.setImageResource(R.drawable.showers_day);
                        break;
                    case "Sunny":
                        imageView.setImageResource(R.drawable.sunny_day);
                        break;
                    default:
                        imageView.setImageResource(R.drawable.sunny_day);
                        break;
                }




            Log.v(ActivityTag,"Async Post Execute");
        }

        @Override
        protected ForeCastObj[] doInBackground(String[] params) {
           String result =  callWebforecastService(params[0],params[1]);
            Unit = params[1];
            try {
                return getWeatherDataFromJson(result, 7);
            }catch (JSONException ex){
                Log.e(ActivityTag, ex.getMessage(),ex);
                ex.printStackTrace();
            }
            return null;

        }

        private String callWebforecastService(String location,String units){

            Log.v(ActivityTag,"Enter call webservice");
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM= "q";
                final String FORMAT_PARAM =  "mode";
                final String UNIT_PARAM = "units";
                final String DAYS_PARAM = "cnt";

                Uri buildUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, location)
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNIT_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM,Integer.toString(numDays))
                        .build();
                // URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q="+location+"&mode=json&units=metric&cnt=7");
                URL url = new URL(buildUri.toString());
                Log.v(ActivityTag,"Build URL ="+buildUri.toString());


                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e("PlaceholderFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                forecastJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }
            return forecastJsonStr;
        }

        /* The date/time conversion code is going to be moved outside the asynctask later,
        * so for convenience we're breaking it out into its own method now.
        */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "℉/" + roundedLow + "℉";
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private ForeCastObj[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            ForeCastObj[] resultStrs = new ForeCastObj[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                //day = getReadableDateString(dateTime);
                day = Utility.getDayName(getApplicationContext(),dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                ForeCastObj tmpObj = new ForeCastObj();
                tmpObj.day = day;
                tmpObj.description = description;
                Log.d(LOG_TAG,Unit);
                tmpObj.hight = Utility.formatTemperature(getApplicationContext(),high,(Unit=="metric"? false:true));
                tmpObj.low = Utility.formatTemperature(getApplicationContext(),low,(Unit=="metric"? false:true));

                resultStrs[i] = tmpObj;
            }

            return resultStrs;
        }
    }

}

package app.sunrise.com.example.ciordache.sunrise;

import android.content.Intent;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.TextView;
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
import java.util.Arrays;
import java.util.Date;

/**
 * Created by ciordache on 06.08.2014.
 */

    public   class ForecastFragment extends Fragment {

        ArrayList<String> forecast ;
    ArrayAdapter<String> arrayAdapter;
        public ForecastFragment() {
            forecast = new ArrayList<String>();
        }
        Uri.Builder uriBuilder;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            forecast.add("Today sunny - 88/63");
            forecast.add("Today sunny - 83/63");
            forecast.add("Today sunny - 84/63");
            forecast.add("Today sunny - 85/63");
            forecast.add("Today sunny - 86/63");
            forecast.add("Today sunny - 87/63");

            uriBuilder = new Uri.Builder();

            uriBuilder.scheme("http").authority("api.openweathermap.org").appendPath("data").appendPath("2.5").appendPath("forecast");
            uriBuilder.appendPath("daily").appendQueryParameter("q","94043").appendQueryParameter("units","metric").appendQueryParameter("cnt","7");
            uriBuilder.appendQueryParameter("mode","json");


            GetJsonBack getJsonBack = new GetJsonBack() ;
            getJsonBack.execute(new String[] {"location"});


             arrayAdapter = new ArrayAdapter<String>(getActivity(),R.layout.list_item_forecast,R.id.list_item_forecast_textview,forecast);
            ListView ll = (ListView)  rootView.findViewById(R.id.listview_forecast);
            ll.setAdapter(arrayAdapter);
            ll.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Toast.makeText(getActivity(),"position" + i  + "position 2" + l + "view" +((TextView)view).getText() ,Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(getActivity(),DetailActivity.class);
                    Bundle b  = new Bundle();
                    b.putString("detail",((TextView) view).getText().toString());
                    intent.putExtras(b);

                    startActivity(intent);


                }
            });
            return rootView;
        }


        private class GetJsonBack extends AsyncTask<String, Void,String[]> {
            private final String LOG_TAG = GetJsonBack.class.getSimpleName();
            @Override
            protected String[] doInBackground(String[] objects) {

                String location = (String) objects[0];
                   String json = getJson(location);

                String [] forecastList=null;

                try {
                    forecastList =  (String[]) getWeatherDataFromJson(json,7);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error JSON", e);
                }

                return forecastList;

            }

            @Override
            protected void onPostExecute(String[] strings) {
                forecast = new ArrayList<String>( Arrays.asList(strings));
                if(arrayAdapter!=null ) {

                    arrayAdapter.clear();
                    arrayAdapter.addAll(forecast);
                }
            }

            /* The date/time conversion code is going to be moved outside the asynctask later,
                         * so for convenience we're breaking it out into its own method now.
                         */
            private String getReadableDateString(long time){
                // Because the API returns a unix timestamp (measured in seconds),
                // it must be converted to milliseconds in order to be converted to valid date.
                Date date = new Date(time * 1000);
                SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
                return format.format(date).toString();
            }

            /**
             * Prepare the weather high/lows for presentation.
             */
            private String formatHighLows(double high, double low) {
                // For presentation, assume the user doesn't care about tenths of a degree.
                long roundedHigh = Math.round(high);
                long roundedLow = Math.round(low);

                String highLowStr = roundedHigh + "/" + roundedLow;
                return highLowStr;
            }

            /**
             * Take the String representing the complete forecast in JSON Format and
             * pull out the data we need to construct the Strings needed for the wireframes.
             *
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

            private String getJson(String location) {
                HttpURLConnection urlConnection = null;
                BufferedReader bufferedReader = null;
                String forecastJson = null;

                try {

                    URL url = new URL(uriBuilder.build().toString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    InputStream inputStream = urlConnection.getInputStream();

                    StringBuffer buffer = new StringBuffer();

                    if (inputStream == null) {
                        forecastJson = null;
                    }

                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;

                    while ((line = bufferedReader.readLine()) != null) {

                        buffer.append(line + "/n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        forecastJson = null;
                    } else {
                        forecastJson = buffer.toString();
                    }



                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error ", e);
                    // If the code didn't successfully get the weather data, there's no point in attempting
                    // to parse it.
                    forecastJson = null;
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (final IOException e) {
                            Log.e(LOG_TAG, "Error closing stream", e);
                        }
                    }

                }
                return forecastJson;
            }


        }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment,menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id== R.id.action_refresh) {

            GetJsonBack getJsonBack = new GetJsonBack() ;

            getJsonBack.execute(new String[] {""});
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}


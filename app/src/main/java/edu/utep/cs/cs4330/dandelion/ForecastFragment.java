package edu.utep.cs.cs4330.dandelion;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


/**
 * Created by ajgarcia09 on 3/24/18.
 */

public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> forecastAdapter;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        //inflate the layer for this fragment
        View rootView = inflater.inflate(R.layout.fragment_forecast, container,false);

        ArrayList<String> weekForecast = new ArrayList<String>();
        weekForecast.add("Today - Sunny - 88/63");
        weekForecast.add("Tomorrow - Foggy - 70/46");
        weekForecast.add("Monday - Cloudy - 60/75");
        weekForecast.add("Tuesday - Rainy - 65/80");
        weekForecast.add("Wednesday - Sunny - 70/85");

        forecastAdapter = new ArrayAdapter<String>(this.getActivity(),R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,weekForecast);

        //Get a reference to the listView, attach it to this adapter.
        //Will supply list item layouts to  the list view based on weekForecast
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(forecastAdapter);


        return rootView;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**Allow the fragment to handle menu events
         * (we want callbacks for this method)**/
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
         inflater.inflate(R.menu.menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        /**If refresh was selected, return true**/
        if(id == R.id.action_refresh) {
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("5420926");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]>{

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


        /** Take the String representing the complete forecast in JSON Format and
         * pull our the data we need to construct the Strings needed for the wireframes.
         *
         * Parsing is easy: constructor gets the JSON string and converts it
         * into an Object hierarchy.
         *
         * Sample JSON Object:
         *{
            "cod": "200",
            "message": 0.0046,
            "cnt": 40,
            "list": [
               {
                 "dt": 1524258000,
                 "main": {
                     "temp": 279.45,
                     "temp_min": 279.45,
                     "temp_max": 280.026,
                     "pressure": 808.62,
                     "sea_level": 1020.82,
                     "grnd_level": 808.62,
                     "humidity": 84,
                     "temp_kf": -0.58
                  },
                 "weather": [
                    {
                      "id": 500,
                      "main": "Rain",
                      "description": "light rain",
                      "icon": "10d"
                     }
                  ],
             "clouds": {
                "all": 92
              },
             "wind": {
               "speed": 12.01,
               "deg": 147.001
              },
             "rain": {
               "3h": 1.15679
              },
             "sys": {
               "pod": "d"
             },
              "dt_txt": "2018-04-20 21:00:00"
         },
         next index in list, and so on...
         *
         * @param forecastJsonStr
         * @param numDays
         * @return resultStrs
         * @throws JSONException
         */

        @SuppressLint("NewApi")
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)throws JSONException{
            //These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_MAIN = "main"; //to obtain min and max temperatures
            final String OWM_MAX = "temp_max";
            final String OWM_MIN = "temp_min";
            final String OWM_DESCRIPTION = "main"; //to obtain weather description e.g. sunny, cloudy, windy

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            /*OWM returns only daily forecasts based upon the local time of the city that is being
              asked for, which means that we need to know the GMT offset to translate this
              data properly.

              Since this daya is also sent in-order and the first day is always the current day,
              we're going to take advantage of that to get a nice
              normalized UTC date for all our weather. */

            Time dayTime = new Time();
            dayTime.setToNow();

            //we start at the day returned by local time
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(),dayTime.gmtoff);

            //now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i =0; i < numDays; i++){
                //for now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                //Get each JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                /*The date/time is returned as a long. We need to convert that into something
                human-readable, since most people won't read "1400356800" as "this saturday"
                 */
                long dateTime;
                //Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                //description is in a child array called "weather", which is 1 element long
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                //Temperatures are in a child object called "main"
                JSONObject mainObject = dayForecast.getJSONObject(OWM_MAIN);
                double high = mainObject.getDouble(OWM_MAX);
                double low = mainObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high,low);
                resultStrs[i] = day +" - " + description + " - " + highAndLow;
            }

            for(String s : resultStrs)
                Log.v(LOG_TAG, "Forecast entry: "+s);

            return resultStrs;
        }



        /**The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         * @param time
         * @return
         */
        @TargetApi(Build.VERSION_CODES.N)
        private String getReadableDateString(long time){
            /*Because the API returns a unix timestamp (measured in seconds);
            it must be converted to milliseconds in order to be converted to a
            valid date.
             */
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);

        }

        /**Prepare the weather high/lows for presentation.
         *
         * @param high
         * @param low
         * @return
         */
        private String formatHighLows(double high, double low){
            //For presentation, assume the user doesn't care about the tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;

            return highLowStr;
        }




        @Override
        protected String[] doInBackground(String... params) {
            /**If there's no zip code, there's nothing to look up.
             * (Verify the size of params)
             */

           if(params.length == 0)
              return null;

            /**Need to be declared outside the try/catch
             * block so they will be closed in the finally block
             */

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            /**Will contain the raw JSON response as a string**/

            String forecastJsonStr = null;
            String format = "json";
            String units = "metric";
            int numDays = 7;
            String cityId = "5420926";
            String apiKey = "c262b4ba92b7f829dd3e440b8658d0b1";

            try{
                /**Construct the URL for the OpenWeatherMap query.
                 * Possible parameters are avaiable at OWM's forecast API page, at
                 * http://openweathermap.org/API#forecast
                 */

                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast?";
                final String ID_PARAM = "id"; //El Paso's Open Weather Map City ID
                final String UNITS_PARAM = "units";
                final String APIKEY_PARAM = "APPID";



                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(ID_PARAM, cityId)
                        .appendQueryParameter(UNITS_PARAM,units)
                        .appendQueryParameter(APIKEY_PARAM, apiKey).build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI: " + builtUri.toString());
                //http://api.openweathermap.org/data/2.5/forecast?id=5420926&APPID=c262b4ba92b7f829dd3e440b8658d0b1
                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast?id=5420926&APPID=c262b4ba92b7f829dd3e440b8658d0b1");

                /**Create the request to OpenWeatherMap
                 * and open the connection
                 */

                urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                /**Read the input stream into a String**/

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if(inputStream == null)
                    return null; //nothing to do
                reader = new BufferedReader(new InputStreamReader(inputStream));

                /**Since it's JSON, adding a newline isn't necessary (it won't affect parsing).
                 * But it makes debugging a lot easier if the completed buffer
                 * is printed for debugging.
                 */
                String line;
                while((line = reader.readLine()) != null){
                    buffer.append(line + "\n");
                }

                if(buffer.length() == 0)
                    return null; //stream is empty, can't parse it

                forecastJsonStr = buffer.toString();
                /**output the network data to the logger to
                 * verify we got all the JSON stuff correctly.
                 */
                Log.v(LOG_TAG, "Forecast JSON String: " + forecastJsonStr);
            } catch(IOException e){
                /**Failed to fetch weather data. Can't parse it**/
                Log.e(LOG_TAG,"Error", e);
                return null;
            } finally {
                if(urlConnection != null){
                    urlConnection.disconnect();
                }
                if(reader!=null){
                    try{
                        reader.close();
                    }catch(final IOException e){
                        Log.e(LOG_TAG,"Error closing stream",e);
                    }
                }

            }
            try{
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch(JSONException e){
                Log.e(LOG_TAG, e.getMessage(),e);
                e.printStackTrace();
            }
            Log.e(LOG_TAG, "Returned empty array, try-catch failed");
            return new String[0];
        }

        @Override
        protected void onPostExecute(String[] result) {
            if(result != null){
                forecastAdapter.clear();
                //new weather data
                forecastAdapter.addAll(result);

            }
        }
    }


}

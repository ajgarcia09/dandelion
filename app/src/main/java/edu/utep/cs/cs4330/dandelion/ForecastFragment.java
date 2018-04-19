package edu.utep.cs.cs4330.dandelion;

import android.app.Fragment;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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

        ArrayAdapter<String> forecastAdapter = new ArrayAdapter<String>(this.getActivity(),R.layout.list_item_forecast,
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

    public class FetchWeatherTask extends AsyncTask<String, Void, Void>{

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        @Override
        protected Void doInBackground(String... params) {
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
                //final String QUERY_PARAM = "q";
                //final String FORMAT_PARAM = "mode";
                //final String UNITS_PARAM = "units";
                //final String DAYS_PARAM = "cnt";
                final String APIKEY_PARAM = "APPID";


//                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
//                        .appendQueryParameter(ID_PARAM, cityId)
//                        .appendQueryParameter(APIKEY_PARAM, apiKey)
//                        .appendQueryParameter(QUERY_PARAM, params[0])
//                        .appendQueryParameter(FORMAT_PARAM, format)
//                        .appendQueryParameter(UNITS_PARAM, units)
//                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays)).build();

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(ID_PARAM, cityId)
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
            return null;
        }
    }


}

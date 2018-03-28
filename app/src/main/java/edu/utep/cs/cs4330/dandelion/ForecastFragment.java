package edu.utep.cs.cs4330.dandelion;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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

public class MainFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        //inflate the layer for this fragment
        View rootView = inflater.inflate(R.layout.fragment_main, container,false);

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

        /**Need to be declared outside the try/catch
         * block so they will be closed in the finally block
         */

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        /**Will contain the raw JSON response as a string**/

        String forecastJsonStr = null;

        try{
            /**Construct the URL for the OpenWeatherMap query.
             * Possible parameters are avaiable at OWM's forecast API page, at
             * http://openweathermap.org/API#forecast
             */

            URL url = new URL("http://api.openweathermap.org/data/2.5/forecast?id=5420926&APPID=c262b4ba92b7f829dd3e440b8658d0b1");

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
        } catch(IOException e){
            /**Failed to fetch weather data. Can't parse it**/
            Log.e("MainFragment","Error", e);
            return null;
        } finally {
            if(urlConnection != null){
                urlConnection.disconnect();
            }
            if(reader!=null){
                try{
                    reader.close();
                }catch(final IOException e){
                    Log.e("MainFragment","Error closing stream",e);
                }
            }

        }

        return rootView;

    }


}

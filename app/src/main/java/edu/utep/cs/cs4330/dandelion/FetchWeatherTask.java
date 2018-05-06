package edu.utep.cs.cs4330.dandelion;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import edu.utep.cs.cs4330.dandelion.data.WeatherContract;
import edu.utep.cs.cs4330.dandelion.data.WeatherContract.WeatherEntry;



/**
 * Created by ajgarcia09 on 5/5/18.
 */

public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    private ArrayAdapter<String> mForecastAdapter;
    private final Context mContext;

    public FetchWeatherTask(Context context, ArrayAdapter<String> forecastAdapter) {
        mContext = context;
        mForecastAdapter = forecastAdapter;
    }

    private boolean DEBUG = true;
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
     *
     * @return resultStrs
     * @throws JSONException
     */

    @SuppressLint("NewApi")
    private String[] getWeatherDataFromJson(String forecastJsonStr, String locationSetting)throws JSONException {
        //These are the names of the JSON objects that need to be extracted.

        //Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        //location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "temp_max";
        final String OWM_MIN = "temp_min";

        final String OWM_WEATHER = "weather";
        final String OWM_WEATHER_ID = "id";
        final String OWM_WIND = "wind";


        final String OWM_MAIN = "main"; //to obtain min and max temperatures

        final String OWM_DESCRIPTION = "main"; //to obtain weather description e.g. sunny, cloudy, windy

        try{
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);
            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());


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

            for(int i =0; i < weatherArray.length(); i++) {
                //for now, using the format "Day, description, hi/low"
                // These are the values that will be collected.
                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;
                //Get each JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                /*The date/time is returned as a long. We need to convert that into something
                human-readable, since most people won't read "1400356800" as "this saturday"
                 */

                //Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);

                //description is in a child array called "weather", which is 1 element long
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                //Temperatures are in a child object called "main"
                JSONObject mainObject = dayForecast.getJSONObject(OWM_MAIN);
                high = mainObject.getDouble(OWM_MAX);
                low = mainObject.getDouble(OWM_MIN);
                pressure = mainObject.getDouble(OWM_PRESSURE);
                humidity = mainObject.getInt(OWM_HUMIDITY);

                JSONObject windObject = dayForecast.getJSONObject(OWM_WIND);
                windSpeed = windObject.getDouble(OWM_WINDSPEED);
                windDirection = windObject.getDouble(OWM_WIND_DIRECTION);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                cVVector.add(weatherValues);
            }
// add to database
            if ( cVVector.size() > 0 ) {
                // Student: call bulkInsert to add the weatherEntries to the database here
                // ContentValues [] cvArray = new ContentValues[cVVector.size()];
                // cVVector.toArray(cvArray);
                //mContext.getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI,cvArray);
            }

            // Sort order:  Ascending, by date.
            String sortOrder = WeatherEntry.COLUMN_DATE + " ASC";
            Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(
                    locationSetting, System.currentTimeMillis());

            // Students: Uncomment the next lines to display what what you stored in the bulkInsert

//            Cursor cur = mContext.getContentResolver().query(weatherForLocationUri,
//                    null, null, null, sortOrder);
//
//            cVVector = new Vector<ContentValues>(cur.getCount());
//            if ( cur.moveToFirst() ) {
//                do {
//                    ContentValues cv = new ContentValues();
//                    DatabaseUtils.cursorRowToContentValues(cur, cv);
//                    cVVector.add(cv);
//                } while (cur.moveToNext());
//            }

            Log.d(LOG_TAG, "FetchWeatherTask Complete. " + cVVector.size() + " Inserted");

            String[] resultStrs = convertContentValuesToUXFormat(cVVector);
            return resultStrs;

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
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
           /*Data is fetched in Celsius by default.
           If user prefers to see in Fahrenheit, convert the values here.
           We do this rather than fetching in Fahrenheit so that the user can
           change this option without us having to re-fetch the data once
           we start storing the values in a database.
            */
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String unitType = sharedPrefs.getString(
                mContext.getString(R.string.pref_units_key),
                mContext.getString(R.string.pref_units_celsius));

        if(unitType.equals(mContext.getString(R.string.pref_units_fahrenheit))){
            high = (high *1.8) +32;
            low = (low * 1.8) + 32;
        } else if (!unitType.equals(mContext.getString(R.string.pref_units_celsius))){
            Log.d(LOG_TAG, "Unit type not found: "+ unitType);
        }

        //for presentation, assume the user doesn't care about tenths of a degree.

        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName A human-readable city name, e.g "Mountain View"
     * @param lat the latitude of the city
     * @param lon the longitude of the city
     * @return the row ID of the added location.
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // Students: First, check if the location with this city name exists in the db
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        long locationId;
        Cursor locationCursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        if(locationCursor.moveToFirst()){
            int locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationId = locationCursor.getLong(locationIdIndex);
        }
        else{
                /*Now that the content provider is set up, inserting rows of data is simple.
                First create a ContentValues object to hold the data you want to insert.
                 */
            ContentValues locationValues = new ContentValues();

            /*Then add the data, along with the corresponding name of the data type
            so the content provider knows what kind of value is being inserted.
             */
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME,cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT,lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG,lon);

            //Finally, insert location data into the database
            Uri insertedUri = mContext.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues
            );

            /*The resulting URI contains the ID for the row.
            Extract the locationId from the Uri
             */
            locationId = ContentUris.parseId(insertedUri);

        }
        locationCursor.close();
        return locationId;
    }

    /*
    Students: This code will allow the FetchWeatherTask to continue to return the strings that
    the UX expects so that we can continue to test the application even once we begin using
    the database.
 */
    String[] convertContentValuesToUXFormat(Vector<ContentValues> cvv) {
        // return strings to keep UI functional for now
        String[] resultStrs = new String[cvv.size()];
        for ( int i = 0; i < cvv.size(); i++ ) {
            ContentValues weatherValues = cvv.elementAt(i);
            String highAndLow = formatHighLows(
                    weatherValues.getAsDouble(WeatherEntry.COLUMN_MAX_TEMP),
                    weatherValues.getAsDouble(WeatherEntry.COLUMN_MIN_TEMP));
            resultStrs[i] = getReadableDateString(
                    weatherValues.getAsLong(WeatherEntry.COLUMN_DATE)) +
                    " - " + weatherValues.getAsString(WeatherEntry.COLUMN_SHORT_DESC) +
                    " - " + highAndLow;
        }
        return resultStrs;
    }




    @Override
    protected String[] doInBackground(String... params) {
        /**If there's no zip code, there's nothing to look up.
         * (Verify the size of params)
         */

        if(params.length == 0)
            return null;
        String locationQuery = params[0];

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
        Log.v(LOG_TAG,"params[0] = " + params[0]);
        String zipCode = params[0];
        String apiKey = "c262b4ba92b7f829dd3e440b8658d0b1";

        try{
            /**Construct the URL for the OpenWeatherMap query.
             * Possible parameters are avaiable at OWM's forecast API page, at
             * http://openweathermap.org/API#forecast
             */

            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast?";
            final String ID_PARAM = "id"; //El Paso's Open Weather Map City ID
            final String ZIPCODE_PARAM = "zip";
            final String UNITS_PARAM = "units";
            final String APIKEY_PARAM = "APPID";



            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(ZIPCODE_PARAM, zipCode)
                    .appendQueryParameter(UNITS_PARAM,units)
                    .appendQueryParameter(APIKEY_PARAM, apiKey).build();

            URL url = new URL(builtUri.toString());

            Log.v(LOG_TAG, "Built URI: " + builtUri.toString());
            //http://api.openweathermap.org/data/2.5/forecast?id=5420926&APPID=c262b4ba92b7f829dd3e440b8658d0b1
            //new & correct (with city ID):
            //http://api.openweathermap.org/data/2.5/forecast?id=5520993&units=metric&APPID=c262b4ba92b7f829dd3e440b8658d0b1

            //correct with zip code:
            //http://api.openweathermap.org/data/2.5/forecast?zip=79952&units=metric&APPID=c262b4ba92b7f829dd3e440b8658d0b1

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
            return getWeatherDataFromJson(forecastJsonStr, locationQuery);
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
            mForecastAdapter.clear();
            //new weather data
            mForecastAdapter.addAll(result);

        }
    }
}
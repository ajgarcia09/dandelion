package edu.utep.cs.cs4330.dandelion.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;

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

import edu.utep.cs.cs4330.dandelion.MainActivity;
import edu.utep.cs.cs4330.dandelion.R;
import edu.utep.cs.cs4330.dandelion.Utility;
import edu.utep.cs.cs4330.dandelion.data.WeatherContract;
import edu.utep.cs.cs4330.dandelion.data.WeatherContract.WeatherEntry;


public class DandelionSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = DandelionSyncAdapter.class.getSimpleName();
    // Interval at which to sync with the weather, in milliseconds.
    // 60 seconds (1 minute)  180 = 3 hours
    public static final int SYNC_INTERVAL = 60*180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    public DandelionSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting Sync.");

        String zipCode = Utility.getPreferredLocation(getContext());
        /**Need to be declared outside the try/catch
         * block so they will be closed in the finally block
         */

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        /**Will contain the raw JSON response as a string**/

        String forecastJsonStr = null;
        String units = "metric";
        int numDays = 7;

        String apiKey = "c262b4ba92b7f829dd3e440b8658d0b1";

        try{
            /**Construct the URL for the OpenWeatherMap query.
             * Possible parameters are avaiable at OWM's forecast API page, at
             * http://openweathermap.org/API#forecast
             */

            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast?";
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
                return; //nothing to do
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
                return ; //stream is empty, can't parse it

            forecastJsonStr = buffer.toString();
            if(forecastJsonStr == null)
                Log.v(LOG_TAG,"forecastJsonStr is null");
            getWeatherDataFromJson(forecastJsonStr,zipCode);
            /**output the network data to the logger to
             * verify we got all the JSON stuff correctly.
             */
            Log.v(LOG_TAG, "Forecast JSON String: " + forecastJsonStr);
        } catch(IOException e){
            /**Failed to fetch weather data. Can't parse it**/
            Log.e(LOG_TAG,"Error", e);
        } catch (JSONException e){
            Log.e(LOG_TAG, e.getMessage(),e);

        } finally{
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
        return ;
    }

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
    private void getWeatherDataFromJson(String forecastJsonStr, String locationSetting)throws JSONException {
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
            int inserted = 0;
            // add to database
            if ( cVVector.size() > 0 ) {
                // Student: call bulkInsert to add the weatherEntries to the database here
                ContentValues [] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                inserted = getContext().getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI,cvArray);

                //delete old data so we don't build up an endless history
                getContext().getContentResolver().delete(WeatherEntry.CONTENT_URI,
                        WeatherEntry.COLUMN_DATE + " <=?",
                        new String[] {Long.toString(dayTime.setJulianDay(julianStartDay-1))});
                notifyWeather();
            }
            Log.d(LOG_TAG, "Sync Complete." + inserted + "Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void notifyWeather() {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
        Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if ( displayNotifications ) {
            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);

            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
            // Last sync was more than 1 day ago, let's send a notification with the weather.
            String locationQuery = Utility.getPreferredLocation(context);
            Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

            // we'll query our contentProvider, as always
            Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

            if (cursor.moveToFirst()) {
                int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                double high = cursor.getDouble(INDEX_MAX_TEMP);
                double low = cursor.getDouble(INDEX_MIN_TEMP);
                String desc = cursor.getString(INDEX_SHORT_DESC);

                int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
                Resources resources = context.getResources();
                Bitmap largeIcon = BitmapFactory.decodeResource(resources,
                Utility.getArtResourceForWeatherCondition(weatherId));
                String title = context.getString(R.string.app_name);

                // Define the text of the forecast.
                String contentText = String.format(context.getString(R.string.format_notification),
                            desc,
                            Utility.formatTemperature(context, high),
                            Utility.formatTemperature(context, low));

                 // NotificationCompatBuilder is a very convenient way to build backward-compatible
                 // notifications.  Just throw in some data.
                 NotificationCompat.Builder mBuilder =
                 new NotificationCompat.Builder(getContext()).setColor(resources.getColor(R.color.colorAccent))
                                .setSmallIcon(iconId)
                                .setLargeIcon(largeIcon)
                                .setContentTitle(title)
                                .setContentText(contentText);

                 // Make something interesting happen when the user clicks on the notification.
                 // In this case, opening the app is sufficient.
                 Intent resultIntent = new Intent(context, MainActivity.class);

                 // The stack builder object will contain an artificial back stack for the
                 // started Activity.
                 // This ensures that navigating backward from the Activity leads out of
                 // your application to the Home screen.

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);

                NotificationManager mNotificationManager =
                    (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

                // WEATHER_NOTIFICATION_ID allows you to update the notification later on.
                mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());

                //refreshing last sync
                SharedPreferences.Editor editor = prefs.edit();
                                        editor.putLong(lastNotificationKey, System.currentTimeMillis());
                                        editor.commit();
                                    }
                cursor.close();
            }

        }

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
        Cursor locationCursor = getContext().getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
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
            Uri insertedUri = getContext().getContentResolver().insert(
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



    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

        }
        return newAccount;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        DandelionSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}
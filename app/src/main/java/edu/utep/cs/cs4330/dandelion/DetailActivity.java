package edu.utep.cs.cs4330.dandelion;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import edu.utep.cs.cs4330.dandelion.data.WeatherContract.WeatherEntry;


public class DetailActivity extends AppCompatActivity {
    public static final String LOG_TAG = DetailActivity.class.getSimpleName();

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
        //getMenuInflater().inflate(R.menu.detail, menu);
        //getMenuInflater().inflate(R.menu.detailfragment,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this,SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Intent intent = getActivity().getIntent();
            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
            //read the data in the intent
            if(intent != null && intent.hasExtra(Intent.EXTRA_TEXT)){
                String forecastStr = intent.getStringExtra(Intent.EXTRA_TEXT);
                ((TextView)rootView.findViewById(R.id.detail_text)).setText(forecastStr);
            }
            return rootView;
        }
    }

    public static class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

        private static final String LOG_TAG = DetailFragment.class.getSimpleName();
        private static final String FORECAST_SHARE_HASHTAG = "#DandelionApp";
        private ShareActionProvider mShareActionProvider;
        private String mForecast;

        private static final int DETAIL_LOADER = 0;

        private static final String[] FORECAST_COLUMNS = {
                WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
                WeatherEntry.COLUMN_DATE,
                WeatherEntry.COLUMN_SHORT_DESC,
                WeatherEntry.COLUMN_MAX_TEMP,
                WeatherEntry.COLUMN_MIN_TEMP,
        };

        // these constants correspond to the projection defined above, and must change if the
        // projection changes
         private static final int COL_WEATHER_ID = 0;
         private static final int COL_WEATHER_DATE = 1;
         private static final int COL_WEATHER_DESC = 2;
         private static final int COL_WEATHER_MAX_TEMP = 3;
         private static final int COL_WEATHER_MIN_TEMP = 4;

        public DetailFragment(){
            Log.v(LOG_TAG, "ENTERED detailed fragment constructor");
            setHasOptionsMenu(true);

        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
            Log.v(LOG_TAG, "ENTERED onCreateOptionsMenu");
            //inflate the menu; this adds items to the action bar if it is present
            inflater.inflate(R.menu.detailfragment,menu);

            //Retrieve the share menu item
            MenuItem menuItem = menu.findItem(R.id.action_share);

            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

            /**Attach an intent to this ShareActionProvider. You can update this at any time,
             * like when the user selects a new piece of data they might like to share.
             *
             */

            if(mForecast != null){
                mShareActionProvider.setShareIntent(createSharedForecastIntent());
            }
            else{
                Log.d(LOG_TAG, "Share Action Provider is null");
            }
        }



        @Override
        public View onCreateView(LayoutInflater inflater,ViewGroup container,
                                 Bundle savedInstanceState){
            Log.v(LOG_TAG, "ENTERED onCreateView");

            return inflater.inflate(R.layout.fragment_detail,container,false);
        }

        private Intent createSharedForecastIntent(){
            Log.v(LOG_TAG, "ENTERED createSharedForecastIntent");
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            //return to dandelion app, not the app that handles the intent
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG);
            return shareIntent;

        }

        @Override
         public void onActivityCreated(Bundle savedInstanceState) {
           getLoaderManager().initLoader(DETAIL_LOADER, null, this);
            super.onActivityCreated(savedInstanceState);
        }

       @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.v(LOG_TAG, "In onCreateLoader");
            Intent intent = getActivity().getIntent();
            if (intent == null) {
                return null;
            }
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    intent.getData(),
                    FORECAST_COLUMNS,
                    null,
                    null,
                    null
                    );
             }

        @Override
         public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            Log.v(LOG_TAG, "In onLoadFinished");
            if (!data.moveToFirst())
                return;

            String dateString = Utility.formatDate(data.getLong(COL_WEATHER_DATE));
            String weatherDescription = data.getString(COL_WEATHER_DESC);
            boolean isMetric = Utility.isMetric(getActivity());

            String high = Utility.formatTemperature(getActivity(),
                    data.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
            String low = Utility.formatTemperature(getActivity(),
                    data.getDouble(COL_WEATHER_MIN_TEMP), isMetric);
            mForecast = String.format("%s - %s - %s/%s", dateString, weatherDescription, high, low);

            TextView detailTextView = (TextView)getView().findViewById(R.id.detail_text);
            detailTextView.setText(mForecast);

            // If onCreateOptionsMenu has already happened, we need to update the share intent now.
            if (mShareActionProvider != null)
              mShareActionProvider.setShareIntent(createSharedForecastIntent());

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) { }

    }
}
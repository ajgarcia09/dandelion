package edu.utep.cs.cs4330.dandelion;

import android.content.Intent;
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

public class DetailActivity extends AppCompatActivity {
    public static final String LOG_TAG = DetailActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            Log.v(LOG_TAG, "about to create DetailFragment");
            DetailFragment fragment = new DetailFragment();
            Log.v(LOG_TAG, "Created detailFragment");
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
            Log.v(LOG_TAG, "created PlaceholderFragment");
            /*DetailFragment fragment = new DetailFragment()
             //fragment.setArguments(arguments);*/

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        //getMenuInflater().inflate(R.menu.detailfragment,menu);
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

    public static class DetailFragment extends Fragment{
        private static final String LOG_TAG = DetailFragment.class.getSimpleName();
        private static final String FORECAST_SHARE_HASHTAG = "#DandelionApp";
        private String mForecastStr;

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

            //Get the provider and hold onto it to set/change the share intent
            ShareActionProvider mShareActionProvider =
                    (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

            /**Attach an intent to this ShareActionProvider. You can update this at any time,
             * like when the user selects a new piece of data they might like to share.
             *
             */

            if(mShareActionProvider != null){
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
            View rootView = inflater.inflate(R.layout.fragment_detail,container,false);

            //the detail activity called via intent. Inspect the intent for forecast data
            Intent intent = getActivity().getIntent();
            if(intent!= null && intent.hasExtra(Intent.EXTRA_TEXT)){
                mForecastStr = intent.getStringExtra(Intent.EXTRA_TEXT);
                ((TextView) rootView.findViewById(R.id.detail_text)).setText(mForecastStr);
            }
            return rootView;
        }

        private Intent createSharedForecastIntent(){
            Log.v(LOG_TAG, "ENTERED createSharedForecastIntent");
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            //return to dandelion app, not the app that handles the intent
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastStr + FORECAST_SHARE_HASHTAG);
            return shareIntent;

        }

    }
}
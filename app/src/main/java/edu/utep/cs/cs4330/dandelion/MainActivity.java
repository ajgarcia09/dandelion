package edu.utep.cs.cs4330.dandelion;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private String mLocation;
    public static final String FORECASTFRAGMENT_TAG = "FFTAG";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
       mLocation = Utility.getPreferredLocation(this);
        super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(new ForecastFragment(), FORECASTFRAGMENT_TAG)
                    .commit();
        }


    }

    @Override
    protected void onResume(){
        super.onResume();
        String location = Utility.getPreferredLocation(this);
        if(location != null && !location.equals(mLocation)){
            ForecastFragment ff = (ForecastFragment)getSupportFragmentManager().findFragmentByTag(FORECASTFRAGMENT_TAG);
            if(ff != null)
               ff.onLocationChanged();
            mLocation = location;
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /**Handle action bar item clicks here. The action bar will
         * automatically handle clicks on the Home/Up button, so as
         * long as you specify a parent activity in AndroidManifest.xml.
         */
        int id = item.getItemId(); // which item was selected?
        if(id == R.id.action_settings){
            startActivity(new Intent(this,SettingsActivity.class));
            return true;
        }
        if(id == R.id.action_map){
            openPreferredLocationInMap();
        }
        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationInMap(){
        //get the current location from preferences
       String location = Utility.getPreferredLocation(this);

        Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q",location)
                .build();

        //get implicit intent to open the maps app
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        //start the activity only if the activity resolves soccessfully
        if(intent.resolveActivity(getPackageManager()) != null){
            startActivity(intent);
        }
        else{
            Log.d(LOG_TAG, "Couldn't call " + location);
        }
    }

}
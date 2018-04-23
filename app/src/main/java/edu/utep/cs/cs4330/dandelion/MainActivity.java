package edu.utep.cs.cs4330.dandelion;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        return super.onOptionsItemSelected(item);
    }
}
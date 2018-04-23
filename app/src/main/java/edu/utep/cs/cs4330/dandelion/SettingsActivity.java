package edu.utep.cs.cs4330.dandelion;

/**
 * Created by ajgarcia09 on 4/23/18.
 */

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

   public static class PrefFragment extends PreferenceFragment{
        @Override
       public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            //Add "general" preferences, defined in the XML file
            PreferenceManager.setDefaultValues(getActivity(),
                    R.xml.pref_general,false);
            //load the preferences from an XML resource
            addPreferencesFromResource(R.xml.pref_general);
        }
   }

}
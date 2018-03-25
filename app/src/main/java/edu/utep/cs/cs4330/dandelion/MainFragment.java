package edu.utep.cs.cs4330.dandelion;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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
        return rootView;


    }
}

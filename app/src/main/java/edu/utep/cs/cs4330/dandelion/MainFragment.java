package edu.utep.cs.cs4330.dandelion;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by ajgarcia09 on 3/24/18.
 */

public class MainFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        //inflate the layer for this fragment
        return inflater.inflate(R.layout.fragment_main, container,false);
    }
}

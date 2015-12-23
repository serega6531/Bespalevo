package ru.serega6531.bespalevo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class WatchActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private BoxInsetLayout mContainerView;
    private ListView mListView;

    private GoogleApiClient conn;
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> listItems = new ArrayList<>();
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);

        mContainerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        updateDisplay(true);
                        return true;
                    case MotionEvent.ACTION_DOWN:
                        updateDisplay(false);
                        return true;
                }

                return false;
            }
        });

        mListView = (ListView) findViewById(R.id.listView);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        mListView.setAdapter(listAdapter);

        pref = getPreferences(MODE_PRIVATE);
        Set<String> def = new HashSet<>();
        Log.d("Bespalevo", "Loading latest list from config");
        updateList(new ArrayList<>(pref.getStringSet("latest", def)));

        conn = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        updateDisplay(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Bespalevo", "Connecting to GoogleApiClient");
        conn.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Bespalevno", "Removing listener");
        Wearable.DataApi.removeListener(conn, this);
        Log.d("Bespalevo", "Disconnecting from GoogleApiClient");
        conn.disconnect();

        SharedPreferences.Editor editor = pref.edit();
        Log.d("Bespalevo", "Saving latest list to config");
        editor.putStringSet("latest", new HashSet<>(listItems));
        editor.commit();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("Bespalevo", "Adding listener");
        Wearable.DataApi.addListener(conn, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Bespalevo", "Suspended: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("Bespalevo", "Connection failed: " + connectionResult.getErrorMessage());
    }

    @Override
    public void onDataChanged(DataEventBuffer buf) {
        final List<DataEvent> events = FreezableUtils
                .freezeIterable(buf);

        for(DataEvent e : events){
            if(e.getType() == DataEvent.TYPE_CHANGED){
                DataItem i = e.getDataItem();
                if(i.getUri().getPath().equals("/text")){
                    DataMap map = DataMapItem.fromDataItem(i).getDataMap();
                    updateList(map.getStringArrayList("list"));
                }
            }
        }
    }


    /*@Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }*/

    private void updateDisplay(boolean hide) {
        if (hide) {
            //mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            //mListView.setVisibility(View.GONE);
        } else {
            //mContainerView.setBackground(null);
            //mListView.setVisibility(View.VISIBLE);
        }
    }

    private void updateList(ArrayList<String> list){
        Log.d("Bespalevno", "Updating display: " + list.toString());
        listItems.clear();

        list.removeAll(Arrays.asList("dummy"));

        listItems.addAll(list);
        listAdapter.notifyDataSetChanged();
    }

}

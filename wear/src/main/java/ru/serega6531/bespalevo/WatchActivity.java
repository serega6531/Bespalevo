package ru.serega6531.bespalevo;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WatchActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private FrameLayout mContainerView;
    private static ListView mListView;

    private GoogleApiClient conn;
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> listItems = new ArrayList<>();
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mContainerView = (FrameLayout) findViewById(R.id.container);

        mContainerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        updateDisplay(mContainerView, true);
                        return true;
                    case MotionEvent.ACTION_DOWN:
                        updateDisplay(mContainerView, false);
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

        updateDisplay(mContainerView, true);
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
        listItems.remove("Ничего нет :(");
        editor.putStringSet("latest", new HashSet<>(listItems));
        editor.apply();
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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
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

    private static void updateDisplay(FrameLayout mContainerView, boolean hide) {
        if (hide) {
            Log.d("Bespalevo", "Hiding text");
            mContainerView.setBackgroundColor(Color.BLACK);
            mListView.setVisibility(View.GONE);
        } else {
            Log.d("Bespalevo", "Showing text");
            mContainerView.setBackgroundColor(Color.WHITE);
            mListView.setVisibility(View.VISIBLE);
        }
    }

    private void updateList(ArrayList<String> list){
        Log.d("Bespalevno", "Updating display: " + list.toString());
        listItems.clear();

        if(list.size() > 0)
            listItems.addAll(list);
        else
            listItems.add("Ничего нет :(");

        listAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Обновлено", Toast.LENGTH_SHORT).show();
    }

}

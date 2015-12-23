package ru.serega6531.bespalevo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Random;

public class PhoneActivity extends AppCompatActivity {

    private ArrayList<String> listItems = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;
    private GoogleApiClient conn;
    private Random rand = new Random();
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final PhoneActivity activity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ListView list = (ListView) findViewById(R.id.listView);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        list.setAdapter(listAdapter);
        list.setOnTouchListener(new SwipeDismissListViewTouchListener(list, new SwipeDismissListViewTouchListener.DismissCallbacks() {
            @Override
            public boolean canDismiss(int position) {
                return true;
            }

            @Override
            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                for (int position : reverseSortedPositions)
                    listAdapter.remove(listAdapter.getItem(position));
            }
        }));

        conn = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d("Bespalevo", "onConnected: " + connectionHint);

                        PutDataMapRequest req = PutDataMapRequest.create("/text");
                        ArrayList<String> put = new ArrayList<String>(listItems);

                        int dummies = rand.nextInt(5);
                        for(int i = 0; i < dummies; i++)
                            put.add("dummy");       //KOSTILI!!!!

                        req.getDataMap().putStringArrayList("list", put);

                        PendingResult<DataApi.DataItemResult> res = Wearable.DataApi.putDataItem(conn, req.asPutDataRequest());
                        res.setResultCallback(new ResolvingResultCallbacks<DataApi.DataItemResult>(activity, 0) {
                            @Override
                            public void onSuccess(DataApi.DataItemResult dataItemResult) {
                                Log.d("Bespalevo", "Sync successful (" + dataItemResult.getDataItem().getData().length + " bytes)");
                                Toast.makeText(activity, "Отправляется...", Toast.LENGTH_SHORT).show();
                                conn.disconnect();
                            }

                            @Override
                            public void onUnresolvableFailure(Status status) {
                                Log.e("Bespalevo", "Sync error: " + status.getStatusMessage());
                                Toast.makeText(activity, "Ошибка: " + status.getStatusMessage(), Toast.LENGTH_LONG).show();
                                conn.disconnect();
                            }
                        });
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d("Bespalevo", "onConnectionSuspended: " + cause);
                    }
                }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d("Bespalevo", "onConnectionFailed: " + result);
                        Toast.makeText(activity, "Ошибка: " + result.getErrorMessage(), Toast.LENGTH_LONG).show();
                    }
                }).addApi(Wearable.API)
                .build();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText input = new EditText(activity);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                new AlertDialog.Builder(activity)
                        .setTitle("Введите текст")
                        .setView(input)
                        .setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                addText(input.getText().toString());
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("Отменить", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .create().show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_phone, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sync) {
            syncText();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addText(String text){
        if(text.isEmpty())
            return;

        listItems.add(text);
        listAdapter.notifyDataSetChanged();
    }

    private void syncText(){
        conn.connect();
    }

}

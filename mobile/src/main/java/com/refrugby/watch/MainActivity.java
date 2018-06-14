package com.refrugby.watch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


import com.google.android.gms.tasks.Task;

import com.google.android.gms.wearable.Wearable;



import com.google.android.gms.wearable.Node;
import com.google.android.gms.tasks.Tasks;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void start(View v){
        setContentView(R.layout.watch_settings);
        Spinner awayColour = findViewById(R.id.away_colour);
        awayColour.setSelection(1);
        TextView debug = findViewById(R.id.debug_text);

    }


    public void sendSettings(View v) {
        EditText halfLength = findViewById(R.id.half_length);
        EditText extraTimeLength = findViewById(R.id.extra_time_length);
        EditText ycLength = findViewById(R.id.yc_length);
        Spinner homeColour = findViewById(R.id.home_colour);
        Spinner awayColour = findViewById(R.id.away_colour);

        String serialisedSettings = "half_length:" + halfLength.getText() + "|extra_time_length:" + extraTimeLength.getText() + "|yc_length:" + ycLength.getText() +
                "|home_colour:" + homeColour.getSelectedItem().toString() + "|away_colour:" + awayColour.getSelectedItem().toString() + "|";

        new NewThread("/rrw_settings", serialisedSettings).start();
    }

    class NewThread extends Thread {
        String path;
        String message;

        //Constructor for sending information to the Data Layer//
        NewThread(String p, String m) {
            path = p;
            message = m;
        }

        public void run() {

            //Retrieve the connected devices, known as nodes//
//            TextView debug = findViewById(R.id.debug_text);

            Task<List<Node>> wearableList =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask = Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());
                }

            } catch (ExecutionException exception) {
//                debug.setText(exception.toString());
            } catch (InterruptedException exception) {
//                debug.setText(exception.toString());
            }
        }
    }
}

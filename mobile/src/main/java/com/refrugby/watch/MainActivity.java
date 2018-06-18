package com.refrugby.watch;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
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

    public void showSettings(View v){
        setContentView(R.layout.watch_settings);
        Spinner awayColour = findViewById(R.id.away_colour);
        awayColour.setSelection(1);
    }


    public void sendSettings(View v) {
        EditText halfLength = findViewById(R.id.half_length);
        EditText extraTimeLength = findViewById(R.id.extra_time_length);
        EditText ycLength = findViewById(R.id.yc_length);
        Spinner homeColour = findViewById(R.id.home_colour);
        Spinner awayColour = findViewById(R.id.away_colour);

        String serialisedSettings = "half_length:" + halfLength.getText() + "|extra_time_length:" + extraTimeLength.getText() + "|yc_length:" + ycLength.getText() +
                "|home_colour:" + homeColour.getSelectedItem().toString() + "|away_colour:" + awayColour.getSelectedItem().toString() + "|";

        new MessageThread("/rrw_settings", serialisedSettings).start();
    }

    class MessageThread extends Thread {
        String path;
        String message;

        //Constructor for sending information to the Data Layer//
        MessageThread(String p, String m) {
            path = p;
            message = m;
        }

        public void run() {

            //Retrieve the connected devices, known as nodes//
            Task<List<Node>> wearableList =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask = Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());
                }

            } catch (ExecutionException exception) {
                // catch
            } catch (InterruptedException exception) {
                // catch
            }
        }
    }

    public void rateOnAppStore(View v) {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market back stack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }
}

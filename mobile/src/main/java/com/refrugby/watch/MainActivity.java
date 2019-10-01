package com.refrugby.watch;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import com.google.android.gms.wearable.CapabilityInfo;
import android.os.Handler;
import android.os.ResultReceiver;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.wearable.intent.RemoteIntent;


public class MainActivity extends AppCompatActivity implements CapabilityClient.OnCapabilityChangedListener {
    private static final String TAG = "MainMobileActivity";
    private static final String CHECKING_MESSAGE = "Checking for Rugby Referee Watch on smart watch...\n";
    private static final String NO_DEVICES = "No devices linked to your phone. Please pair an Android Wear OS 2.0 smart watch before using this app.\n";
    private static final String MISSING_ALL_MESSAGE = "Cannot detect Rugby Referee Watch on a connected smart watch, click install to attempt installation.\n";
//    private static final String INSTALLED_SOME_DEVICES_MESSAGE = "Rugby Ref Watch is installed on your smart watch.\n";
    private static final String INSTALLED_ALL_DEVICES_MESSAGE = "Rugby Referee Watch is installed on your smart watch.\n";
    private static final String CAPABILITY_WEAR_APP = "wear_rugby_ref_watch";
    private static final String PLAY_STORE_APP_URI = "market://details?id=com.refrugby.watch";

    // Result from sending RemoteIntent to wear device(s) to open app in play/app store.
    private final ResultReceiver mResultReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            Log.d(TAG, "onReceiveResult: " + resultCode);

            if (resultCode == RemoteIntent.RESULT_OK) {
                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        "Play Store Request to Wear device successful.",
                        Toast.LENGTH_SHORT);
                toast.show();

            } else if (resultCode == RemoteIntent.RESULT_FAILED) {
                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        "Request Failed. Do you have a Wear >2.0 smart watch connected?",
                        Toast.LENGTH_LONG);
                toast.show();

            } else {
                throw new IllegalStateException("Unexpected result " + resultCode);
            }
        }
    };

    private TextView mInformationTextView;
    private Button mRemoteOpenButton;

    private Set<Node> mWearNodesWithApp;
    private List<Node> mAllConnectedNodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInformationTextView = findViewById(R.id.information_text_view);
        mRemoteOpenButton = findViewById(R.id.remote_open_button);

        mInformationTextView.setText(CHECKING_MESSAGE);

        mRemoteOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPlayStoreOnWearDevicesWithoutApp();
            }
        });

        TextView introTextView = findViewById(R.id.intro_text);
        introTextView.setMovementMethod(new ScrollingMovementMethod());

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

        Wearable.getCapabilityClient(this).removeListener(this, CAPABILITY_WEAR_APP);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

        Wearable.getCapabilityClient(this).addListener(this, CAPABILITY_WEAR_APP);

        // Initial request for devices with our capability, aka, our Wear app installed.
        findWearDevicesWithApp();

        // Initial request for all Wear devices connected (with or without our capability).
        // Additional Note: Because there isn't a listener for ALL Nodes added/removed from network
        // that isn't deprecated, we simply update the full list when the Google API Client is
        // connected and when capability changes come through in the onCapabilityChanged() method.
        findAllWearDevices();
    }

    /*
     * Updates UI when capabilities change (install/uninstall wear app).
     */
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(TAG, "onCapabilityChanged(): " + capabilityInfo);

        mWearNodesWithApp = capabilityInfo.getNodes();

        // Because we have an updated list of devices with/without our app, we need to also update
        // our list of active Wear devices.
        findAllWearDevices();

        verifyNodeAndUpdateUI();
    }

    private void findWearDevicesWithApp() {
        Log.d(TAG, "findWearDevicesWithApp()");

        Task<CapabilityInfo> capabilityInfoTask = Wearable.getCapabilityClient(this)
                .getCapability(CAPABILITY_WEAR_APP, CapabilityClient.FILTER_ALL);

        capabilityInfoTask.addOnCompleteListener(new OnCompleteListener<CapabilityInfo>() {
            @Override
            public void onComplete(Task<CapabilityInfo> task) {

                if (task.isSuccessful()) {
                    Log.d(TAG, "Capability request succeeded.");

                    CapabilityInfo capabilityInfo = task.getResult();
                    mWearNodesWithApp = capabilityInfo.getNodes();

                    Log.d(TAG, "Capable Nodes: " + mWearNodesWithApp);

                    verifyNodeAndUpdateUI();

                } else {
                    Log.d(TAG, "Capability request failed to return any results.");
                }
            }
        });
    }

    private void findAllWearDevices() {
        Log.d(TAG, "findAllWearDevices()");

        Task<List<Node>> NodeListTask = Wearable.getNodeClient(this).getConnectedNodes();

        NodeListTask.addOnCompleteListener(new OnCompleteListener<List<Node>>() {
            @Override
            public void onComplete(Task<List<Node>> task) {

                if (task.isSuccessful()) {
                    Log.d(TAG, "Node request succeeded.");
                    mAllConnectedNodes = task.getResult();

                } else {
                    Log.d(TAG, "Node request failed to return any results.");
                }

                verifyNodeAndUpdateUI();
            }
        });
    }

    private void verifyNodeAndUpdateUI() {
        Log.d(TAG, "verifyNodeAndUpdateUI()");

        if ((mWearNodesWithApp == null) || (mAllConnectedNodes == null)) {
            Log.d(TAG, "Waiting on Results for both connected nodes and nodes with app");

        } else if (mAllConnectedNodes.isEmpty()) {
            Log.d(TAG, NO_DEVICES);
            mInformationTextView.setText(NO_DEVICES);
            mRemoteOpenButton.setVisibility(View.INVISIBLE);

        } else if (mWearNodesWithApp.isEmpty()) {
            Log.d(TAG, MISSING_ALL_MESSAGE);
            mInformationTextView.setText(MISSING_ALL_MESSAGE);
            mRemoteOpenButton.setVisibility(View.VISIBLE);

        }
//        else if (mWearNodesWithApp.size() < mAllConnectedNodes.size()) {
//
//            String installMessage =
//                    String.format(INSTALLED_SOME_DEVICES_MESSAGE, mWearNodesWithApp);
//            Log.d(TAG, installMessage);
//            mInformationTextView.setText(installMessage);
//            mRemoteOpenButton.setVisibility(View.INVISIBLE);
//            findViewById(R.id.rate).setVisibility(View.VISIBLE);
//            findViewById(R.id.start_button).setVisibility(View.VISIBLE);
//
//        }
        else {
            String installMessage = String.format(INSTALLED_ALL_DEVICES_MESSAGE, mWearNodesWithApp);
            Log.d(TAG, installMessage);
            mInformationTextView.setText(installMessage);
            mRemoteOpenButton.setVisibility(View.INVISIBLE);
            findViewById(R.id.rate).setVisibility(View.VISIBLE);
            findViewById(R.id.start_button).setVisibility(View.VISIBLE);

        }
    }

    private void openPlayStoreOnWearDevicesWithoutApp() {
        Log.d(TAG, "openPlayStoreOnWearDevicesWithoutApp()");

        // Create a List of Nodes (Wear devices) without your app.
        ArrayList<Node> nodesWithoutApp = new ArrayList<>();
        if (mAllConnectedNodes == null || mAllConnectedNodes.isEmpty()){
            return;
        }

        for (Node node : mAllConnectedNodes) {
            if (!mWearNodesWithApp.contains(node)) {
                nodesWithoutApp.add(node);
            }
        }

        if (!nodesWithoutApp.isEmpty()) {
            Log.d(TAG, "Number of nodes without app: " + nodesWithoutApp.size());

            Intent intent =
                    new Intent(Intent.ACTION_VIEW)
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                            .setData(Uri.parse(PLAY_STORE_APP_URI));

            for (Node node : nodesWithoutApp) {
                RemoteIntent.startRemoteActivity(
                        getApplicationContext(),
                        intent,
                        mResultReceiver,
                        node.getId());
            }
        }
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

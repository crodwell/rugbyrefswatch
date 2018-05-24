package nz.co.rodwell.rugbyrefswatch;

import android.os.CountDownTimer;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.wear.widget.CircularProgressLayout;
import android.support.wearable.activity.WearableActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class YellowCardActivity extends WearableActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_select);


        Intent i = getIntent();
        // Receiving the Data
        final String side = i.getStringExtra("side");
        Log.e("YC",  "side: " + side);

        final RecyclerView recyclerView = findViewById(R.id.player_recycler_view);
        recyclerView.setHasFixedSize(true);
        GridLayoutManager mLayoutManager = new GridLayoutManager(this, 4);
        recyclerView.setLayoutManager(mLayoutManager);

        recyclerView.setAdapter(new RecyclerViewAdapter(new RecyclerViewAdapter.AdapterCallback() {
            @Override
            public void onItemClicked(final String player) {
                setContentView(R.layout.confirm_yc);
                TextView txt = findViewById(R.id.confirmMsg);
                txt.setText("Starting Yellow Card Timer for\n" + player);
                CircularProgressLayout mCircularProgress = findViewById(R.id.circular_progress);
                mCircularProgress.setOnTimerFinishedListener(new  CircularProgressLayout.OnTimerFinishedListener(){
                    @Override
                    public void onTimerFinished(CircularProgressLayout layout){
                        Intent i = new Intent();
                        // Sending param key as 'website' and value as 'androidhive.info'
                        i.putExtra("player", player);
                        i.putExtra("side", side);

                        setResult(RESULT_OK,i);
                        finish();
                    }
                });
                mCircularProgress.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
                mCircularProgress.setTotalTime(5000);
                mCircularProgress.startTimer();
            }
        }, new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"}));

    }

}

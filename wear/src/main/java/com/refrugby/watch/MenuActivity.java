package com.refrugby.watch;

import android.support.wear.widget.CircularProgressLayout;
import android.support.wearable.activity.WearableActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;


public class MenuActivity extends WearableActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        Intent i = getIntent();
        final Boolean finalPeriod = i.getBooleanExtra("finalPeriod", false);
        if (finalPeriod){
            Button period = findViewById(R.id.next_period);
            period.setVisibility(View.GONE);
        }

    }

    public void cancelMenu(View v){
        setResult(RESULT_CANCELED);
        finish();
    }

    public void nextPeriod(View v) {
        setContentView(R.layout.confirm_period);
        TextView txt = findViewById(R.id.confirmMsg);
        txt.setText(R.string._startingNextPeriod);
        CircularProgressLayout mCircularProgress = findViewById(R.id.circular_progress);
        mCircularProgress.setOnTimerFinishedListener(new  CircularProgressLayout.OnTimerFinishedListener(){
            @Override
            public void onTimerFinished(CircularProgressLayout layout){
                Intent i = new Intent();
                i.putExtra("action","period");
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

    public void restartMatch(View v) {
        setContentView(R.layout.confirm_period);
        TextView txt = findViewById(R.id.confirmMsg);
        txt.setText(R.string._restartingMatch);
        CircularProgressLayout mCircularProgress = findViewById(R.id.circular_progress);
        mCircularProgress.setOnTimerFinishedListener(new  CircularProgressLayout.OnTimerFinishedListener(){
            @Override
            public void onTimerFinished(CircularProgressLayout layout){
                Intent i = new Intent();
                i.putExtra("action","restart");
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

    public void quitApp(View v){
        setContentView(R.layout.confirm_period);
        TextView txt = findViewById(R.id.confirmMsg);
        txt.setText(R.string._quittingApp);
        CircularProgressLayout mCircularProgress = findViewById(R.id.circular_progress);
        mCircularProgress.setOnTimerFinishedListener(new  CircularProgressLayout.OnTimerFinishedListener(){
            @Override
            public void onTimerFinished(CircularProgressLayout layout){
                Intent i = new Intent();
                i.putExtra("action","quit");
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

}

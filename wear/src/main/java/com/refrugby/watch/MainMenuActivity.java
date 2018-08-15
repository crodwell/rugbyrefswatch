package com.refrugby.watch;

import android.support.wear.widget.CircularProgressLayout;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


import java.util.concurrent.TimeUnit;

import java.util.ArrayList;


public class MainMenuActivity extends WearableActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        Intent i = getIntent();
        WearableRecyclerView recyclerView = findViewById(R.id.main_menu_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setEdgeItemsCenteringEnabled(true);
        recyclerView.setLayoutManager(new WearableLinearLayoutManager(this));

        ArrayList<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(new MenuItem(R.drawable.back_icon, "Back"));
        if (!i.getBooleanExtra("finalPeriod", false)){
            menuItems.add(new MenuItem(R.drawable.next_period_icon,"Next Period"));
        }
        menuItems.add(new MenuItem(R.drawable.summary_icon,"Match Summary"));
        menuItems.add(new MenuItem(R.drawable.restart_match_icon,"Restart Match"));
        menuItems.add(new MenuItem(R.drawable.quit_app_icon, "Quit App"));

        recyclerView.setAdapter(new MainMenuAdapter(this, menuItems, new MainMenuAdapter.AdapterCallback() {
            @Override
            public void onItemClicked(final Integer menuPosition) {
                switch (menuPosition){
                    case 0:  cancelMenu(); break;
                    case 1:  nextPeriod(); break;
                    case 2:  showSummary(); break;
                    case 3:  restartMatch(); break;
                    case 4:  quitApp(); break;
                    default : cancelMenu();
                }
            }
        }));
    }

    public void cancelSummary(View v){
        cancelMenu();
    }


    public void cancelMenu(){
        setResult(RESULT_CANCELED);
        finish();
    }

    public void nextPeriod() {
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

    public void showSummary() {
        setContentView(R.layout.match_summary);

        ArrayList<Penalty> homePens = getIntent().getParcelableArrayListExtra("homePens");
        ArrayList<Penalty> awayPens = getIntent().getParcelableArrayListExtra("awayPens");

        for (Penalty item:homePens) {
            int resourceID = getResources().getIdentifier("home_pen_" + item.period, "id", getPackageName());
            TextView penTxt = findViewById(resourceID);
            Log.d("home", item.currentTime.toString());

            penTxt.setText(penTxt.getText().toString() + String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(item.currentTime), TimeUnit.MILLISECONDS.toSeconds(item.currentTime)) + "\n");
        }
        for (Penalty item:awayPens) {
            int resourceID = getResources().getIdentifier("away_pen_" + item.period, "id", getPackageName());
            TextView penTxt = findViewById(resourceID);
            Log.d("away", item.currentTime.toString());
            penTxt.setText(penTxt.getText().toString() + String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(item.currentTime), TimeUnit.MILLISECONDS.toSeconds(item.currentTime)) + "\n");
        }
    }

    public void restartMatch() {
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

    public void quitApp(){
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

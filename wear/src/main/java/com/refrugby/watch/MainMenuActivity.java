package com.refrugby.watch;

import androidx.wear.widget.CircularProgressLayout;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


import java.util.Locale;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;


public class MainMenuActivity extends WearableActivity {

    private Integer currentPeriod;
    private Long currentMatchTime;
    private boolean u18;
    public boolean countdownMatchClock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        setAmbientEnabled(); // Enables Always-on
        Intent i = getIntent();
        currentPeriod = i.getIntExtra("currentPeriod", 0);
        currentMatchTime = i.getLongExtra("currentMatchTime", 0);
        u18 = i.getBooleanExtra("u18", false);
        countdownMatchClock = i.getBooleanExtra("countdownMatchClock", false);
        WearableRecyclerView recyclerView = findViewById(R.id.main_menu_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setEdgeItemsCenteringEnabled(true);
        recyclerView.setLayoutManager(new WearableLinearLayoutManager(this));

        ArrayList<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(new MenuItem("back", R.drawable.back_icon, "Back"));
        if (!i.getBooleanExtra("finalPeriod", false)){
            menuItems.add(new MenuItem("nextPeriod", R.drawable.next_period_icon,"Next Period"));
        }
        menuItems.add(new MenuItem("matchSummary", R.drawable.summary_icon,"Match Summary"));
        if (currentPeriod == 0 && currentMatchTime == 0){
            if (!u18){
                menuItems.add(new MenuItem("switchU18", R.drawable.next_period_icon,"U18 Time (RFU)"));
            } else {
                menuItems.add(new MenuItem("switchU18", R.drawable.next_period_icon,"Adult Match Time"));
            }
        }

        if (currentPeriod == 0 && currentMatchTime == 0){
            if (!countdownMatchClock){
                menuItems.add(new MenuItem("switchCountdownMatchClock", R.drawable.next_period_icon,"Count Down Mode"));
            } else {
                menuItems.add(new MenuItem("switchCountdownMatchClock", R.drawable.next_period_icon,"Count Up Mode"));
            }
        }

        menuItems.add(new MenuItem("restartMatch", R.drawable.restart_match_icon,"Restart Match"));
        menuItems.add(new MenuItem("quitApp", R.drawable.quit_app_icon, "Quit App"));

        recyclerView.setAdapter(new MainMenuAdapter(menuItems, new MainMenuAdapter.AdapterCallback() {
            @Override
            public void onItemClicked(final String menuId) {
                switch (menuId){
                    case "nextPeriod":  nextPeriod(); break;
                    case "matchSummary":  showSummary(); break;
                    case "switchU18":  switchU18(); break;
                    case "switchCountdownMatchClock":  switchCountDownMatchClock(); break;
                    case "restartMatch":  restartMatch(); break;
                    case "quitApp":  quitApp(); break;
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

    public void switchU18(){
        Intent i = new Intent();
        i.putExtra("action","u18");
        setResult(RESULT_OK,i);
        finish();
    }

    public void switchCountDownMatchClock(){
        Intent i = new Intent();
        i.putExtra("action","countdownMatchClock");
        setResult(RESULT_OK,i);
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

        // Un hide summary sections according to current period.
        for (int i=0 ; i <= currentPeriod; i++) {
            LinearLayout header = findViewById(getResources().getIdentifier("period_" + i + "_header", "id", getPackageName()));
            LinearLayout data = findViewById(getResources().getIdentifier("period_" + i + "_data", "id", getPackageName()));
            header.setVisibility(View.VISIBLE);
            data.setVisibility(View.VISIBLE);
        }

        ArrayList<Penalty> homePens = getIntent().getParcelableArrayListExtra("homePens");
        ArrayList<Penalty> awayPens = getIntent().getParcelableArrayListExtra("awayPens");

        for (Penalty item:homePens) {
            int resourceID = getResources().getIdentifier("home_pen_" + item.period, "id", getPackageName());
            TextView penTxt = findViewById(resourceID);
            Log.d("home", item.currentTime.toString());
            penTxt.setText(penTxt.getText().toString() + (item.yellowCard ? "YC " : "") + String.format(Locale.getDefault(), "%d:%02d", TimeUnit.MILLISECONDS.toMinutes(item.currentTime), TimeUnit.MILLISECONDS.toSeconds(item.currentTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(item.currentTime))) + "\n");
        }
        for (Penalty item:awayPens) {
            int resourceID = getResources().getIdentifier("away_pen_" + item.period, "id", getPackageName());
            TextView penTxt = findViewById(resourceID);
            Log.d("away", item.currentTime.toString());
            penTxt.setText(penTxt.getText().toString() + String.format(Locale.getDefault(), "%d:%02d", TimeUnit.MILLISECONDS.toMinutes(item.currentTime), TimeUnit.MILLISECONDS.toSeconds(item.currentTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(item.currentTime))) + (item.yellowCard ? " YC" : "") +"\n");
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

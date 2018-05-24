package nz.co.rodwell.rugbyrefswatch;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import android.view.View;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import android.content.Intent;
import android.util.Log;
import java.util.List;
import android.widget.ArrayAdapter;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.GridLayoutManager;
import android.support.wear.widget.CircularProgressLayout;

import android.media.MediaPlayer;



public class MainActivity extends WearableActivity {

    private int matchTimerState;
    private CountDownTimer matchTimer;
    private CountDownTimer matchPauseTimer;
    private long currentMatchTime; //milliseconds left in match

    ArrayList<YellowCard> homeYCs = new ArrayList<YellowCard>();
    ArrayList<YellowCard> awayYCs = new ArrayList<YellowCard>();

    //Activity Request Code Constants
    static final int YELLOW_CARD = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();
    }

    public void penCount(View v) {
        TextView txt=(TextView) findViewById(v.getId());
        int currentCount = Integer.parseInt(txt.getText().toString());
        txt.setText(Integer.toString(currentCount + 1));
    }


    public void newHomeYC(View v){
        Intent yellowCard = new Intent(getApplicationContext(), YellowCardActivity.class);
        yellowCard.putExtra("side", "home");
        startActivityForResult(yellowCard, YELLOW_CARD);
    }

    public void newAwayYC(View v){
        Intent yellowCard = new Intent(getApplicationContext(), YellowCardActivity.class);
        yellowCard.putExtra("side", "away");
        startActivityForResult(yellowCard, YELLOW_CARD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == YELLOW_CARD) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a player.
                // The Intent's data Uri identifies which contact was selected.
                String player = data.getExtras().get("player").toString();
                String side = data.getExtras().get("side").toString();
                Log.e("return-side", side);
                if (side == "home"){
                    homeYCs.add(new YellowCard(player));
                } else {
                    awayYCs.add(new YellowCard(player));
                }


                Log.e("player: ", player);
                // Do something with the contact here (bigger example below)
            } else{
                Log.e("YC: ", "Cancelled");
            }
        }
    }

    public void matchTimer(View v) {
        if (matchTimerState == 1) { // Pause Timers
            matchTimer.cancel();

            for (YellowCard item:homeYCs) {
                item.pauseTimer();
            }
            for (YellowCard item:awayYCs) {
                item.pauseTimer();
            }


            matchTimerState = 2;

            matchPauseTimer = new CountDownTimer(300000, 20000) { // vibrate every 20 seconds for 5 mins while match is paused
                public void onTick(long millisUntilFinished) {
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    long[] vibrationPattern = {0, 500, 50, 300};
                    //-1 - don't repeat
                    final int indexInPatternToRepeat = -1;
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                }
                public void onFinish() {
                }
            }.start();




        } else if (matchTimerState == 2) { // Restart Timers
            startMatchClock(currentMatchTime);
            matchTimerState = 1;
            matchPauseTimer.cancel();

//            for (int x=0; x < homeYCsCurrentTimes.size(); x++) {
//                final int y = x;
//                homeYCs.set(x, new CountDownTimer(homeYCsCurrentTimes.get(x), 1000) { // adjust the milli seconds here
//                    public void onTick(long millisUntilFinished) {
//                        homeYCsCurrentTimes.set(y,millisUntilFinished);
//                    }
//
//                    public void onFinish() {
//                    }
//                }.start());
//            }
            for (YellowCard item:homeYCs) {
                item.restartTimer();
            }
            for (YellowCard item:awayYCs) {
                item.restartTimer();
            }


        } else { // Start Timer
            startMatchClock(2400000);
            matchTimerState = 1;
        }
    }

    private void startMatchClock(long millisInFuture){
        final TextView txt = findViewById(R.id.match_clock);
        final TextView homeYcTxt = findViewById(R.id.home_yc);
        final TextView awayYcTxt = findViewById(R.id.away_yc);

        matchTimer = new CountDownTimer(millisInFuture, 1000) {
            public void onTick(long millisUntilFinished) {
                txt.setText(""+String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
                currentMatchTime = millisUntilFinished;

                String homeYCText = new String();
                for (int i = 0; i < homeYCs.size(); i++) {
                    if (i < 3 || homeYCs.size() == 4) {
                        homeYCText += homeYCs.get(i).player + " " + String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(homeYCs.get(i).currentTime),
                                TimeUnit.MILLISECONDS.toSeconds(homeYCs.get(i).currentTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(homeYCs.get(i).currentTime))) + "\n";
                    }
                    if (homeYCs.size() > 4){
                        homeYCText += "+" + Integer.toString(homeYCs.size() - 3);
                    }
                }
                homeYcTxt.setText(homeYCText);

                String awayYCText = new String();
                for (int i = 0; i < awayYCs.size(); i++) {
                    if (i < 3 || awayYCs.size() == 4) {
                        awayYCText += awayYCs.get(i).player + " " + String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(awayYCs.get(i).currentTime),
                                TimeUnit.MILLISECONDS.toSeconds(awayYCs.get(i).currentTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(awayYCs.get(i).currentTime))) + "\n";
                    }
                    if (awayYCs.size() > 4){
                        awayYCText += "+" + Integer.toString(awayYCs.size() - 3);
                    }
                }
                awayYcTxt.setText(awayYCText);

                //get any yellow cards running and print their times
//                String YCText = new String();
//                for (int i = 0; i < homeYCsCurrentTimes.size(); i++) {
//                    if (i < 3 || homeYCsCurrentTimes.size() == 4){
//                        YCText += "" + String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(homeYCsCurrentTimes.get(i)),
//                                TimeUnit.MILLISECONDS.toSeconds(homeYCsCurrentTimes.get(i)) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(homeYCsCurrentTimes.get(i)))) + "\n";
//                    }
//                }
//                if (homeYCsCurrentTimes.size() > 4){
//                    YCText += "+" + Integer.toString(homeYCsCurrentTimes.size() - 3);
//                }
//                homeYcTxt.setText(YCText);
            }

            public void onFinish() {
                txt.setText("Times up!");
            }
        }.start();
    }

}

class YellowCard {
    public String side;
    public String player;
    public Long currentTime;
    private CountDownTimer ycTimer;

    public YellowCard (String player){
        this.player = player;
        createTimer();
    }

    private void createTimer() {
        ycTimer = new CountDownTimer(600000, 1000) { // adjust the milli seconds here
            public void onTick(long millisUntilFinished) {
                currentTime = millisUntilFinished;
            }

            public void onFinish() {
            }
        }.start();
    }

    public void pauseTimer() {
        ycTimer.cancel();
    }

    public void restartTimer() {
        ycTimer = new CountDownTimer(currentTime, 1000) { // adjust the milli seconds here
            public void onTick(long millisUntilFinished) {
                currentTime = millisUntilFinished;
            }

            public void onFinish() {
            }
        }.start();
    }
}



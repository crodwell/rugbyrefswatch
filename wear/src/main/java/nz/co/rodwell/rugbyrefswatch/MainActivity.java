package nz.co.rodwell.rugbyrefswatch;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import android.view.View;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import android.content.Intent;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.graphics.Color;


import android.media.MediaPlayer;



public class MainActivity extends WearableActivity {

    private int matchTimerState;
    private CountDownTimer matchTimer;
    private CountDownTimer matchPauseTimer;
    private long currentMatchTime; //milliseconds left in match
//    private long halfLength = 2400000; // 40 mins
    private long halfLength = 10000;
    private long overtimeLength = 600000;
    private boolean halfTimeHooter;

    ArrayList<YellowCard> homeYCs = new ArrayList<>();
    ArrayList<YellowCard> awayYCs = new ArrayList<>();
    private int activeHomeYCs = 0;
    private int activeAwayYCs = 0;

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
        TextView txt = findViewById(v.getId());
        int currentCount = Integer.parseInt(txt.getText().toString());
        txt.setText(Integer.toString(currentCount + 1));
    }


    public void newHomeYC(View v){
        Intent yellowCard = new Intent(getApplicationContext(), YellowCardActivity.class);
        yellowCard.putExtra("side", "home");
        ArrayList<String> history = new ArrayList<>();
        for (YellowCard item:homeYCs) {
            history.add(item.player);
        }
        yellowCard.putExtra("history", history);
        startActivityForResult(yellowCard, YELLOW_CARD);
    }

    public void newAwayYC(View v){
        Intent yellowCard = new Intent(getApplicationContext(), YellowCardActivity.class);
        yellowCard.putExtra("side", "away");
        ArrayList<String> history = new ArrayList<>();
        for (YellowCard item:awayYCs) {
            history.add(item.player);
        }
        yellowCard.putExtra("history", history);
        startActivityForResult(yellowCard, YELLOW_CARD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == YELLOW_CARD) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a player.
                String player = data.getExtras().get("player").toString();
                String side = data.getExtras().get("side").toString();
                if (side.equals("home")){
                    homeYCs.add(new YellowCard(this, player));
                    activeHomeYCs++;
                } else {
                    awayYCs.add(new YellowCard(this, player));
                    activeAwayYCs++;
                }
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

            for (YellowCard item:homeYCs) {
                if (!item.expired) {
                    item.restartTimer();
                }
            }
            for (YellowCard item:awayYCs) {
                if (!item.expired) {
                    item.restartTimer();
                }
            }


        } else { // Start Timer
            startMatchClock(0);
            matchTimerState = 1;
        }
    }

    public void ackYcCompleted(View v){
        for (YellowCard item:homeYCs) {
            if (item.expired) {
                activeHomeYCs--;
                item.acknowledge();
            }
        }

        for (YellowCard item:awayYCs) {
            if (item.expired) {
                activeAwayYCs--;
                item.acknowledge();
            }
        }



        v.setVisibility(View.GONE);
    }



    private void startMatchClock(long millisInFuture){
        final TextView txt = findViewById(R.id.match_clock);

        final long bigLong = 20000000; // stupidly high limit we will never reach. Let's us run Countdown as Countup. (I didn't like Chronometer package)
        final long startTime = bigLong - millisInFuture;

        matchTimer = new CountDownTimer(startTime, 1000) {
            public void onTick(long millisUntilFinished) {

                currentMatchTime = bigLong - millisUntilFinished;
                txt.setText(""+String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(currentMatchTime),
                        TimeUnit.MILLISECONDS.toSeconds(currentMatchTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentMatchTime))));
                if (currentMatchTime > halfLength && !halfTimeHooter) {
                    txt.setBackgroundColor(Color.RED);
                    txt.setTextColor(Color.WHITE);
                    MediaPlayer mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.hooter);
                    mediaPlayer.setVolume(1.0F, 1.0F);
                    mediaPlayer.start();
                    halfTimeHooter = true;
                }
                printYcStatus();
            }

            public void onFinish() {
            }
        }.start();
    }

    private void printYcStatus(){
        Integer homeYcTextview = 0;
        Integer awayYcTextview = 0;

        if (homeYCs.size() > 0) {
            String homeYCText;
            for (int i = 0; i < homeYCs.size(); i++) {
                int resourceID = getResources().getIdentifier("home_yc_" + Integer.toString(homeYcTextview), "id", getPackageName());
                TextView homeYc = findViewById(resourceID);
                if (homeYCs.get(i).acknowledged){
                    ((TextView)findViewById(R.id.home_yc_0)).setText("");
                    ((TextView)findViewById(R.id.home_yc_1)).setText("");
                    continue;
                }

                if( homeYCs.get(i).expired){
                    homeYc.setTextColor(Color.RED);
                    (findViewById(R.id.ackYc)).setVisibility(View.VISIBLE);
                } else {
                    homeYc.setTextColor(Color.BLACK);
                }
                if (homeYcTextview == 1 && activeHomeYCs > 2) {
                    homeYCText = "+" + Integer.toString(activeHomeYCs - 1);
                    homeYc.setTextColor(Color.BLACK);
                    homeYc.setText(homeYCText);
                }

                if (homeYcTextview < 1 || activeHomeYCs == 2) {
                    homeYCText = homeYCs.get(i).player + " " + String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(homeYCs.get(i).currentTime),
                            TimeUnit.MILLISECONDS.toSeconds(homeYCs.get(i).currentTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(homeYCs.get(i).currentTime))) + "\n";
                    homeYc.setText(homeYCText);
                    homeYcTextview++;
                }

            }
        }

        if (awayYCs.size() > 0) {
            String awayYCText;
            for (int i = 0; i < awayYCs.size(); i++) {
                int resourceID = getResources().getIdentifier("away_yc_" + Integer.toString(awayYcTextview), "id", getPackageName());
                TextView awayYc = findViewById(resourceID);

                if (awayYCs.get(i).acknowledged){
                    ((TextView)findViewById(R.id.away_yc_0)).setText("");
                    ((TextView)findViewById(R.id.away_yc_1)).setText("");
                    continue;
                }

                if( awayYCs.get(i).expired){
                    awayYc.setTextColor(Color.RED);
                    (findViewById(R.id.ackYc)).setVisibility(View.VISIBLE);
                } else {
                    awayYc.setTextColor(Color.BLACK);
                }

                if (awayYcTextview == 1 && activeAwayYCs > 2) { // if we've printed one already
                    awayYCText = "+" + Integer.toString(activeAwayYCs - 1);
                    awayYc.setTextColor(Color.BLACK);
                    awayYc.setText(awayYCText);
                }

                if (awayYcTextview < 1 || activeAwayYCs == 2) { //always print item if first in list or 2 total
                    awayYCText = awayYCs.get(i).player + " " + String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(awayYCs.get(i).currentTime),
                            TimeUnit.MILLISECONDS.toSeconds(awayYCs.get(i).currentTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(awayYCs.get(i).currentTime))) + "\n";
                    awayYc.setText(awayYCText);
                    awayYcTextview++;
                }

            }
        }
    }

}

class YellowCard {
    private Context context;
    public String player;
    public Long currentTime;
    public boolean acknowledged;
    public boolean expired;
    private CountDownTimer ycTimer;
    private MediaPlayer mediaPlayer;

    public YellowCard (Context context, String player){
        this.context = context;
        this.player = player;
        createTimer();
    }

    private void createTimer() {
        ycTimer = new CountDownTimer(30000, 1000) { // adjust the milli seconds here
            public void onTick(long millisUntilFinished) {
                currentTime = millisUntilFinished;
            }

            public void onFinish() {
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.alarm);
                mediaPlayer.setVolume(1.0F, 1.0F);
                mediaPlayer.start();
                expired = true;
            }
        }.start();
    }

    public void pauseTimer() {
        ycTimer.cancel();
    }

    public void acknowledge() {
        this.acknowledged = true;
        this.mediaPlayer.stop();
    }

    public void restartTimer() {
        ycTimer = new CountDownTimer(currentTime, 1000) { // adjust the milli seconds here
            public void onTick(long millisUntilFinished) {
                currentTime = millisUntilFinished;
            }

            public void onFinish() {
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.alarm);
                mediaPlayer.setVolume(1.0F, 1.0F);
                mediaPlayer.start();
                expired = true;
            }
        }.start();
    }
}



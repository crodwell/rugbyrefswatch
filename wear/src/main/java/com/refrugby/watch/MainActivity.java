package com.refrugby.watch;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.graphics.Color;
import android.view.View.OnLongClickListener;
import android.util.Log;
import android.os.Parcel;



import android.media.MediaPlayer;
import org.apache.commons.lang3.StringUtils;


public class MainActivity extends WearableActivity {

    private int matchTimerState;
    private CountDownTimer matchTimer;
    private CountDownTimer matchPauseTimer;
    private long currentMatchTime;

    private int currentPeriod = 0;
    private String[] periodLabels = new String[]{"1st Half", "2nd Half", "Extra Time 1", "Extra Time 2"};
    private long[] periodLengths = new long[]{2400000, 2400000, 600000, 600000};
    private long yellowCardLength = 600000L;

    private boolean halfTimeHooter;

    ArrayList<Penalty> homePens = new ArrayList<>();
    ArrayList<Penalty> awayPens = new ArrayList<>();
    ArrayList<YellowCard> homeYCs = new ArrayList<>();
    ArrayList<YellowCard> awayYCs = new ArrayList<>();
    private int activeHomeYCs = 0;
    private int activeAwayYCs = 0;

    //Activity Request Code Constants
    static final int YELLOW_CARD = 100;
    static final int PERIOD_SELECT = 200;

    private static final Map<String, String> teamBgColours;
    static
    {
        teamBgColours = new HashMap<>();
        teamBgColours.put("Blue", "#1f2287");
        teamBgColours.put("White", "#FFFFFF");
        teamBgColours.put("Red", "#9A1B00");
        teamBgColours.put("Black", "#000000");
        teamBgColours.put("Gold", "#FFC300");
        teamBgColours.put("Green", "#146230");
        teamBgColours.put("Purple", "#421462");
    }

    private static final Map<String, String> teamTextColours;
    static
    {
        teamTextColours = new HashMap<>();
        teamTextColours.put("Blue", "#FFFFFF");
        teamTextColours.put("White", "#000000");
        teamTextColours.put("Red", "#FFFFFF");
        teamTextColours.put("Black", "#FFFFFF");
        teamTextColours.put("Gold", "#000000");
        teamTextColours.put("Green", "#FFFFFF");
        teamTextColours.put("Purple", "#FFFFFF");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAmbientEnabled(); // Enables Always-on
        setContentView(R.layout.activity_main);

        Button homePen = findViewById(R.id.home_pen);
        homePen.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (homePens.size() > 0){
                    homePens.remove(homePens.size() - 1);
                    TextView txt = findViewById(v.getId());
                    txt.setText(Integer.toString(homePens.size()));
                }
                return true;
            }
        });

        Button awayPen = findViewById(R.id.away_pen);
        awayPen.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (awayPens.size() > 0){
                    awayPens.remove(awayPens.size() - 1);
                    TextView txt = findViewById(v.getId());
                    txt.setText(Integer.toString(awayPens.size()));
                }
                return true;
            }
        });



        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);


    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            String serialisedSettings = intent.getExtras().get("message").toString();
            String halfLength = StringUtils.substringBetween(serialisedSettings, "half_length:", "|");
            String extraTimeLength = StringUtils.substringBetween(serialisedSettings, "extra_time_length:", "|");
            String ycLength = StringUtils.substringBetween(serialisedSettings, "yc_length:", "|");
            String homeColour = StringUtils.substringBetween(serialisedSettings, "home_colour:", "|");
            String awayColour = StringUtils.substringBetween(serialisedSettings, "away_colour:", "|");
            periodLengths = new long[]{Long.parseLong(halfLength) * 60000, Long.parseLong(halfLength) * 60000, Long.parseLong(extraTimeLength) * 60000, Long.parseLong(extraTimeLength) * 60000};
            yellowCardLength = Long.parseLong(ycLength) * 60000;
            Log.d("hex", teamBgColours.get(homeColour));
            Log.d("int", Integer.toString(Color.parseColor(teamBgColours.get(homeColour))));
            Log.d("hex", awayColour);
            Button home_pen = findViewById(R.id.home_pen);
            Button away_pen = findViewById(R.id.away_pen);
            home_pen.setBackgroundColor(Color.parseColor(teamBgColours.get(homeColour)));
            home_pen.setTextColor(Color.parseColor(teamTextColours.get(homeColour)));
            away_pen.setBackgroundColor(Color.parseColor(teamBgColours.get(awayColour)));
            away_pen.setTextColor(Color.parseColor(teamTextColours.get(awayColour)));

            restartMatch();
        }
    }

    public void homePen(View v) {
        homePens.add(new Penalty(currentMatchTime, currentPeriod));
        TextView txt = findViewById(R.id.home_pen);
        txt.setText(Integer.toString(homePens.size()));
    }

    public void awayPen(View v) {
        awayPens.add(new Penalty(currentMatchTime, currentPeriod));
        TextView txt = findViewById(R.id.away_pen);
        txt.setText(Integer.toString(awayPens.size()));
    }

    public void homeYC(View v){
        boolean foundExpiredYC = false;
        for (YellowCard item:homeYCs) {
            if (item.expired && !item.acknowledged) {
                activeHomeYCs--;
                item.acknowledge();
                foundExpiredYC = true;
            }
        }
        if (foundExpiredYC) {
            return;
        }


        if (matchTimerState == 0){
            return; // don't allow YC if match hasn't started
        }
        Intent yellowCard = new Intent(getApplicationContext(), YellowCardActivity.class);
        yellowCard.putExtra("side", "home");
        ArrayList<String> history = new ArrayList<>();
        for (YellowCard item:homeYCs) {
            history.add(item.player);
        }
        yellowCard.putExtra("history", history);
        startActivityForResult(yellowCard, YELLOW_CARD);
    }

    public void awayYC(View v){
        boolean foundExpiredYC = false;
        for (YellowCard item : awayYCs) {
            if (item.expired && !item.acknowledged) {
                activeAwayYCs--;
                item.acknowledge();
                foundExpiredYC = true;
            }
        }
        if (foundExpiredYC) {
            return;
        }

        if (matchTimerState == 0){
            return; // don't allow YC if match hasn't started
        }
        Intent yellowCard = new Intent(getApplicationContext(), YellowCardActivity.class);
        yellowCard.putExtra("side", "away");
        ArrayList<String> history = new ArrayList<>();
        for (YellowCard item:awayYCs) {
            history.add(item.player);
        }
        yellowCard.putExtra("history", history);
        startActivityForResult(yellowCard, YELLOW_CARD);
    }

    public void menu(View v){
        Intent menu = new Intent(getApplicationContext(), MainMenuActivity.class);
        menu.putExtra("finalPeriod", (currentPeriod == periodLengths.length -1));
        menu.putExtra("currentPeriod", currentPeriod);
        menu.putParcelableArrayListExtra("homePens", homePens);
        menu.putParcelableArrayListExtra("awayPens", awayPens);
        startActivityForResult(menu, PERIOD_SELECT);
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
                    homeYCs.add(new YellowCard(this, player, yellowCardLength));
                    activeHomeYCs++;
                } else {
                    awayYCs.add(new YellowCard(this, player, yellowCardLength));
                    activeAwayYCs++;
                }
            }
        }

        if (requestCode == PERIOD_SELECT){
            if (resultCode == RESULT_OK) {
                String action = data.getExtras().get("action").toString();
                if (action.equals("quit")){
                    finishAndRemoveTask();
                }
                if (action.equals("period")){
                    currentPeriod++;
                    final TextView infoBar = findViewById(R.id.info_bar);
                    infoBar.setText(periodLabels[currentPeriod] + " (" + Long.toString(periodLengths[currentPeriod] / 60000) + " mins)");

                    for (YellowCard item:homeYCs) {
                        item.pauseTimer();
                    }
                    for (YellowCard item:awayYCs) {
                        item.pauseTimer();
                    }
                    TextView matchClock = findViewById(R.id.match_clock);
                    matchClock.setText("0:00");
                    matchClock.setBackgroundColor(Color.WHITE);
                    matchClock.setTextColor(Color.BLACK);
                    if (matchTimerState > 0) {
                        matchTimer.cancel();
                    }
                    if (matchTimerState == 2) {
                        matchPauseTimer.cancel();
                    }
                    halfTimeHooter = false;
                    matchTimerState = 0;
                    currentMatchTime = 0L;

                }
                if (action.equals("restart")){
                    restartMatch();
                }
            }
        }

    }

    public void matchTimer(View v) {
        switch (matchTimerState) {
            case 1: // Pause Timers
                matchTimer.cancel();

                for (YellowCard item : homeYCs) {
                    item.pauseTimer();
                }
                for (YellowCard item : awayYCs) {
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
                break;

            case 2:  // Restart Timers
                startMatchClock(currentMatchTime);
                matchTimerState = 1;
                matchPauseTimer.cancel();

                for (YellowCard item : homeYCs) {
                    if (!item.expired) {
                        item.restartTimer();
                    }
                }
                for (YellowCard item : awayYCs) {
                    if (!item.expired) {
                        item.restartTimer();
                    }
                }
                break;

            default: // Start Timer
                startMatchClock(0);
                matchTimerState = 1;
                // restart YC timers as this may be start of next period
                for (YellowCard item : homeYCs) {
                    if (!item.expired) {
                        item.restartTimer();
                    }
                }
                for (YellowCard item : awayYCs) {
                    if (!item.expired) {
                        item.restartTimer();
                    }
                }
        }
    }

    public void restartMatch() {
        if (matchTimerState > 0) {
            matchTimer.cancel();
        }
        if (matchTimerState == 2) {
            matchPauseTimer.cancel();
        }

        for (YellowCard item:homeYCs) {
            item.pauseTimer();
        }
        for (YellowCard item:awayYCs) {
            item.pauseTimer();
        }
        homeYCs.clear();
        awayYCs.clear();
        printYcStatus();
        TextView matchClock = findViewById(R.id.match_clock);
        matchClock.setText("0:00");
        matchClock.setBackgroundColor(Color.WHITE);
        matchClock.setTextColor(Color.BLACK);
        halfTimeHooter = false;
        matchTimerState = 0;
        currentPeriod = 0;
        currentMatchTime = 0L;
        TextView infoBar = findViewById(R.id.info_bar);
        infoBar.setText(periodLabels[currentPeriod] + "(" + Long.toString(periodLengths[currentPeriod] / 60000) + " mins)");
        ((TextView)findViewById(R.id.home_pen)).setText("0");
        ((TextView)findViewById(R.id.away_pen)).setText("0");
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
                if (currentMatchTime > periodLengths[currentPeriod] && !halfTimeHooter) {
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
        } else {
            ((TextView)findViewById(R.id.home_yc_0)).setText("");
            ((TextView)findViewById(R.id.home_yc_1)).setText("");
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
//                    (findViewById(R.id.ackYc)).setVisibility(View.VISIBLE);
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
        } else {
            ((TextView)findViewById(R.id.away_yc_0)).setText("");
            ((TextView)findViewById(R.id.away_yc_1)).setText("");
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
    private long yellowCardLength;

    public YellowCard (Context context, String player, long yellowCardLength){
        this.context = context;
        this.player = player;
        this.yellowCardLength = yellowCardLength;
        createTimer();
    }

    private void createTimer() {
        ycTimer = new CountDownTimer(yellowCardLength, 1000) { // adjust the milli seconds here
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

class Penalty implements Parcelable {
    public Long currentTime;
    public int period;

    public Penalty (Long currentTime, int period){
        this.currentTime = currentTime;
        this.period = period;
    }

    /******************************************/
    /********** Parcelable interface **********/
    /******************************************/

    @Override
    public int describeContents() { // (2)
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) // (3)
    {
        out.writeLong(currentTime);
        out.writeInt(period);
    }

    private static Penalty readFromParcel(Parcel in) { // (4)
        Long currentTime = in.readLong();
        int period = in.readInt();
        return new Penalty(currentTime, period);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() // (5)
    {
        public Penalty createFromParcel(Parcel in) // (6)
        {
            return readFromParcel(in);
        }

        public Penalty[] newArray(int size) { // (7)
            return new Penalty[size];
        }
    };
}




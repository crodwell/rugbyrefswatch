package com.refrugby.watch;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.wearable.activity.WearableActivity;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.graphics.Color;
import android.view.View.OnLongClickListener;
import android.util.Log;
import android.os.Parcel;
import android.widget.LinearLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


import android.media.MediaPlayer;
import org.apache.commons.lang3.StringUtils;

import static android.content.Context.VIBRATOR_SERVICE;


public class MainActivity extends WearableActivity {

    private boolean deviceHasSpeaker;
    private boolean deviceHasGps;

    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;
    private Float metersTravelled = 0.0F;


    private int matchTimerState;  // 0 = Initial, 1 = Running, 2 = Paused
    private boolean countdownMatchClock;
    private CountDownTimer matchTimer;
    private CountDownTimer matchPauseTimer;
    private long currentMatchTime;

    private int currentPeriod = 0;
    private boolean u18 = false;
    private String[] periodLabels = new String[]{"1st Half", "2nd Half", "Extra Time 1", "Extra Time 2"};
    private long[] periodLengths = new long[]{(40 * 60000), (40 * 60000), (10 *60000), (10 * 60000)};
    private long yellowCardLength = (10 * 60000L);
    // for testing
    private boolean debug = false;

    private String[] teamColourList = new String[]{"Blue", "White", "Red", "Black", "Gold", "Green", "Purple", "Silver"};

    private boolean halfTimeHooter;

    ArrayList<Penalty> homePens = new ArrayList<>();
    ArrayList<Penalty> awayPens = new ArrayList<>();
    ArrayList<YellowCard> homeYCs = new ArrayList<>();
    ArrayList<YellowCard> awayYCs = new ArrayList<>();
    private int activeHomeYCs = 0;
    private int activeAwayYCs = 0;
    private Integer homeColourCode = 0;
    private Integer awayColourCode = 3;

    //Activity Request Code Constants
    static final int CARD = 100;
    static final int MAIN_MENU = 200;

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
        teamBgColours.put("Silver", "#CCCCCC");
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
        teamTextColours.put("Silver", "#000000");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAmbientEnabled(); // Enables Always-on
        setContentView(R.layout.activity_main);

        checkDeviceHardware();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Log.d("deviceHasSpeaker", Boolean.toString(deviceHasSpeaker));
        Log.d("deviceHasGps", Boolean.toString(deviceHasGps));

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        u18 = sharedPref.getBoolean("u18", false);
        setU18Mode(u18);
        countdownMatchClock = sharedPref.getBoolean("countdownMatchClock", false);
        if (debug) {
            periodLengths = new long[]{60000L, 60000L, 30000L, 30000L};
            yellowCardLength = 30000L;
        }

        restartMatch();

        Button homePen = findViewById(R.id.home_pen);
        homePen.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (homePens.size() > 0){
                    if (!homePens.get(homePens.size() - 1).yellowCard){
                        homePens.remove(homePens.size() - 1);
                        TextView txt = findViewById(v.getId());
                        txt.setText(Integer.toString(homePens.size()));
                        drawPenaltyHistory();
                    }
                }
                return true;
            }
        });

        Button awayPen = findViewById(R.id.away_pen);
        awayPen.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (awayPens.size() > 0){
                    if (!awayPens.get(awayPens.size() - 1).yellowCard) {
                        awayPens.remove(awayPens.size() - 1);
                        TextView txt = findViewById(v.getId());
                        txt.setText(Integer.toString(awayPens.size()));
                        drawPenaltyHistory();
                    }
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
            try {
                setU18Mode(false);
                String serialisedSettings = intent.getExtras().get("message").toString();
                String halfLength = StringUtils.substringBetween(serialisedSettings, "half_length:", "|");
                String extraTimeLength = StringUtils.substringBetween(serialisedSettings, "extra_time_length:", "|");
                String ycLength = StringUtils.substringBetween(serialisedSettings, "yc_length:", "|");
                String homeColour = StringUtils.substringBetween(serialisedSettings, "home_colour:", "|");
                String awayColour = StringUtils.substringBetween(serialisedSettings, "away_colour:", "|");
                periodLengths = new long[]{Long.parseLong(halfLength) * 60000, Long.parseLong(halfLength) * 60000, Long.parseLong(extraTimeLength) * 60000, Long.parseLong(extraTimeLength) * 60000};
                yellowCardLength = Long.parseLong(ycLength) * 60000;

                // hack to get the colour index from phone message
                Integer i = 0;
                while (i < teamColourList.length) {

                    if (homeColour.equals(teamColourList[i])){
                        homeColourCode = i;
                    }
                    if (awayColour.equals(teamColourList[i])){
                        awayColourCode = i;
                    }
                    i++;
                }

                drawTeamColour(R.id.home_pen, homeColourCode);
                drawTeamColour(R.id.away_pen, awayColourCode);

                restartMatch();
            }  catch (NullPointerException exception) {
                // just ignore.
            }


        }
    }

    public void drawTeamColour(Integer targetViewId, Integer colourCode){
        findViewById(targetViewId).setBackgroundColor(Color.parseColor(teamBgColours.get(teamColourList[colourCode])));
        ((TextView)findViewById(targetViewId)).setTextColor(Color.parseColor(teamTextColours.get(teamColourList[colourCode])));
    }

    public void homePen(View v) {
        // if match hasn't started yet, change colour
        if (currentMatchTime == 0 && currentPeriod == 0){
            if (homeColourCode == (teamColourList.length - 1)){
                homeColourCode = 0;
            } else {
                homeColourCode++;
            }
            drawTeamColour(R.id.home_pen, homeColourCode);
            return;
        }

        // check last pen wasn't < 4 seconds ago (prevents accidental double-click)
        if (homePens.size() > 0){
            Penalty lastPen = homePens.get(homePens.size() - 1);
            if (currentPeriod == lastPen.period && currentMatchTime - lastPen.currentTime < 4000){
                return;
            }
        }

        homePens.add(new Penalty(currentMatchTime, currentPeriod, false, false,"home", "", lastKnownLocation));
        ((TextView)findViewById(R.id.home_pen)).setText(Integer.toString(homePens.size()));
        drawPenaltyHistory();
    }

    public void awayPen(View v) {
        // if match hasn't started yet, change colour
        if (currentMatchTime == 0 && currentPeriod == 0){
            if (awayColourCode == (teamColourList.length - 1)){
                awayColourCode = 0;
            } else {
                awayColourCode++;
            }
            drawTeamColour(R.id.away_pen, awayColourCode);
            return;
        }

        // check last pen wasn't < 4 seconds ago (prevents accidental double-click)
        if (awayPens.size() > 0){
            Penalty lastPen = awayPens.get(awayPens.size() - 1);
            if (currentPeriod == lastPen.period && currentMatchTime - lastPen.currentTime < 4000){
                return;
            }
        }
        awayPens.add(new Penalty(currentMatchTime, currentPeriod, false, false,"away", "", lastKnownLocation));
        ((TextView)findViewById(R.id.away_pen)).setText(Integer.toString(awayPens.size()));
        drawPenaltyHistory();
    }

    public void drawPenaltyHistory(){
        ArrayList<Penalty> allPens = new ArrayList<>();
        allPens.addAll(homePens);
        allPens.addAll(awayPens);


        // Sorts Penalties by half, then time, most recent first.
         Collections.sort(allPens, new Comparator<Penalty>() {

            public int compare(Penalty o2, Penalty o1) {

                Integer p1 = o1.getPeriod();
                Integer p2 = o2.getPeriod();
                int sComp = p1.compareTo(p2);

                if (sComp != 0) {
                    return sComp;
                }

                Long t1 = o1.getCurrentTime();
                Long t2 = o2.getCurrentTime();
                return t1.compareTo(t2);
            }});

        LinearLayout linearLayout = findViewById(R.id.penalty_history);
        linearLayout.removeAllViews();
        int i = 0;
        int penaltyWidth = 40;

        for (Penalty item:allPens) {
            i++;
            TextView drawablePen = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            if (linearLayout.getWidth() < ((penaltyWidth + 1) * i)){ // if too big for screen, don't print any more.
                break;
            }


            params.setMargins(0,-6, 1, 0);
            params.height = LinearLayout.LayoutParams.MATCH_PARENT;
            drawablePen.setLayoutParams(params);
            if (item.side == "home") {
                drawablePen.setBackgroundColor(Color.parseColor(teamBgColours.get(teamColourList[homeColourCode])));
                drawablePen.setTextColor(Color.parseColor(teamTextColours.get(teamColourList[homeColourCode])));
            } else {
                drawablePen.setBackgroundColor(Color.parseColor(teamBgColours.get(teamColourList[awayColourCode])));
                drawablePen.setTextColor(Color.parseColor(teamTextColours.get(teamColourList[awayColourCode])));
            }
            drawablePen.setWidth(penaltyWidth);
            drawablePen.setTextSize(16);
            drawablePen.setGravity(Gravity.CENTER);
            drawablePen.setText(String.format(Locale.getDefault(), "%d", TimeUnit.MILLISECONDS.toMinutes(item.currentTime)));

            linearLayout.addView(drawablePen);
        }



    }

    public void homeYC(View v){
        // check list for expired timers, and acknowledge before creating new ones
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
        startActivityForResult(yellowCard, CARD);
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
        startActivityForResult(yellowCard, CARD);
    }

    public void menu(View v){
        Intent menu = new Intent(getApplicationContext(), MainMenuActivity.class);
        menu.putExtra("metersTravelled", metersTravelled);
        menu.putExtra("currentMatchTime", currentMatchTime);
        menu.putExtra("u18", u18);
        menu.putExtra("countdownMatchClock", countdownMatchClock);
        menu.putExtra("finalPeriod", (currentPeriod == periodLengths.length -1));
        menu.putExtra("currentPeriod", currentPeriod);
        menu.putParcelableArrayListExtra("homePens", homePens);
        menu.putParcelableArrayListExtra("awayPens", awayPens);
        startActivityForResult(menu, MAIN_MENU);
    }


    public void drawInitialMatchClock() {
        TextView matchClock = findViewById(R.id.match_clock);
        if (countdownMatchClock) {
            matchClock.setText(String.format(Locale.getDefault(), "%d:%02d", TimeUnit.MILLISECONDS.toMinutes(periodLengths[currentPeriod]),
                    TimeUnit.MILLISECONDS.toSeconds(periodLengths[currentPeriod]) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(periodLengths[currentPeriod]))));
        } else {
            matchClock.setText("0:00");
        }
        matchClock.setBackgroundColor(Color.WHITE);
        matchClock.setTextColor(Color.BLACK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CARD) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a player.
                String player = data.getExtras().get("player").toString();
                String side = data.getExtras().get("side").toString();
                boolean redCard = data.getBooleanExtra("redCard", false);
                boolean yellowCard = data.getBooleanExtra("yellowCard", false);

                if (side.equals("home")){
                    if (yellowCard) {
                        homeYCs.add(new YellowCard(this, player, yellowCardLength, deviceHasSpeaker));
                        activeHomeYCs++;
                    }
                    homePens.add(new Penalty(currentMatchTime, currentPeriod, yellowCard, redCard, "home", player, lastKnownLocation));
                    ((TextView)findViewById(R.id.home_pen)).setText(Integer.toString(homePens.size()));
                } else {
                    if (yellowCard) {
                        awayYCs.add(new YellowCard(this, player, yellowCardLength, deviceHasSpeaker));
                        activeAwayYCs++;
                    }
                    awayPens.add(new Penalty(currentMatchTime, currentPeriod, yellowCard, redCard, "away", player, lastKnownLocation));
                    ((TextView)findViewById(R.id.away_pen)).setText(Integer.toString(awayPens.size()));
                }
                drawPenaltyHistory();
            }

        }

        if (requestCode == MAIN_MENU){
            if (resultCode == RESULT_OK) {
                String action = data.getExtras().get("action").toString();
                if (action.equals("quit")){
                    restartMatch(); // clears timers so they don't carry on after quit
                    finishAndRemoveTask();
                }

                if (action.equals("period")){
                    currentPeriod++;
                    final TextView infoBar = findViewById(R.id.info_bar);
                    infoBar.setText(periodLabels[currentPeriod] + " (" + Long.toString(periodLengths[currentPeriod] / 60000) + " mins)");

                    pauseYellowCardTimers();
                    drawInitialMatchClock();
                    if (matchTimerState > 0) {
                        matchTimer.cancel();
                    }
                    if (matchTimerState == 2) { // if match is paused, kill the vibrate timer
                        matchPauseTimer.cancel();
                    }
                    halfTimeHooter = false;
                    matchTimerState = 0;
                    currentMatchTime = 0L;

                }

                if (action.equals("restart")){
                    restartMatch();
                }

                if (action.equals("u18")){
                    u18 = !u18;
                    SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("u18", u18);
                    editor.apply();
                    setU18Mode(u18);
                    restartMatch();
                }

                if (action.equals("countdownMatchClock")){
                    countdownMatchClock = !countdownMatchClock;
                    SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("countdownMatchClock", countdownMatchClock);
                    editor.apply();
                    restartMatch();
                }
            }
        }

    }

    public void setU18Mode(boolean enabled){
        if (enabled){
            periodLabels = new String[]{"1st Half", "2nd Half"};
            periodLengths = new long[]{(35 * 60000), (35 * 60000)};
            yellowCardLength = (7 * 60000L);
        } else {
            periodLabels = new String[]{"1st Half", "2nd Half", "Extra Time 1", "Extra Time 2"};
            periodLengths = new long[]{(40 * 60000), (40 * 60000), (10 *60000), (10 * 60000)};
            yellowCardLength = (10 * 60000L);
        }
    }

    public void matchTimer(View v) {
        switch (matchTimerState) {
            case 1: // If 1 (running) then pause timers
                pauseYellowCardTimers();
                matchTimer.cancel();
                matchTimerState = 2;

                // vibrate every 20 seconds for up to 5 minutes to remind us match is paused
                matchPauseTimer = new CountDownTimer(300000, 20000) {
                    public void onTick(long millisUntilFinished) {
                        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                        long[] vibrationPattern = {0, 500, 50, 300}; // {delay, vibrate, sleep, vibrate, sleep...
                        //-1 - don't repeat
                        final int indexInPatternToRepeat = -1;
                        vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                        printYcStatus();
                    }

                    public void onFinish() {
                    }
                }.start();

                break;

            case 2: // If 2 (Paused) then restart timers
                startMatchClock(currentMatchTime);
                matchTimerState = 1;
                matchPauseTimer.cancel();
                restartYellowCardTimers();
                break;

            default: // Start Timer
                startMatchClock(0);
                matchTimerState = 1;
                // restart YC timers as this may be start of next period
                restartYellowCardTimers();
        }
    }

    public void pauseYellowCardTimers() {
        for (YellowCard item : homeYCs) {
            item.pauseTimer();
        }
        for (YellowCard item : awayYCs) {
            item.pauseTimer();
        }
    }


    public void restartYellowCardTimers() {
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

    public void restartMatch() {
        if (matchTimerState > 0) {
            matchTimer.cancel();
        }
        if (matchTimerState == 2) {
            matchPauseTimer.cancel();
        }

        metersTravelled = 0.0F;

        pauseYellowCardTimers(); // we kill the timers and overwrite YC objects next

        homePens = new ArrayList<>();
        homeYCs = new ArrayList<>();
        activeHomeYCs = 0;

        awayPens = new ArrayList<>();
        awayYCs = new ArrayList<>();
        activeAwayYCs = 0;

        drawPenaltyHistory();

        printYcStatus();
        drawInitialMatchClock();
        halfTimeHooter = false;
        matchTimerState = 0;
        currentPeriod = 0;
        currentMatchTime = 0L;
        TextView infoBar = findViewById(R.id.info_bar);
        infoBar.setText(periodLabels[currentPeriod] + " (" + Long.toString(periodLengths[currentPeriod] / 60000) + " mins)");
        ((TextView)findViewById(R.id.home_pen)).setText("0");
        ((TextView)findViewById(R.id.away_pen)).setText("0");

    }

    private void startMatchClock(final long millisInFuture){
        final TextView txt = findViewById(R.id.match_clock);

        final long bigLong = 20000000; // stupidly high limit we will never reach. Let's us run Countdown as Count up. (I didn't like Chronometer package)
        final long startTime = bigLong - millisInFuture;
        startTrackLocation();

        matchTimer = new CountDownTimer(startTime, 1000) {
            public void onTick(long millisUntilFinished) {

                currentMatchTime = bigLong - millisUntilFinished;

                if (!countdownMatchClock) {
                    txt.setText(String.format(Locale.getDefault(), "%d:%02d", TimeUnit.MILLISECONDS.toMinutes(currentMatchTime),
                            TimeUnit.MILLISECONDS.toSeconds(currentMatchTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentMatchTime))));
                } else {
                    Long countDownTime = Math.abs(periodLengths[currentPeriod] - currentMatchTime);
                    txt.setText(String.format(Locale.getDefault(), "%d:%02d", TimeUnit.MILLISECONDS.toMinutes(countDownTime),
                            TimeUnit.MILLISECONDS.toSeconds(countDownTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(countDownTime))));
                }

                if (currentMatchTime > periodLengths[currentPeriod] && !halfTimeHooter) {
                    txt.setBackgroundColor(Color.RED);
                    txt.setTextColor(Color.WHITE);
                    if (deviceHasSpeaker) {
                        MediaPlayer mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.hooter);
                        mediaPlayer.setVolume(1.0F, 1.0F);
                        mediaPlayer.start();
                    }
                    halfTimeHooter = true;
                    // Vibrate as well for watches that don't have speakers:
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    long[] vibrationPattern = {0, 3000}; // {delay, vibrate, sleep, vibrate, sleep...
                    //-1 - don't repeat
                    final int indexInPatternToRepeat = -1;
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                }
                yellowCardTickTimers();
                printYcStatus();

                trackDistanceByGps();

            }

            public void onFinish() {
            }
        }.start();
    }

    private void startTrackLocation(){
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
//                for (Location location : locationResult.getLocations()) {
//                    if (location != null) {
//                        Log.d("gps2", location.toString());
//                    }
//                }
            }
        };
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    private void trackDistanceByGps(){
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Get last known location. In some rare situations this can be null.
                if (location != null) {
//                    Log.d("gps", location.toString());
                    if (lastKnownLocation != null){
                        if (lastKnownLocation.getLongitude() != 0 && lastKnownLocation.getLatitude() != 0){
                            metersTravelled += location.distanceTo(lastKnownLocation);
                        }
                    }
//                    Log.d("distance", Float.toString(metersTravelled));
                    lastKnownLocation = location;
                } else {
                    Log.d("gps", "location is null");
                }

            }
        });
    }



    private void yellowCardTickTimers(){
        for (YellowCard item : homeYCs) {
            item.decrementTimer();
        }
        for (YellowCard item : awayYCs) {
            item.decrementTimer();
        }
    }

    private void printYcStatus(){
        Integer homeYcTextView = 0;
        Integer awayYcTextView = 0;

        if (homeYCs.size() > 0) {
            String homeYCText;
            for (int i = 0; i < homeYCs.size(); i++) {

                int resourceID = getResources().getIdentifier("home_yc_" + homeYcTextView, "id", getPackageName());
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
                if (homeYcTextView == 1 && activeHomeYCs > 2) {
                    homeYCText = "+" + (activeHomeYCs - 1);
                    homeYc.setTextColor(Color.BLACK);
                    homeYc.setText(homeYCText);
                }

                if (homeYcTextView < 1 || activeHomeYCs == 2) {
                    homeYCText = homeYCs.get(i).getText();
                    homeYc.setText(homeYCText);
                    homeYcTextView++;
                }

            }
        } else {
            ((TextView)findViewById(R.id.home_yc_0)).setText("");
            ((TextView)findViewById(R.id.home_yc_1)).setText("");
        }

        if (awayYCs.size() > 0) {
            String awayYCText;
            for (int i = 0; i < awayYCs.size(); i++) {

                int resourceID = getResources().getIdentifier("away_yc_" + (awayYcTextView), "id", getPackageName());
                TextView awayYc = findViewById(resourceID);

                if (awayYCs.get(i).acknowledged){
                    ((TextView)findViewById(R.id.away_yc_0)).setText("");
                    ((TextView)findViewById(R.id.away_yc_1)).setText("");
                    continue;
                }

                if( awayYCs.get(i).expired){
                    awayYc.setTextColor(Color.RED);
                } else {
                    awayYc.setTextColor(Color.BLACK);
                }

                if (awayYcTextView == 1 && activeAwayYCs > 2) { // if we've printed one already
                    awayYCText = "+" + (activeAwayYCs - 1);
                    awayYc.setTextColor(Color.BLACK);
                    awayYc.setText(awayYCText);
                }

                if (awayYcTextView < 1 || activeAwayYCs == 2) { //always print item if first in list or 2 total
                    awayYCText = awayYCs.get(i).getText();
                    awayYc.setText(awayYCText);
                    awayYcTextView++;
                }

            }
        } else {
            ((TextView)findViewById(R.id.away_yc_0)).setText("");
            ((TextView)findViewById(R.id.away_yc_1)).setText("");
        }
    }

    public void checkDeviceHardware(){
        PackageManager packageManager = this.getPackageManager();
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        // Check whether the device has a speaker.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                // Check FEATURE_AUDIO_OUTPUT to guard against false positives.
                packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    deviceHasSpeaker = true;
                }
            }
        }

        deviceHasGps = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);

        if (deviceHasGps) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            }
        }


    }

}

class YellowCard {

    private Context context;
    public String player;
    public Long timeRemaining;
    public boolean acknowledged;
    public boolean expired;
    private boolean paused;
    private boolean deviceHasSpeaker;
    private MediaPlayer mediaPlayer;

    public YellowCard (Context context, String player, long yellowCardLength, boolean deviceHasSpeaker){
        this.context = context;
        this.player = player;
        this.timeRemaining = yellowCardLength;
        this.deviceHasSpeaker = deviceHasSpeaker;
    }

    public void decrementTimer(){
        if (this.expired || this.paused){
            return;
        }

        //update time remaining
        this.timeRemaining = this.timeRemaining - 1000L;

        if (this.timeRemaining == 0){
            if (this.deviceHasSpeaker) {
                this.mediaPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.alarm);
                this.mediaPlayer.setVolume(1.0F, 1.0F);
                this.mediaPlayer.start();
            } else {
                // vibrate for watches that don't have speakers
                Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
                long[] vibrationPattern = {0, 1000, 500, 1000}; // {delay, vibrate, sleep, vibrate, sleep...
                final int indexInPatternToRepeat = -1; //-1 - don't repeat
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
            }
            this.expired = true;
        }
    }

    public String getText() {
        return this.player + " " + String.format(Locale.getDefault(), "%d:%02d", TimeUnit.MILLISECONDS.toMinutes(this.timeRemaining),
                TimeUnit.MILLISECONDS.toSeconds(this.timeRemaining) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(this.timeRemaining))) + "\n";
    }

    public void pauseTimer() {
        this.paused = true;
    }

    public void acknowledge() {
        this.acknowledged = true;
        if (this.deviceHasSpeaker) {
            this.mediaPlayer.stop();
        }

    }

    public void restartTimer() {
        this.paused = false;
    }

}


class Penalty implements Parcelable {
    public Long currentTime;
    public int period;
    public boolean yellowCard;
    public boolean redCard;
    public String side;
    public String player;
    public double latitude;
    public double longitude;

    public Penalty (Long currentTime, int period, boolean yellowCard, boolean redCard, String side, String player, Location location){
        this.currentTime = currentTime;
        this.period = period;
        this.yellowCard = yellowCard;
        this.redCard = redCard;
        this.side = side;
        this.player = player;
        if (location != null){
            this.latitude = location.getLatitude();
            this.longitude = location.getLongitude();
        }
    }

    @Override
    public int describeContents() { // (2)
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) // (3)
    {
        out.writeLong(currentTime);
        out.writeInt(period);
        out.writeByte((byte) (yellowCard ? 1 : 0));
        out.writeByte((byte) (redCard ? 1 : 0));
        out.writeString(side);
        out.writeString(player);
        out.writeDouble(latitude);
        out.writeDouble(longitude);
    }

    private static Penalty readFromParcel(Parcel in) { // (4)
        Long currentTime = in.readLong();
        int period = in.readInt();
        boolean yellowCard = in.readByte() != 0;
        boolean redCard = in.readByte() != 0;
        String side = in.readString();
        String player = in.readString();
        Location location = new Location("");
        location.setLatitude(in.readDouble());
        location.setLongitude(in.readDouble());
        return new Penalty(currentTime, period, yellowCard, redCard, side, player, location);
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

    public int getPeriod() {
        return period;
    }

    public Long getCurrentTime() {
        return currentTime;
    }
}




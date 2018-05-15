package nz.co.rodwell.rugbyrefswatch;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import android.view.View;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.media.MediaPlayer;



public class MainActivity extends WearableActivity {

    private int matchTimerState;
    private CountDownTimer matchTimer;
    private CountDownTimer matchPauseTimer;
    private long currentMatchTime; //milliseconds left in match

    ArrayList<CountDownTimer> homeYCs = new ArrayList<CountDownTimer>();
    ArrayList<Long> homeYCsCurrentTimes = new ArrayList<Long>();
    ArrayList<CountDownTimer> awayYCs = new ArrayList<CountDownTimer>();
    ArrayList<Long> awayYCsCurrentTimes = new ArrayList<Long>();



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
        final int thisYC = homeYCs.size();
        homeYCsCurrentTimes.add(600000L);
        homeYCs.add(thisYC, new CountDownTimer(600000, 1000) { // adjust the milli seconds here
            public void onTick(long millisUntilFinished) {
                homeYCsCurrentTimes.set(thisYC,millisUntilFinished);
            }

            public void onFinish() {
            }
        }.start());
    }

    public void matchTimer(View v) {
        if (matchTimerState == 1) { // Pause Timers
            matchTimer.cancel();

            for (CountDownTimer item:homeYCs) {
                item.cancel();
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

            for (int x=0; x < homeYCsCurrentTimes.size(); x++) {
                final int y = x;
                homeYCs.set(x, new CountDownTimer(homeYCsCurrentTimes.get(x), 1000) { // adjust the milli seconds here
                    public void onTick(long millisUntilFinished) {
                        homeYCsCurrentTimes.set(y,millisUntilFinished);
                    }

                    public void onFinish() {
                    }
                }.start());
            }



        } else { // Start Timer
            startMatchClock(2400000);
            matchTimerState = 1;
        }
    }

    private void startMatchClock(long millisInFuture){
        final TextView txt = (TextView) findViewById(R.id.match_clock);
        final TextView homeYcTxt = (TextView) findViewById(R.id.home_yc);
        final TextView awayYcTxt = (TextView) findViewById(R.id.away_yc);

        matchTimer = new CountDownTimer(millisInFuture, 1000) {
            public void onTick(long millisUntilFinished) {
                txt.setText(""+String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
                currentMatchTime = millisUntilFinished;

                //get any yellow cards running and print their times
                String YCText = new String();
                for (int i = 0; i < homeYCsCurrentTimes.size(); i++) {
                    if (i < 3 || homeYCsCurrentTimes.size() == 4){
                        YCText += "" + String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(homeYCsCurrentTimes.get(i)),
                                TimeUnit.MILLISECONDS.toSeconds(homeYCsCurrentTimes.get(i)) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(homeYCsCurrentTimes.get(i)))) + "\n";
                    }
                }
                if (homeYCsCurrentTimes.size() > 4){
                    YCText += "+" + Integer.toString(homeYCsCurrentTimes.size() - 3);
                }
                homeYcTxt.setText(YCText);
            }

            public void onFinish() {
                txt.setText("Times up!");
            }
        }.start();
    }
}

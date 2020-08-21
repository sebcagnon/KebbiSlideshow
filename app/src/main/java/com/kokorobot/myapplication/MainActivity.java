package com.kokorobot.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ViewFlipper;

import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventCallback;
import com.nuwarobotics.service.agent.VoiceEventCallback;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private static final String TAG = "KEBBISAMPLE";

    private NuwaRobotAPI mRobot;
    private ViewFlipper mViewFlipper;
    private String mState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        getSupportActionBar().hide();
        //Remove notification bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        // robot SDK を開始します
        mState = "init";
        mRobot = new NuwaRobotAPI(this, new IClientId(TAG));
        registerRobotCallbacks();
        mViewFlipper = findViewById(R.id.view_flipper);
        findViewById(R.id.slide_1_start_btn).setOnClickListener(this);
        super.onResume();
    }

    private void registerRobotCallbacks() {
        mRobot.registerRobotEventListener(new RobotEventCallback() {
            @Override
            public void onWikiServiceStart() {
                // Robot SDK は開始できましたけど、モーションを使える前に2-3秒待たなきゃならない。。。
                String[] ttsData = getString(R.string.slide_1_tts).split("/");
                mRobot.startTTS(ttsData[0]);
                mRobot.motionPlay(ttsData[1], false);
            }
        });

        mRobot.registerVoiceEventListener(new VoiceEventCallback() {
            @Override
            public void onTTSComplete(boolean isError) {
                // TTS が終わったら、次のスライドに行く
                if (mState == "started")
                    // 1000ms を待ちます
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            nextSlide();
                        }
                    }, 1000);
            }
        });
    }

    @Override
    public void onClick(View view) {
        mState = "started";
        nextSlide();
    }

    private void nextSlide() {
        // mViewFlipper.showNext を出来るため、Ui Threadを使わなきゃなりません
        // nextSlideはonTTSCompleteからやばれて、onTTSCompleteはロボットのthreadにある
        // なので、runOnUiThreadを使います
        runOnUiThread(() -> {
            mViewFlipper.showNext();
            if (mViewFlipper.getDisplayedChild() == 0) {
                // 最初に戻りましたので、終わり！
                mState = "finished";
            }
            String viewTag = (String) mViewFlipper.getCurrentView().getTag();
            // viewTag = "話す事/モーション名" => split で分けます
            String[] ttsData = viewTag.split("/");
            Log.d(TAG, ttsData[0]);
            Log.d(TAG, ttsData[1]);
            // motion を実行
            mRobot.motionPlay(ttsData[1], false);
            // tts を実行 => TTS が終わったら、onTTSCompleteへ行きます
            mRobot.startTTS(ttsData[0]);
        });
    }
}
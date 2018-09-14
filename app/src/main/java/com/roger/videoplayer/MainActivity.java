package com.roger.videoplayer;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback {
    private static final String TAG = "VideoPlayer";
    public static final float SHOW_SCALE = 16 * 1.0f / 9;
    private String mVideoUrl =
            "https://s3-ap-northeast-1.amazonaws.com/mid-exam/Video/taeyeon.mp4";
//     private String mVideoUrl =
//            "https://s3-ap-northeast-1.amazonaws.com/mid-exam/Video/protraitVideo.mp4";
    private int mRewindAndForwardTime = 15000;
    private int mControllerDisappearTime = 3000;
    private RelativeLayout mVideoPlayerRelativeLayout;
    private ConstraintLayout mControllerConstraintLayout;
    private TextView mCurrentTimeTextView;
    private TextView mTotalTimeTextView;
    private SeekBar mVideoProgressSeekBar;
    private ImageButton mMuteButton;
    private ImageButton mRewindButton;
    private ImageButton mPlayPauseButton;
    private ImageButton mForwardButton;
    private ImageButton mFullscreenButton;
    private SurfaceView mVideoPlayerSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private MediaPlayer mMediaPlayer;
    private int mCurrentPosition = 0;
    private int mVideoMaxTime;
    private boolean isPlaying = true;
    private boolean isVideoExist = true;
    private boolean isMute = false;
    private boolean isSeekBarTouched = false;
    private boolean isLand;
    private int mScreenWidth;
    private int mScreenHeight;
    private DisplayMetrics mDisplayMetrics;

    private Handler mControllerHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setStatusBar();

        mVideoPlayerRelativeLayout = findViewById(R.id.relativeLayout_video_player);
        mControllerConstraintLayout = findViewById(R.id.constraintLayout_controller);
        mCurrentTimeTextView = findViewById(R.id.textView_current_time);
        mTotalTimeTextView = findViewById(R.id.textView_total_time);
        mVideoProgressSeekBar = findViewById(R.id.seekBar_video_progress);
        mMuteButton = findViewById(R.id.imageButton_mute_unmute);
        mRewindButton = findViewById(R.id.imageButton_rewind);
        mPlayPauseButton = findViewById(R.id.imageButton_play_pause);
        mForwardButton = findViewById(R.id.imageButton_forward);
        mFullscreenButton = findViewById(R.id.imageButton_fullscreen_exit_fullscreen);
        mVideoPlayerSurfaceView = findViewById(R.id.surfaceView_video_player);

        mVideoPlayerRelativeLayout.setOnClickListener(this);
        mMuteButton.setOnClickListener(this);
        mRewindButton.setOnClickListener(this);
        mPlayPauseButton.setOnClickListener(this);
        mForwardButton.setOnClickListener(this);
        mFullscreenButton.setOnClickListener(this);

        mVideoProgressSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        mSurfaceHolder = mVideoPlayerSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mDisplayMetrics = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        mScreenWidth = mDisplayMetrics.widthPixels;
        mScreenHeight = mDisplayMetrics.heightPixels;
    }

    private void setStatusBar() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //4.4
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //5.0
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);//calculateStatusColor(Color.WHITE, (int) alphaValue)
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.relativeLayout_video_player:
                showOrHideController();
                break;
            case R.id.imageButton_mute_unmute:
                openOrCloseVolume();
                break;
            case R.id.imageButton_rewind:
                rewind();
                break;
            case R.id.imageButton_play_pause:
                playOrPause();
                break;
            case R.id.imageButton_forward:
                forward();
                break;
            case R.id.imageButton_fullscreen_exit_fullscreen:
                changeOrientation();
                break;
            default:
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPlay(mCurrentPosition);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isVideoExist = false;
        if (mMediaPlayer.isPlaying()) {
            mCurrentPosition = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.stop();
        }
    }

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeekBarTouched = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeekBarTouched = false;
            int progress = seekBar.getProgress();
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.seekTo(progress);
            }
        }
    };

    private void startPlay(final int playTime) {
        isVideoExist = true;
        mMediaPlayer.setDisplay(mSurfaceHolder);
        try {
            mMediaPlayer.setDataSource(mVideoUrl);
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    changeVideoSize();
                    mMediaPlayer.seekTo(playTime);
                    mMediaPlayer.start();
                    mVideoMaxTime = mMediaPlayer.getDuration();
                    mVideoProgressSeekBar.setMax(mVideoMaxTime);
                    mTotalTimeTextView.setText(milliSecondsToTimer(mVideoMaxTime));
                    new Thread() {
                        @Override
                        public void run() {
                            while (isVideoExist) {
                                if (!isSeekBarTouched) {
                                    mCurrentPosition = mMediaPlayer.getCurrentPosition();
                                    mVideoProgressSeekBar.setProgress(mCurrentPosition);
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        setCurrentTimeTextView();
                                    }
                                });
                                try {
                                    sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }.start();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Log.d(TAG, "onBufferingUpdate: " + percent);
                int secondaryProgress = mVideoMaxTime * percent / 100;
                mVideoProgressSeekBar.setSecondaryProgress(secondaryProgress);
            }
        });

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mMediaPlayer.pause();
                mCurrentPosition = 0;
                mMediaPlayer.seekTo(mCurrentPosition);
                mVideoProgressSeekBar.setProgress(mCurrentPosition);
                setCurrentTimeTextView();
                playOrPause();
            }
        });
    }

    private void  changeVideoSize() {
        float area;
        int height;

        if (!isLand) {
            height = (int) (mScreenWidth / SHOW_SCALE);
            area = SHOW_SCALE;
        } else {
            height = mScreenHeight;
            area = mScreenWidth / mScreenHeight;
        }

        RelativeLayout.LayoutParams layoutParams =
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        mVideoPlayerSurfaceView.setLayoutParams(layoutParams);

        int mediaWidth = mMediaPlayer.getVideoWidth();
        int mediaHeight = mMediaPlayer.getVideoHeight();

        float media = mediaWidth * 1.0f / mediaHeight;

        RelativeLayout.LayoutParams layoutParamsSv;

        if (area > media) {
            int svWidth = (int) (height * media);
            layoutParamsSv = new RelativeLayout.LayoutParams(svWidth, height);
            layoutParamsSv.addRule(RelativeLayout.CENTER_IN_PARENT);
            mVideoPlayerSurfaceView.setLayoutParams(layoutParamsSv);
        }

        if (area < media) {
            int svHeight = mScreenWidth;
//            int svHeight = (int) (mScreenWidth / media);
            layoutParamsSv = new RelativeLayout.LayoutParams(mScreenWidth, svHeight);
            layoutParamsSv.addRule(RelativeLayout.CENTER_IN_PARENT);
            mVideoPlayerSurfaceView.setLayoutParams(layoutParamsSv);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        isLand = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        mScreenWidth = mDisplayMetrics.widthPixels;
        mScreenHeight = mDisplayMetrics.heightPixels;

        changeVideoSize();
    }

    private void changeOrientation() {
        if (Configuration.ORIENTATION_LANDSCAPE == this.getResources()
                .getConfiguration().orientation) {
            // 回到直向
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            
            mFullscreenButton.setImageDrawable(getDrawable(R.drawable.ic_fullscreen_black_24dp));
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mControllerHandler.removeCallbacks(controllerRunnable);
        } else {
            // 回到橫向
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            mFullscreenButton.setImageDrawable(getDrawable(R.drawable.ic_fullscreen_exit_black_24dp));
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mControllerHandler.postDelayed(controllerRunnable, mControllerDisappearTime);
        }
    }

    private  void showOrHideController() {
        if (isLand) {
            showController();
            mControllerHandler.removeCallbacks(controllerRunnable);
            mControllerHandler.postDelayed(controllerRunnable, mControllerDisappearTime);
        }
    }

    Runnable controllerRunnable = new Runnable() {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideController();
                }
            });
        }
    };


    private void showController() {
        mControllerConstraintLayout.setVisibility(View.VISIBLE);
    }

    private void hideController() {
        mControllerConstraintLayout.setVisibility(View.GONE);
    }

    private void playOrPause() {
        if (isPlaying) {
            mPlayPauseButton.setImageDrawable(getDrawable(R.drawable.ic_play_arrow_black_24dp));
            isPlaying = false;
            mMediaPlayer.pause();
        } else {
            mPlayPauseButton.setImageDrawable(getDrawable(R.drawable.ic_pause_black_24dp));
            isPlaying = true;
            mMediaPlayer.start();
        }
    }

    private void rewind() {
        int countTime = mCurrentPosition - mRewindAndForwardTime;
        if (countTime <= 0) {
            countTime = 0;
        }
        mCurrentPosition = countTime;
        mMediaPlayer.seekTo(mCurrentPosition);
        mVideoProgressSeekBar.setProgress(mCurrentPosition);
        setCurrentTimeTextView();
    }

    private void forward() {
        int countTime = mCurrentPosition + mRewindAndForwardTime;
        if (countTime >= mVideoMaxTime) {
            countTime = mVideoMaxTime;
        }
        mCurrentPosition = countTime;
        mMediaPlayer.seekTo(mCurrentPosition);
        mVideoProgressSeekBar.setProgress(mCurrentPosition);
        setCurrentTimeTextView();
    }

    private void openOrCloseVolume() {
        if (isMute) {
            isMute = false;
            mMuteButton.setImageDrawable(getDrawable(R.drawable.ic_volume_off_black_24dp));
            mMediaPlayer.setVolume(1, 1);
        } else {
            isMute = true;
            mMuteButton.setImageDrawable(getDrawable(R.drawable.ic_volume_mute_black_24dp));
            mMediaPlayer.setVolume(0, 0);
        }
    }

    private void setCurrentTimeTextView() {
        mCurrentTimeTextView.setText(milliSecondsToTimer(mCurrentPosition));
    }

    private String milliSecondsToTimer(int milliseconds) {
        String finalTimerString = "";
        String secondsString;

        // Convert total duration into time
        int hours = milliseconds / (1000 * 60 * 60);
        int minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }
        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        mMediaPlayer.release();
    }
}

package com.tech42.callrecorder.views;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.View;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tech42.callrecorder.R;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;


public class AudioPlayer extends Activity {
    private ImageView playPauseImg;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private double startTime = 0;
    private Handler durationHandler = new Handler();;
    private int forwardTime = 5000;
    private int backwardTime = 5000;
    private double timeElapsed = 0, finalTime = 0;
    private SeekBar seekbar;
    private TextView startTimeView, stopTimeView, fileName;
    private String path, fileId;
    public static int oneTimeOnly = 0;
    int count = 0;
    private Button share, cancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        playPauseImg = (ImageView) findViewById(R.id.play_pause);

        startTimeView = (TextView)findViewById(R.id.textView2);
        stopTimeView = (TextView)findViewById(R.id.textView3);
        fileName = (TextView)findViewById(R.id.file_name);
        share = (Button) findViewById(R.id.share);
        cancel = (Button) findViewById(R.id.cancel);

        Bundle bundle = getIntent().getExtras();
        path = bundle.getString("path");
        fileName.setText(bundle.getString("name"));
        fileId = bundle.getString("fileId");

        seekbar = (SeekBar)findViewById(R.id.seekBar);
        seekbar.setClickable(false);
        playPauseImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.pause_img));

        try{
            FileInputStream fis = new FileInputStream(path);
            mediaPlayer.setDataSource(fis.getFD());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),"File not found",Toast.LENGTH_SHORT).show();
        }

        finalTime = mediaPlayer.getDuration();
        startTime = mediaPlayer.getCurrentPosition();

        if (oneTimeOnly == 0) {
            seekbar.setMax((int) finalTime);
            oneTimeOnly = 1;
        }

        stopTimeView.setText(String.format("%d:%d",
                TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
                TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                                finalTime)))
        );
        startTimeView.setText(String.format("%d:%d",
                TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                                startTime)))
        );
        if (!mediaPlayer.isPlaying()){
            playAudio();
        }

        playPauseImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    playPauseImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.play_img));
                } else{
                    playAudio();
                    playPauseImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.pause_img));
                }
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                oneTimeOnly = 0;
                mediaPlayer.stop();
                File f=new File(path);
                Uri uri = Uri.parse("file://"+f.getAbsolutePath());
                Intent share = new Intent(Intent.ACTION_SEND);
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.setType("audio/*");
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(share, "Share audio File"));
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                oneTimeOnly = 0;
                mediaPlayer.stop();
                finish();
            }
        });
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            startTime = mediaPlayer.getCurrentPosition();
            startTimeView.setText(String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                    TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                    toMinutes((long) startTime)))
            );
            seekbar.setProgress((int)startTime);
            durationHandler.postDelayed(this, 100);
            if(!mediaPlayer.isPlaying()){
                playPauseImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.play_img));
            }
        }
    };

    private  void playAudio(){
        mediaPlayer.start();
        timeElapsed = mediaPlayer.getCurrentPosition();
        seekbar.setProgress((int) timeElapsed);
        durationHandler.postDelayed(UpdateSongTime, 100);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        oneTimeOnly = 0;
        mediaPlayer.stop();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        oneTimeOnly = 0;
        mediaPlayer.stop();
    }
}












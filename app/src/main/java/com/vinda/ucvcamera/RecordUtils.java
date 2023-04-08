package com.vinda.ucvcamera;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.yuan.camera_test.R;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordUtils {
    private ExecutorService executor;    //录音JNI函数不具有线程安全性，因此用单线程
    private MediaRecorder recorder;
    private MediaPlayer player;
    private File recorderFile;
    private long startRecorderTime, stopRecorderTime;
    private boolean isPlaying = false, isRecord;
    private MainActivity activity;
    private final Handler mHandler = new Handler();
    /**
     * 更新话筒状态
     */
    private int BASE = 1;
    private int SPACE = 200;// 间隔取样时间
    private Runnable mUpdateMicStatusTimer = new Runnable() {
        public void run() {
            updateMicStatus();
        }
    };

    public RecordUtils(MainActivity activity) {
        executor = Executors.newSingleThreadExecutor();
        this.activity = activity;
    }


    public void startRecorder(final OnRecordListener onRecordListener) {
        isRecord = true;
        isPlaying = false;
        //提交后台任务，开始录音
        //stopPlayer();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                //1释放上一次的录音
                releaseRecorder();
                //2开始录音
                try {
                    if(!exeStart()) {
                        recorderFile = null;
                        //录音失败
                        if(onRecordListener != null) {
                            onRecordListener.onRecordFail();
                        }
                    } else {//成功
                        if(onRecordListener != null) {
                            onRecordListener.onRecordSuccess();
                        }
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean exeStart() throws IOException {

        Log.i("tan6458", "开始录音:");
        //创建MediaRecorder
        recorder = new MediaRecorder();
        //创建录音文件
        recorderFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/recorderdemo/"+System.currentTimeMillis()+".amr");
        if(!recorderFile.getParentFile().exists())
            recorderFile.getParentFile().mkdirs();
        try {
            recorderFile.createNewFile();
        } catch(IOException e) {
            e.printStackTrace();
        }
        //配置MediaRecorder
        //从麦克风采集
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //保存文件为MP4格式
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        //所有android系统都支持的适中采样的频率        22.05KHz、44.1KHz、48KHz
        ////        recorder. setMaxDuration(6000);//最大录音时间
        recorder.setAudioSamplingRate(44100);
        //通用的AAC编码格式[格式]
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        //设置码率(就像一分钟视频多少帧)[码率]
        recorder.setAudioEncodingBitRate(96000);//fm质量
        //设置文件录音的位置
        recorder.setOutputFile(recorderFile.getAbsolutePath());
        recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                Log.i("tan6458", "what:"+what);
                Log.i("tan6458", "extra:"+extra);
            }
        });    //开始录音----------------------
        recorder.prepare();
        recorder.start();
        updateMicStatus();
        startRecorderTime = System.currentTimeMillis();
        //记录开始录音时间，用于统计时长，小于3秒中，录音不发送
        return true;
    }

    private void updateMicStatus() {
        if(recorder != null && isRecord) {
            int ratio = recorder.getMaxAmplitude() / BASE;
            int db = 0;// 分贝
            if(ratio > 1)
                db = (int) (20 * Math.log10(ratio));
            Log.i("tan6458", "分贝值："+db);
            final double finalDb = db;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((VoiceWave) activity.findViewById(R.id.videoView)).setDecibel((int) finalDb);
                }
            });
            mHandler.postDelayed(mUpdateMicStatusTimer, SPACE);
        }
    }

    /**
     * 停止录音
     */
    public void stopRecord(final OnRecordListener onRecordListener) {
        isRecord = false;
        //提交后台任务，停止录音
        executor.submit(new Runnable() {
            @Override
            public void run() {
                //停止录音
                if(!exeStop()) {
                    recorderFile = null;
                    if(onRecordListener != null) {
                        onRecordListener.onRecordFail();
                    }
                } else {
                    if(onRecordListener != null) {
                        onRecordListener.onRecordSuccess();
                    }
                }
                //释放
                releaseRecorder();
            }
        });
    }

    //停止录音 recorder.stop();
    private boolean exeStop() {
        recorder.stop();
        stopRecorderTime = System.currentTimeMillis();
        final int second = (int) (stopRecorderTime-startRecorderTime) / 1000;

        return true;
    }


    /**
     * 播放录音
     */
    public void clickPlay(final MediaPlayer.OnErrorListener errorListener) {
        if(!isPlaying) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    exePlay(recorderFile, errorListener);
                }
            });
        }
    }

    public void exePlay(File audioFile, final MediaPlayer.OnErrorListener errorListener) {
        try {
            if(audioFile == null) {
                Toast.makeText(activity, "录音文件为空", Toast.LENGTH_SHORT).show();
                return;
            }
            //配置播放器 MediaPlayer
            player = new MediaPlayer();
            //设置声音文件
            player.setDataSource(audioFile.getAbsolutePath());
            //配置音量,中等音量
            player.setVolume(1, 1);
            //播放是否循环
            player.setLooping(false);
            //设置监听回调 播放完毕
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlayer();
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    stopPlayer();
                    if(errorListener != null) {
                        player.setOnErrorListener(errorListener);
                    }
                    return true;
                }
            });
            //设置播放
            player.prepare();
            player.start();
            //异常处理，防止闪退
        } catch(Exception e) {
            e.printStackTrace();
            stopPlayer();
        }
    }


    //当activity关闭时，停止这个线程，防止内存泄漏
    public void destroy() {
        executor.shutdownNow();
        releaseRecorder();
    }

    public interface OnRecordListener {
        void onRecordSuccess();

        void onRecordFail();
    }

    private void releaseRecorder() {
        if(recorder != null) {
            Log.i("tan6458", "释放上一次录音");
            recorder.release();
            recorder = null;
        }
    }

    private void stopPlayer() {
        isPlaying = false;
        if(player != null) {
            player.release();
            player = null;
        }
    }
}

package com.tencentcs.iotvideodemo.videoplayer;

import android.Manifest;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tencentcs.iotvideo.iotvideoplayer.IErrorListener;
import com.tencentcs.iotvideo.iotvideoplayer.IPreparedListener;
import com.tencentcs.iotvideo.iotvideoplayer.IRecordListener;
import com.tencentcs.iotvideo.iotvideoplayer.ISnapShotListener;
import com.tencentcs.iotvideo.iotvideoplayer.IStatusListener;
import com.tencentcs.iotvideo.iotvideoplayer.ITimeListener;
import com.tencentcs.iotvideo.iotvideoplayer.IUserDataListener;
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoView;
import com.tencentcs.iotvideo.iotvideoplayer.PlayerStateEnum;
import com.tencentcs.iotvideo.iotvideoplayer.codec.MediaCodecAudioDecoder;
import com.tencentcs.iotvideo.iotvideoplayer.codec.MediaCodecPlayer;
import com.tencentcs.iotvideo.iotvideoplayer.codec.MediaCodecVideoDecoder;
import com.tencentcs.iotvideo.iotvideoplayer.player.LivePlayer;
import com.tencentcs.iotvideo.utils.LogUtils;
import com.tencentcs.iotvideo.utils.Utils;
import com.tencentcs.iotvideodemo.R;
import com.tencentcs.iotvideodemo.base.BaseFragment;
import com.tencentcs.iotvideodemo.utils.StorageManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

public class MonitorPlayerFragment extends BaseFragment implements View.OnClickListener {
    private static final String TAG = "MonitorPlayerFragment";

    private View mVideoView;
    private TextureView mPreviewSurface;
    private LivePlayer mMonitorPlayer;

    private ConstraintLayout mRootView;
    private Button mPlayBtn;
    private Button mStopBtn;
    private Button mSnapBtn;
    private Button mRecordBtn;

    private Button mMuteBtn;

    private Button mStartTalk;
    private Button mStopTalk;
    private Button mOpenCamera;
    private Button mChooseCamera;
    private Button mCloseCamera;
    private TextView mTvMonitorState;

    private String mDeviceId = "";
    private boolean mUseMediaCodec;
    private boolean mRenderDirectly;
    private int mRenderDirectlyType;

    private OutputListener mOutputListener;

    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monitor_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRootView = view.findViewById(R.id.root_view);
        mPreviewSurface = view.findViewById(R.id.preview_surface);
        mPlayBtn = view.findViewById(R.id.play_btn);
        mPlayBtn.setOnClickListener(this);
        mStopBtn = view.findViewById(R.id.stop_btn);
        mStopBtn.setOnClickListener(this);
        mSnapBtn = view.findViewById(R.id.snap_btn);
        mSnapBtn.setOnClickListener(this);
        mRecordBtn = view.findViewById(R.id.record_btn);
        mRecordBtn.setOnClickListener(this);

        mMuteBtn = view.findViewById(R.id.mute_btn);
        mMuteBtn.setOnClickListener(this);

        mStartTalk = view.findViewById(R.id.start_talk_btn);
        mStartTalk.setOnClickListener(this);
        mStopTalk = view.findViewById(R.id.stop_talk_btn);
        mStopTalk.setOnClickListener(this);
        mOpenCamera = view.findViewById(R.id.open_camera_btn);
        mOpenCamera.setOnClickListener(this);
        mChooseCamera = view.findViewById(R.id.choose_camera_btn);
        mChooseCamera.setOnClickListener(this);
        mCloseCamera = view.findViewById(R.id.close_camera_btn);
        mCloseCamera.setOnClickListener(this);

        Bundle bundle = getArguments();
        if (bundle != null) {
            mDeviceId = bundle.getString("deviceID");
            mUseMediaCodec = bundle.getBoolean("useMediaCodec", false);
            mRenderDirectly = bundle.getBoolean("renderDirectly", false);
            mRenderDirectlyType = bundle.getInt("renderDirectlyType", 0);
        }
        LogUtils.i(TAG, "mDeviceId = " + mDeviceId + " useMediaCodec = " + mUseMediaCodec
                + " renderDirectly = " + mRenderDirectly + " renderDirectlyType = " + mRenderDirectlyType);

        addVideoView(view.getContext());
        addStatusOutputView(view.getContext());

        mMonitorPlayer = new LivePlayer();
        mMonitorPlayer.setDataResource(mDeviceId);
        mMonitorPlayer.setPreparedListener(mPreparedListener);
        mMonitorPlayer.setStatusListener(mStatusListener);
        mMonitorPlayer.setTimeListener(mTimeListener);
        mMonitorPlayer.setErrorListener(mErrorListener);
        mMonitorPlayer.setUserDataListener(mUserDataListener);
        if (mVideoView instanceof IoTVideoView) {
            if (mUseMediaCodec) {
                mMonitorPlayer.setVideoView((IoTVideoView) mVideoView);
                mMonitorPlayer.setVideoDecoder(new MediaCodecVideoDecoder());
                mMonitorPlayer.setAudioDecoder(new MediaCodecAudioDecoder());
            } else {
                mMonitorPlayer.setVideoView((IoTVideoView) mVideoView);
            }
        } else {
            new MediaCodecPlayer(view.getContext(), mVideoView, mMonitorPlayer).init();
        }
        appendToOutput("设备ID：" + mDeviceId + " useMediaCodec = " + mUseMediaCodec + " " + mVideoView.getClass().getSimpleName());
    }

    private void addVideoView(Context context) {
        if (mUseMediaCodec && mRenderDirectly) {
            if (mRenderDirectlyType == 0) {
                mVideoView = new TextureView(context);
            } else {
                mVideoView = new GLSurfaceView(context);
                ((GLSurfaceView) mVideoView).setEGLContextClientVersion(2);
            }
        } else {
            mVideoView = new IoTVideoView(context);
        }
        mVideoView.setId(View.generateViewId());
        mRootView.addView(mVideoView);
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mVideoView.getLayoutParams();
        layoutParams.width = 0;
        layoutParams.height = 0;
        layoutParams.dimensionRatio = "H,16:9";
        layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
    }

    private IPreparedListener mPreparedListener = new IPreparedListener() {
        @Override
        public void onPrepared() {
            LogUtils.d(TAG, "onPrepared");
            appendToOutput("开始准备");
        }
    };

    private IStatusListener mStatusListener = new IStatusListener() {
        @Override
        public void onStatus(int status) {
            LogUtils.d(TAG, "onStatus status " + status);
            appendToOutput("播放状态：" + getPlayStatus(status));
        }
    };

    private ITimeListener mTimeListener = new ITimeListener() {
        @Override
        public void onTime(long currentTime) {
            //LogUtils.d(TAG, "onTime currentTime " + currentTime);
        }
    };

    private IErrorListener mErrorListener = new IErrorListener() {
        @Override
        public void onError(int error) {
            LogUtils.d(TAG, "onError error " + error);
            appendToOutput("播放错误：" + Utils.getErrorDescription(error));
        }
    };

    private IUserDataListener mUserDataListener = new IUserDataListener() {
        @Override
        public void onReceive(byte[] data) {
            LogUtils.d(TAG, "onReceive ----");
            appendToOutput("收到数据：" + data);
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play_btn:
                mMonitorPlayer.play();
                break;
            case R.id.stop_btn:
                mMonitorPlayer.stop();
                break;
            case R.id.snap_btn:
                if (!StorageManager.isPicPathAvailable()) {
                    Toast.makeText(view.getContext(), "storage is not available", Toast.LENGTH_LONG).show();
                    break;
                }
                File snapFile = new File(StorageManager.getPicPath() + File.separator + mDeviceId);
                if (!snapFile.exists() && !snapFile.mkdirs()) {
                    LogUtils.e(TAG, "can not create file");
                    break;
                }
                mMonitorPlayer.snapShot(snapFile.getAbsolutePath() + File.separator + mSimpleDateFormat.format(new Date()) + ".jpeg",
                        new ISnapShotListener() {
                            @Override
                            public void onResult(int code, String path) {
                                Toast.makeText(view.getContext(), "code:" + code + " path:" + path, Toast.LENGTH_LONG).show();
                                appendToOutput("截图结果：  返回码 " + code + " 路径 " + path);
                            }
                        });
                break;
            case R.id.record_btn:
                if (!StorageManager.isVideoPathAvailable()) {
                    Toast.makeText(view.getContext(), "storage is not available", Toast.LENGTH_LONG).show();
                    return;
                }
                if (mMonitorPlayer.isRecording()) {
                    mRecordBtn.setText("开始录像");
                    appendToOutput("停止录像");
                    mMonitorPlayer.stopRecord();
                } else {
                    mRecordBtn.setText("停止录像");
                    appendToOutput("开始录像");
                    File recordFile = new File(StorageManager.getVideoPath() + File.separator + mDeviceId);
                    if (!recordFile.exists() && !recordFile.mkdirs()) {
                        LogUtils.e(TAG, "can not create file");
                        break;
                    }
                    mMonitorPlayer.startRecord(recordFile.getAbsolutePath() + File.separator + mSimpleDateFormat.format(new Date()) + ".mp4",
                            new IRecordListener() {
                                @Override
                                public void onResult(int code, String path) {
                                    Toast.makeText(view.getContext(), "code:" + code + " path:" + path, Toast.LENGTH_LONG).show();
                                    if (code != 0) {
                                        mRecordBtn.setText("开始录像");
                                    }
                                    appendToOutput("录像结果：  返回码 " + code + " 路径 " + path);
                                }
                            });
                }
                break;
            case R.id.start_talk_btn:
                requestPermissions(new OnPermissionsListener() {
                    @Override
                    public void OnPermissions(boolean granted) {
                        if (granted) {
                            mMonitorPlayer.startTalk();
                            appendToOutput("开始对讲");
                        }
                    }
                }, Manifest.permission.RECORD_AUDIO);
                break;
            case R.id.stop_talk_btn:
                if (mMonitorPlayer.isTalking()) {
                    mMonitorPlayer.stopTalk();
                    appendToOutput("结束对讲");
                }
                break;
            case R.id.open_camera_btn:
                requestPermissions(new OnPermissionsListener() {
                    @Override
                    public void OnPermissions(boolean granted) {
                        if (granted) {
                            mMonitorPlayer.openCameraAndPreview(view.getContext(),
                                    mPreviewSurface, StorageManager.getVideoPath() + File.separator + "preview.mp4");
                            appendToOutput("打开摄像头");
                        }
                    }
                }, Manifest.permission.CAMERA);
                break;
            case R.id.choose_camera_btn:
                mMonitorPlayer.switchCamera(view.getContext());
                appendToOutput("切换摄像头");
                break;
            case R.id.close_camera_btn:
                if (mMonitorPlayer.isCameraOpen()) {
                    mMonitorPlayer.closeCamera();
                    appendToOutput("关闭摄像头");
                }
                break;
            case R.id.mute_btn:
                mMonitorPlayer.mute(!mMonitorPlayer.isMute());
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVideoView instanceof IoTVideoView) {
            ((IoTVideoView) mVideoView).onResume();
        } else if (mVideoView instanceof GLSurfaceView) {
            ((GLSurfaceView) mVideoView).onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVideoView instanceof IoTVideoView) {
            ((IoTVideoView) mVideoView).onPause();
        } else if (mVideoView instanceof GLSurfaceView) {
            ((GLSurfaceView) mVideoView).onPause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMonitorPlayer != null) {
            mMonitorPlayer.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMonitorPlayer != null) {
            mMonitorPlayer.release();
            mMonitorPlayer = null;
        }
    }

    private void addStatusOutputView(Context context) {
        mTvMonitorState = new TextView(context);
        mRootView.addView(mTvMonitorState);
        ConstraintLayout.LayoutParams tvLayoutParams = (ConstraintLayout.LayoutParams) mTvMonitorState.getLayoutParams();
        tvLayoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
        tvLayoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
        tvLayoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        tvLayoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        tvLayoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        mTvMonitorState.setTextColor(context.getResources().getColor(android.R.color.white));
        mTvMonitorState.setGravity(Gravity.END);
    }

    private void appendToOutput(String text) {
        if (mOutputListener != null) {
            mOutputListener.onOutput(text);
        } else {
            mTvMonitorState.setText(text);
        }
    }

    private String getPlayStatus(int status) {
        String playStatus = "";
        switch (status) {
            case PlayerStateEnum.STATE_IDLE:
                playStatus = "未初始化";
                break;
            case PlayerStateEnum.STATE_INITIALIZED:
                playStatus = "已初始化";
                break;
            case PlayerStateEnum.STATE_PREPARING:
                playStatus = "准备中...";
                break;
            case PlayerStateEnum.STATE_READY:
                playStatus = "准备完成";
                break;
            case PlayerStateEnum.STATE_LOADING:
                playStatus = "加载中";
                break;
            case PlayerStateEnum.STATE_PLAY:
                playStatus = "播放中";
                break;
            case PlayerStateEnum.STATE_PAUSE: {
                playStatus = "暂停";
            }
            break;
            case PlayerStateEnum.STATE_STOP:
                playStatus = "停止播放";
                break;
        }
        return playStatus;
    }

    public void setOutputListener(OutputListener listener) {
        mOutputListener = listener;
    }

    public interface OutputListener {
        void onOutput(String text);
    }
}

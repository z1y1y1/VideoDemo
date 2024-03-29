package com.aliyun.demo.recorder.view;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioGroup;

import com.aliyun.common.global.AliyunTag;
import com.aliyun.common.utils.CommonUtil;
import com.aliyun.common.utils.StorageUtils;
import com.aliyun.demo.R;
import com.aliyun.demo.recorder.OpenGLTest;
import com.aliyun.demo.recorder.activity.AlivcSvideoRecordActivity;
import com.aliyun.demo.recorder.bean.RememberBeautyBean;
import com.aliyun.demo.recorder.faceunity.FaceUnityManager;
import com.aliyun.demo.recorder.util.Common;
import com.aliyun.demo.recorder.util.FixedToastUtils;
import com.aliyun.demo.recorder.util.OrientationDetector;
import com.aliyun.demo.recorder.util.PermissionUtils;
import com.aliyun.demo.recorder.util.SharedPreferenceUtils;
import com.aliyun.demo.recorder.util.TimeFormatterUtils;
import com.aliyun.demo.recorder.view.control.CameraType;
import com.aliyun.demo.recorder.view.control.ControlView;
import com.aliyun.demo.recorder.view.control.ControlViewListener;
import com.aliyun.demo.recorder.view.control.FlashType;
import com.aliyun.demo.recorder.view.control.RecordState;
import com.aliyun.demo.recorder.view.countdown.AlivcCountDownView;
import com.aliyun.demo.recorder.view.dialog.BeautyEffectChooser;
import com.aliyun.demo.recorder.view.dialog.DialogVisibleListener;
import com.aliyun.demo.recorder.view.dialog.FilterEffectChooser;
import com.aliyun.demo.recorder.view.dialog.GIfEffectChooser;
import com.aliyun.demo.recorder.view.effects.face.BeautyFaceDetailChooser;
import com.aliyun.demo.recorder.view.effects.face.BeautyService;
import com.aliyun.demo.recorder.view.effects.filter.EffectInfo;
import com.aliyun.demo.recorder.view.effects.filter.interfaces.OnFilterItemClickListener;
import com.aliyun.demo.recorder.view.effects.paster.PasterSelectListener;
import com.aliyun.demo.recorder.view.effects.skin.BeautySkinDetailChooser;
import com.aliyun.demo.recorder.view.focus.FocusView;
import com.aliyun.demo.recorder.view.music.MusicChooser;
import com.aliyun.demo.recorder.view.music.MusicSelectListener;
import com.aliyun.downloader.zipprocessor.DownloadFileUtils;
import com.aliyun.recorder.AliyunRecorderCreator;
import com.aliyun.recorder.supply.AliyunIClipManager;
import com.aliyun.recorder.supply.AliyunIRecorder;
import com.aliyun.recorder.supply.EncoderInfoCallback;
import com.aliyun.recorder.supply.RecordCallback;
import com.aliyun.svideo.base.http.MusicFileBean;
import com.aliyun.svideo.base.widget.ProgressDialog;
import com.aliyun.svideo.base.widget.RecordTimelineView;
import com.aliyun.svideo.base.widget.beauty.BeautyConstants;
import com.aliyun.svideo.base.widget.beauty.BeautyDetailSettingView;
import com.aliyun.svideo.base.widget.beauty.BeautyParams;
import com.aliyun.svideo.base.widget.beauty.enums.BeautyLevel;
import com.aliyun.svideo.base.widget.beauty.enums.BeautyMode;
import com.aliyun.svideo.base.widget.beauty.listener.OnBeautyDetailClickListener;
import com.aliyun.svideo.base.widget.beauty.listener.OnBeautyFaceItemSeletedListener;
import com.aliyun.svideo.base.widget.beauty.listener.OnBeautyModeChangeListener;
import com.aliyun.svideo.base.widget.beauty.listener.OnBeautyParamsChangeListener;
import com.aliyun.svideo.base.widget.beauty.listener.OnBeautySkinItemSeletedListener;
import com.aliyun.svideo.base.widget.beauty.listener.OnViewClickListener;
import com.aliyun.svideo.sdk.external.struct.common.VideoQuality;
import com.aliyun.svideo.sdk.external.struct.effect.EffectBean;
import com.aliyun.svideo.sdk.external.struct.effect.EffectFilter;
import com.aliyun.svideo.sdk.external.struct.effect.EffectPaster;
import com.aliyun.svideo.sdk.external.struct.encoder.EncoderInfo;
import com.aliyun.svideo.sdk.external.struct.encoder.VideoCodecs;
import com.aliyun.svideo.sdk.external.struct.form.PreviewPasterForm;
import com.aliyun.svideo.sdk.external.struct.recorder.CameraParam;
import com.aliyun.svideo.sdk.external.struct.recorder.MediaInfo;
import com.aliyun.svideo.sdk.external.struct.snap.AliyunSnapVideoParam;
import com.aliyun.video.common.utils.DensityUtils;
import com.aliyun.video.common.utils.ScreenUtils;
import com.aliyun.video.common.utils.ThreadUtils;
import com.google.gson.Gson;
import com.qu.preview.callback.OnFrameCallBack;
import com.qu.preview.callback.OnTextureIdCallBack;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 新版本(> 3.6.5之后)录制模块的具体业务实现类
 */
public class AliyunSVideoRecordView extends FrameLayout
    implements DialogVisibleListener, ScaleGestureDetector.OnScaleGestureListener {
    private static final String TAG = AliyunSVideoRecordView.class.getSimpleName();
    private static final String TAG_GIF_CHOOSER = "gif";
    private static final String TAG_BEAUTY_CHOOSER = "beauty";
    private static final String TAG_MUSIC_CHOOSER = "music";
    private static final String TAG_FILTER_CHOOSER = "filter";
    private static final String TAG_BEAUTY_DETAIL_FACE_CHOOSER = "beautyFace";
    private static final String TAG_BEAUTY_DETAIL_SKIN_CHOOSER = "beautySkin";
    //最小录制时长
    private static final int MIN_RECORD_TIME = 0;
    //最大录制时长
    private static final int MAX_RECORD_TIME = Integer.MAX_VALUE;

    private static int TEST_VIDEO_WIDTH = 540;
    private static int TEST_VIDEO_HEIGHT = 960;
    /**
     * v3.7.8改动, GlsurfaceView --> SurfaceView
     * AliyunIRecorder.setDisplayView(GLSurfaceView surfaceView) =====>> AliyunIRecorder.setDisplayView(SurfaceView surfaceView)
     */
    private SurfaceView mSurfaceView;
    private ControlView mControlView;
    private RecordTimelineView mRecordTimeView;
    private AlivcCountDownView mCountDownView;
    private AliyunIRecorder recorder;
    private AliyunIClipManager clipManager;
    private com.aliyun.svideo.sdk.external.struct.recorder.CameraType cameraType
        = com.aliyun.svideo.sdk.external.struct.recorder.CameraType.FRONT;
    private FragmentActivity mActivity;
    private boolean isOpenFailed = false;
    //正在准备录制视频,readyview显示期间为true，其他为false
    private boolean isLoadingReady = false;
    //录制视频是否达到最大值
    private boolean isMaxDuration = false;
    //录制时长
    private int recordTime = 0;
    //文件存放位置
    private String videoPath;

    //最小录制时长
    private int minRecordTime = 2000;
    //最大录制时长
    private int maxRecordTime = 15 * 1000;
    //录制码率
    private int mBitrate = 25;
    //关键帧间隔
    private int mGop = 5;
    //视频质量
    private VideoQuality mVideoQuality = VideoQuality.HD;
    //视频比例
    private int mRatioMode = AliyunSnapVideoParam.RATIO_MODE_3_4;
    //编码方式
    private VideoCodecs mVideoCodec = VideoCodecs.H264_HARDWARE;
    private GIfEffectChooser gifEffectChooser;
    /**
     * 滤镜选择弹窗
     */
    private FilterEffectChooser filterEffectChooser;
    //视频分辨率
    private int mResolutionMode = AliyunSnapVideoParam.RESOLUTION_540P;

    private BeautyEffectChooser beautyEffectChooser;
    //选中的贴图效果
    private EffectPaster effectPaster;
    private OrientationDetector orientationDetector;
    private int rotation;
    private MusicChooser musicChooseView;
    /**
     * 第三方高级美颜支持库faceUnity管理类
     */
    private FaceUnityManager faceUnityManager;
    /**
     * 相机的原始NV21数据
     */
    private byte[] frameBytes;
    private byte[] mFuImgNV21Bytes;
    /**
     * 原始数据宽
     */
    private int frameWidth;
    /**
     * 原始数据高
     */
    private int frameHeight;
    /**
     * faceUnity相关
     */
    private int mFrameId = 0;
    private BeautyParams beautyParams;
    private BeautyFaceDetailChooser beautyFaceDetailChooser;
    private BeautySkinDetailChooser beautySkinDetailChooser;
    /**
     * 美肌美颜微调dialog是否正在显示
     */
    private boolean isbeautyDetailShowing;

    /**
     * 美颜默认档位
     */
    private BeautyLevel defaultBeautyLevel = BeautyLevel.BEAUTY_LEVEL_THREE;

    /**
     * 当前美颜模式
     */
    private BeautyMode currentBeautyFaceMode = BeautyMode.Advanced;
    public static final int TYPE_FILTER = 1;
    public static final int TYPE_MUSIC = 3;
    private LinkedHashMap<Integer, Object> mConflictEffects = new LinkedHashMap<>();
    private EffectBean effectMusic;
    private AsyncTask<Void, Void, Void> finishRecodingTask;
    private AsyncTask<Void, Void, Void> faceTrackPathTask;

    /**
     * 记录filter选中的item索引
     */
    private int currentFilterPosition;
    /**
     * 记录美颜选中的索引, 默认为3
     */
    private int currentBeautyFacePosition = 3;

    /**
     * 记录美颜选中的索引, 默认为3
     */
    private int currentBeautyFaceNormalPosition = 3;

    /**
     * 当前美肌选择的item下标, 默认为3
     */
    private int currentBeautySkinPosition = 3;
    /**
     * 控制mv的添加, 开始录制后,不允许切换mv
     */
    private boolean isAllowChangeMv = true;
    private AsyncTask<Void, Void, Void> mFaceUnityTask;
    private List<BeautyParams> rememberParamList;
    private RememberBeautyBean rememberBeautyBean;
    private AsyncTask<Void, Void, Void> beautyParamCopyTask;
    private ProgressDialog progressBar;

    /**
     * 用于判断当前音乐界面是否可见, 如果可见, 从后台回到前台时, restoreConflictEffect()不恢复mv的播放, 否则会和音乐重复播放
     * <p>
     * true: 可见 false: 不可见
     */
    private boolean isMusicViewShowing;

    /**
     * faceUnity的初始化结果 true: 初始化成功 false: 初始化失败
     */
    private static boolean faceInitResult;
    /**
     * 是否处于后台
     */
    private boolean mIsBackground;
    private BeautyService beautyService;

    private boolean isHasMusic = false;
    private FocusView mFocusView;

    public boolean isHasMusic() {
        return isHasMusic;
    }

    /**
     * 音乐选择监听，通知到外部
     */
    private MusicSelectListener mOutMusicSelectListener;
    /**
     * 恢复冲突的特效，这些特效都是会彼此冲突的，比如滤镜和MV，因为MV中也有滤镜效果，所以MV和滤镜的添加顺序 会影响最终产生视频的效果，在恢复时必须严格按照用户的操作顺序来恢复，
     * 这样就需要维护一个添加过的特效类的列表，然后按照列表顺序 去恢复
     */
    private void restoreConflictEffect() {
        if (!mConflictEffects.isEmpty()) {
            for (Map.Entry<Integer, Object> entry : mConflictEffects.entrySet()) {
                switch (entry.getKey()) {
                case TYPE_FILTER:
                    recorder.applyFilter((EffectFilter)entry.getValue());
                    break;
                case TYPE_MUSIC:
                    EffectBean music = (EffectBean)entry.getValue();

                    if (music != null) {
                        recorder.setMusic(music.getPath(), music.getStartTime(), music.getDuration());
                        // 根据音乐路径判断是否添加了背景音乐,
                        // 在编辑界面, 如果录制添加了背景音乐, 则不能使用音效特效
                        isHasMusic = !TextUtils.isEmpty(music.getPath());
                    }
                    break;
                default:
                    break;
                }
            }
        }
    }

    /**
     * 底层在onPause时会回收资源, 此处选择的滤镜的资源路径, 用于恢复状态
     */
    private String filterSourcePath;

    /**
     * 美颜美肌点击了back按钮
     */
    private boolean isbeautyDetailBack;

    /**
     * 用于防止sdk的oncomplete回调之前, 再次调用startRecord
     */
    private boolean tempIsComplete = true;

    public AliyunSVideoRecordView(Context context) {
        super(context);
        initVideoView();
    }

    public AliyunSVideoRecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView();
    }

    public AliyunSVideoRecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView();
    }

    private void initVideoView() {
        //初始化surfaceView

        initSurfaceView();
        initControlView();
        initCountDownView();
        initBeautyParam();
        initRecorder();
        initRecordTimeView();
        initFocusView();
        initFaceUnity(getContext());
        setFaceTrackModePath();

    }

    /**
     * 焦点focus
     */
    private void initFocusView() {
        mFocusView = new FocusView(getContext());
        mFocusView.setPadding(10, 10, 10, 10);
        addSubView(mFocusView);
    }

    private void initBeautyParam() {
        beautyParamCopyTask = new BeautyParamCopyTask(this).executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR);

        currentBeautySkinPosition = SharedPreferenceUtils.getBeautySkinLevel(getContext());
        currentBeautyFacePosition = SharedPreferenceUtils.getBeautyFaceLevel(getContext());
        currentBeautyFaceNormalPosition = SharedPreferenceUtils.getBeautyNormalFaceLevel(getContext());
    }

    private void beautyParamCopy() {

        rememberBeautyBean = new RememberBeautyBean();

        rememberParamList = new ArrayList<>();

        int size = BeautyConstants.BEAUTY_MAP.size();
        // 需求要记录之前修改的美颜参数, 所以每次先读取json中的数据,如果json无数据, 就从常量中拿
        String jsonBeautyParam = SharedPreferenceUtils.getBeautyParams(getContext());
        if (TextUtils.isEmpty(jsonBeautyParam)) {
            for (int i = 0; i < size; i++) {
                BeautyParams beautyParams = BeautyConstants.BEAUTY_MAP.get(i);
                BeautyParams rememberParam = beautyParams.clone();
                rememberParamList.add(rememberParam);
            }
        } else {
            for (int i = 0; i < size; i++) {
                BeautyParams beautyParams = getBeautyParams(i);
                rememberParamList.add(beautyParams);
            }
        }
        rememberBeautyBean.setBeautyList(rememberParamList);
    }

    public void onPause() {
        mIsBackground = true;
    }

    public void onResume() {
        mIsBackground = false;
    }

    public void onStop() {
        if (mFocusView != null) {
            mFocusView.activityStop();
        }
    }

    private static class BeautyParamCopyTask extends AsyncTask<Void, Void, Void> {
        WeakReference<AliyunSVideoRecordView> weakReference;

        BeautyParamCopyTask(AliyunSVideoRecordView recordView) {
            weakReference = new WeakReference<>(recordView);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AliyunSVideoRecordView recordView = weakReference.get();
            if (recordView != null) {
                recordView.beautyParamCopy();
            }
            return null;
        }
    }

    private void initFaceUnity(Context context) {
        if (!faceInitResult) {
            mFaceUnityTask = new FaceUnityTask(this).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private static class FaceUnityTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<AliyunSVideoRecordView> weakReference;

        FaceUnityTask(AliyunSVideoRecordView recordView) {
            weakReference = new WeakReference<>(recordView);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AliyunSVideoRecordView recordView = weakReference.get();
            if (recordView != null) {
                recordView.faceUnityManager = FaceUnityManager.getInstance();
                faceInitResult = recordView.faceUnityManager.createBeautyItem(recordView.getContext());
                recordView.faceunityDefaultParam();
            }
            return null;
        }
    }

    /**
     * 初始化倒计时view
     */
    private void initCountDownView() {
        if (mCountDownView == null) {
            mCountDownView = new AlivcCountDownView(getContext());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER;
            addView(mCountDownView, params);
        }
    }

    private void initRecordTimeView() {
        mRecordTimeView = new RecordTimelineView(getContext());
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, DensityUtils.dip2px(getContext(), 6));

        params.setMargins(DensityUtils.dip2px(getContext(), 12),
                          DensityUtils.dip2px(getContext(), 6),
                          DensityUtils.dip2px(getContext(), 12),
                          0);
        mRecordTimeView.setColor(R.color.aliyun_color_record_duraton, R.color.aliyun_music_colorPrimary, R.color.aliyun_white,
                                 R.color.alivc_bg_record_time);
        mRecordTimeView.setMaxDuration(clipManager.getMaxDuration());
        mRecordTimeView.setMinDuration(clipManager.getMinDuration());
        addView(mRecordTimeView, params);

    }

    /**
     * 初始化surfaceView
     */
    private void initSurfaceView() {
        mSurfaceView = new SurfaceView(getContext());
        final ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(getContext(), this);
        final GestureDetector gestureDetector = new GestureDetector(getContext(),
        new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                float x = e.getX() / mSurfaceView.getWidth();
                float y = e.getY() / mSurfaceView.getHeight();
                recorder.setFocus(x, y);

                mFocusView.showView();
                mFocusView.setLocation(e.getX(), e.getY());
                return true;
            }
        });
        mSurfaceView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() >= 2) {
                    scaleGestureDetector.onTouchEvent(event);
                } else if (event.getPointerCount() == 1) {
                    gestureDetector.onTouchEvent(event);
                }
                return true;
            }
        });
        addSubView(mSurfaceView);
    }

    /**
     * 初始化控制栏view
     */
    private void initControlView() {
        mControlView = new ControlView(getContext());
        mControlView.setControlViewListener(new ControlViewListener() {
            @Override
            public void onBackClick() {
                if (mBackClickListener != null) {
                    mBackClickListener.onClick();
                }
            }

            @Override
            public void onNextClick() {
                // 完成录制
                if (!isStopToCompleteDuration) {
                    finishRecording();
                }
            }

            @Override
            public void onBeautyFaceClick() {
                showBeautyFaceView();
            }

            @Override
            public void onMusicClick() {
                showMusicSelView();
            }

            @Override
            public void onCameraSwitch() {
                if (recorder != null) {
                    int cameraId = recorder.switchCamera();
                    for (com.aliyun.svideo.sdk.external.struct.recorder.CameraType type : com.aliyun.svideo.sdk
                            .external.struct.recorder.CameraType
                            .values()) {
                        if (type.getType() == cameraId) {
                            cameraType = type;
                        }
                    }
                    if (mControlView != null) {
                        for (CameraType type : CameraType.values()) {
                            if (type.getType() == cameraId) {
                                mControlView.setCameraType(type);
                            }
                        }

                        if (mControlView.getFlashType() == FlashType.ON
                                && mControlView.getCameraType() == CameraType.BACK) {
                            recorder.setLight(com.aliyun.svideo.sdk.external.struct.recorder.FlashType.TORCH);
                        }
                    }
                }
            }

            @Override
            public void onLightSwitch(FlashType flashType) {
                if (recorder != null) {
                    for (com.aliyun.svideo.sdk.external.struct.recorder.FlashType type : com.aliyun.svideo.sdk
                            .external.struct.recorder.FlashType
                            .values()) {
                        if (flashType.toString().equals(type.toString())) {
                            recorder.setLight(type);
                        }
                    }

                }
                if (mControlView.getFlashType() == FlashType.ON
                        && mControlView.getCameraType() == CameraType.BACK) {
                    recorder.setLight(com.aliyun.svideo.sdk.external.struct.recorder.FlashType.TORCH);
                }
            }

            @Override
            public void onRateSelect(float rate) {
                if (recorder != null) {
                    recorder.setRate(rate);
                }
            }

            @Override
            public void onGifEffectClick() {
                showGifEffectView();
            }

            @Override
            public void onReadyRecordClick(boolean isCancel) {
                if (isStopToCompleteDuration) {
                    /*TODO  这里是因为如果 SDK 还没有回调onComplete,倒计时录制，会crash */
                    return;
                }
                if (isCancel) {
                    cancelReadyRecord();
                } else {
                    showReadyRecordView();
                }

            }

            @Override
            public void onStartRecordClick() {
                if (!tempIsComplete) {
                    // 连续点击开始录制，停止录制，要保证在onComplete回调回来再允许点击开始录制,
                    // 否则SDK会出现ANR问题, v3.7.8修复会影响较大, 工具包暂时处理
                    return;
                }
                startRecord();
            }

            @Override
            public void onStopRecordClick() {
                if (!tempIsComplete) {
                    // 连续点击开始录制，停止录制，要保证在onComplete回调回来再允许点击开始录制,
                    // 否则SDK会出现ANR问题, v3.7.8修复会影响较大, 工具包暂时处理
                    return;
                }
                stopRecord();
            }

            @Override
            public void onDeleteClick() {
                if (isStopToCompleteDuration) {
                    // 这里是因为如果 SDK 还没有回调onComplete,点击回删会出现删除的不是最后一段的问题
                    return;
                }
                mRecordTimeView.deleteLast();
                clipManager.deletePart();
                isMaxDuration = false;
                if (mControlView != null) {
                    if (clipManager.getDuration() < clipManager.getMinDuration()) {
                        mControlView.setCompleteEnable(false);
                    }

                    mControlView.updataCutDownView(true);
                }

                if (clipManager.getDuration() == 0) {
                    //音乐可以选择
                    recorder.restartMv();
                    mControlView.setHasRecordPiece(false);
                    isAllowChangeMv = true;
                }
                mControlView.setRecordTime(TimeFormatterUtils.formatTime(clipManager.getDuration()));
            }

            @Override
            public void onFilterEffectClick() {
                // 滤镜选择弹窗弹出
                showFilterEffectView();
            }
        });
        addSubView(mControlView);
    }

    /**
     * 权限申请
     */
    String[] permission = {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * 开始录制
     */
    private void startRecord() {
        boolean checkResult = PermissionUtils.checkPermissionsGroup(getContext(), permission);
        if (!checkResult && mActivity != null) {
            PermissionUtils.requestPermissions(mActivity, permission,
                                               AlivcSvideoRecordActivity.PERMISSION_REQUEST_CODE);
            return;
        }

        if (CommonUtil.SDFreeSize() < 50 * 1000 * 1000) {
            FixedToastUtils.show(getContext(), getResources().getString(R.string.aliyun_no_free_memory));
            return;
        }
        if (isMaxDuration) {
            mControlView.setRecordState(RecordState.STOP);
            return;
        }

        if (recorder != null && !mIsBackground) {
            // 快速显示回删字样, 将音乐按钮置灰,假设此时已经有片段, 在录制失败时, 需要改回false
            mControlView.setHasRecordPiece(true);
            mControlView.setRecordState(RecordState.RECORDING);
            mControlView.setRecording(true);
            recorder.startRecording();

            Log.d(TAG, "startRecording    isStopToCompleteDuration:" + isStopToCompleteDuration);
        }

    }

    /**
     * 视频是是否正正在已经调用stopRecord到onComplete回调过程中这段时间，这段时间不可再次调用stopRecord
     * true: 正在调用stop~onComplete, false反之
     */
    private boolean isStopToCompleteDuration;

    /**
     * 停止录制
     */
    private void stopRecord() {
        Log.d(TAG, "stopRecord    isStopToCompleteDuration:" + isStopToCompleteDuration);
        if (recorder != null && !isStopToCompleteDuration && mControlView.isRecording()) {//
            isStopToCompleteDuration = true;
            tempIsComplete = false;

            //此处添加判断，progressBar弹出，也即当视频片段合成的时候，不调用stopRecording,
            //否则在finishRecording的时候调用stopRecording，会导致finishRecording阻塞
            //暂时规避，等待sdk解决该问题，取消该判断
            if ((progressBar == null || !progressBar.isShowing())) {
                recorder.stopRecording();

            }

        }

    }

    /**
     * 取消拍摄倒计时
     */
    private void cancelReadyRecord() {
        if (mCountDownView != null) {
            mCountDownView.cancle();
        }
    }

    /**
     * 显示准备拍摄倒计时view
     */
    private void showReadyRecordView() {
        if (mCountDownView != null) {
            mCountDownView.start();
        }
    }

    /**
     * 显示音乐选择的控件
     */
    private void showMusicSelView() {
        if (musicChooseView == null) {
            musicChooseView = new MusicChooser();

            musicChooseView.setRecordTime(getMaxRecordTime());

            musicChooseView.setMusicSelectListener(new MusicSelectListener() {

                @Override
                public void onMusicSelect(MusicFileBean musicFileBean, long startTime) {
                    if (musicFileBean != null) {
                        if (effectMusic != null) {
                            mConflictEffects.remove(TYPE_MUSIC);
                        }
                        effectMusic = new EffectBean();
                        effectMusic.setPath(musicFileBean.getPath());
                        effectMusic.setStartTime(startTime);
                        effectMusic.setDuration(getMaxRecordTime());

                        mConflictEffects.put(TYPE_MUSIC, effectMusic);
                        if (mOutMusicSelectListener != null) {
                            mOutMusicSelectListener.onMusicSelect(musicFileBean, startTime);
                        }
                        //如果音乐地址或者图片地址为空则使用默认图标
                        if (TextUtils.isEmpty(musicFileBean.getImage()) || TextUtils.isEmpty(musicFileBean.getPath())) {
                            mControlView.setMusicIconId(R.mipmap.aliyun_svideo_music);
                        } else {
                            mControlView.setMusicIcon(musicFileBean.getImage());
                        }
                    } else {
                        mControlView.setMusicIconId(R.mipmap.aliyun_svideo_music);
                    }
                }
            });

            musicChooseView.setDismissListener(new DialogVisibleListener() {
                @Override
                public void onDialogDismiss() {
                    mControlView.setMusicSelViewShow(false);
                    isMusicViewShowing = false;
                    restoreConflictEffect();
                }

                @Override
                public void onDialogShow() {
                    mControlView.setMusicSelViewShow(true);
                    recorder.applyMv(new EffectBean());
                    isMusicViewShowing = true;
                }
            });
        }
        musicChooseView.show(getFragmentManager(), TAG_MUSIC_CHOOSER);
    }

    /**
     * 当前所选中的动图路径
     */
    private String currPasterPath;

    /**
     * 显示动图效果调节控件
     */
    private void showGifEffectView() {
        if (gifEffectChooser == null) {
            gifEffectChooser = new GIfEffectChooser();
            gifEffectChooser.setDismissListener(this);
            gifEffectChooser.setPasterSelectListener(new PasterSelectListener() {
                @Override
                public void onPasterSelected(PreviewPasterForm imvForm) {
                    String path;
                    if (imvForm.getId() == 150) {
                        //id=150的动图为自带动图
                        path = imvForm.getPath();
                    } else {
                        path = DownloadFileUtils.getAssetPackageDir(getContext(),
                                imvForm.getName(), imvForm.getId()).getAbsolutePath();
                    }
                    currPasterPath = path;
                    addEffectToRecord(path);

                }

                @Override
                public void onSelectPasterDownloadFinish(String path) {
                    // 所选的paster下载完成后, 记录该paster 的path
                    currPasterPath = path;
                }
            });

            gifEffectChooser.setDismissListener(new DialogVisibleListener() {
                @Override
                public void onDialogDismiss() {
                    mControlView.setEffectSelViewShow(false);
                }

                @Override
                public void onDialogShow() {
                    mControlView.setEffectSelViewShow(true);
                    if (!TextUtils.isEmpty(currPasterPath)) {
                        // dialog显示后,如果记录的paster不为空, 使用该paster
                        addEffectToRecord(currPasterPath);
                    }
                }
            });
        }
        gifEffectChooser.show(getFragmentManager(), TAG_GIF_CHOOSER);
    }

    private void addEffectToRecord(String path) {

        if (effectPaster != null) {
            recorder.removePaster(effectPaster);
        }

        effectPaster = new EffectPaster(path);
        recorder.addPaster(effectPaster);

    }

    private FragmentManager getFragmentManager() {
        FragmentManager fm = null;
        if (mActivity != null) {
            fm = mActivity.getSupportFragmentManager();
        } else {
            Context mContext = getContext();
            if (mContext instanceof FragmentActivity) {
                fm = ((FragmentActivity)mContext).getSupportFragmentManager();
            }
        }
        return fm;
    }

    /**
     * 显示滤镜选择的控件
     */
    private void showFilterEffectView() {
        if (filterEffectChooser == null) {
            filterEffectChooser = new FilterEffectChooser();
        }
        if (filterEffectChooser.isAdded()) {
            return;
        }
        // 滤镜改变listener
        filterEffectChooser.setOnFilterItemClickListener(new OnFilterItemClickListener() {
            @Override
            public void onItemClick(EffectInfo effectInfo, int index) {
                if (effectInfo != null) {
                    filterSourcePath = effectInfo.getPath();
                    if (index == 0) {
                        filterSourcePath = null;
                    }
                    EffectFilter filterEffect = new EffectFilter(filterSourcePath);
                    recorder.applyFilter(filterEffect);
                    mConflictEffects.put(TYPE_FILTER, filterEffect);
                }
                currentFilterPosition = index;
            }
        });
        filterEffectChooser.setFilterPosition(currentFilterPosition);
        filterEffectChooser.setDismissListener(new DialogVisibleListener() {
            @Override
            public void onDialogDismiss() {
                mControlView.setEffectSelViewShow(false);
            }

            @Override
            public void onDialogShow() {
                mControlView.setEffectSelViewShow(true);
            }
        });
        filterEffectChooser.show(getFragmentManager(), TAG_FILTER_CHOOSER);
    }
    /**
     * 显示美颜调节的控件
     */
    private void showBeautyFaceView() {
        if (beautyEffectChooser == null) {
            beautyEffectChooser = new BeautyEffectChooser();
        }
        currentBeautySkinPosition = SharedPreferenceUtils.getBeautySkinLevel(getContext());



        // 美颜item选中listener
        beautyEffectChooser.setOnBeautyFaceItemSeletedListener(new OnBeautyFaceItemSeletedListener() {
            @Override
            public void onNormalSelected(int postion, BeautyLevel beautyLevel) {
                defaultBeautyLevel = beautyLevel;
                currentBeautyFaceNormalPosition = postion;
                // 普通美颜
                recorder.setBeautyLevel(beautyLevel.getValue());

                if (beautyService != null) {
                    beautyService.saveSelectParam(getContext(), currentBeautyFaceNormalPosition, currentBeautyFacePosition, currentBeautySkinPosition);
                }
            }

            @Override
            public void onAdvancedSelected(int position, BeautyLevel beautyLevel) {
                currentBeautyFacePosition = position;
                // 高级美颜
                BeautyParams beautyParams = rememberParamList.get(position);
                //// 美白和红润faceUnity的值范围 0~1.0f
                if (beautyService != null) {
                    beautyService.setBeautyParam(beautyParams, BeautyService.BEAUTY_FACE);
                    beautyService.saveSelectParam(getContext(), currentBeautyFaceNormalPosition, currentBeautyFacePosition, currentBeautySkinPosition);
                }
            }
        });

        // 美肌item选中
        beautyEffectChooser.setOnBeautySkinSelectedListener(new OnBeautySkinItemSeletedListener() {
            @Override
            public void onItemSelected(int postion) {
                currentBeautySkinPosition = postion;
                BeautyParams beautyParams = rememberParamList.get(postion);
                if (beautyService != null) {
                    beautyService.setBeautyParam(beautyParams, BeautyService.BEAUTY_SKIN);
                    beautyService.saveSelectParam(getContext(), currentBeautyFaceNormalPosition, currentBeautyFacePosition, currentBeautySkinPosition);
                }
            }
        });

        // 美颜微调dialog
        beautyEffectChooser.setOnBeautyFaceDetailClickListener(new OnBeautyDetailClickListener() {
            @Override
            public void onDetailClick() {
                beautyEffectChooser.dismiss();
                mControlView.setEffectSelViewShow(true);
                showBeautyFaceDetailDialog();

            }
        });

        // 美肌微调dialog
        beautyEffectChooser.setOnBeautySkinDetailClickListener(new OnBeautyDetailClickListener() {
            @Override
            public void onDetailClick() {
                beautyEffectChooser.dismiss();
                mControlView.setEffectSelViewShow(true);
                showBeautySkinDetailDialog();
            }
        });

        // 美颜普通和高级模式切换
        beautyEffectChooser.setOnBeautyModeChangeListener(new OnBeautyModeChangeListener() {
            @Override
            public void onModeChange(RadioGroup group, int checkedId) {

                BeautyParams beautyParams = null;
                if (checkedId == R.id.rb_level_advanced) {
                    currentBeautyFaceMode = BeautyMode.Advanced;
                    recorder.setBeautyStatus(false);
                    beautyParams = rememberParamList.get(currentBeautyFacePosition);
                    if (beautyService != null) {
                        beautyService.setBeautyParam(beautyParams, BeautyService.BEAUTY_FACE);
                    }
                } else if (checkedId == R.id.rb_level_normal) {
                    currentBeautyFaceMode = BeautyMode.Normal;
                    recorder.setBeautyStatus(true);
                    recorder.setBeautyLevel(defaultBeautyLevel.getValue());
                }

                if (beautyService != null) {
                    beautyService.saveBeautyMode(getContext(), currentBeautyFaceMode);
                }
            }
        });

        beautyEffectChooser.setDismissListener(new DialogVisibleListener() {
            @Override
            public void onDialogDismiss() {
                // 如果微调的页面不在显示状态,
                if (!isbeautyDetailShowing) {
                    mControlView.setEffectSelViewShow(false);
                } else {
                    mControlView.setEffectSelViewShow(true);
                }
                isbeautyDetailBack = false;

                if (beautyService != null) {
                    beautyService.saveBeautyMode(getContext(), currentBeautyFaceMode);
                    beautyService.saveSelectParam(getContext(), currentBeautyFaceNormalPosition, currentBeautyFacePosition, currentBeautySkinPosition);
                }
            }

            @Override
            public void onDialogShow() {
                mControlView.setEffectSelViewShow(true);
                beautyEffectChooser.setBeautyParams(AliyunSVideoRecordView.this.beautyParams);

            }
        });


        beautyEffectChooser.show(getFragmentManager(), TAG_BEAUTY_CHOOSER);
    }

    /**
     * 显示美颜微调dialog
     */
    private void showBeautyFaceDetailDialog() {
        beautyParams = getBeautyParams(currentBeautyFacePosition);
        if (beautyParams == null) {
            beautyParams = rememberParamList.get(currentBeautyFacePosition);
        }
        beautyFaceDetailChooser = new BeautyFaceDetailChooser();
        beautyFaceDetailChooser.setBeautyLevel(currentBeautyFacePosition);
        beautyFaceDetailChooser.setOnBeautyParamsChangeListener(
        new OnBeautyParamsChangeListener() {
            @Override
            public void onBeautyChange(BeautyParams param) {
                if (beautyParams != null && param != null) {
                    beautyParams.beautyWhite = param.beautyWhite;
                    beautyParams.beautyBuffing = param.beautyBuffing;
                    beautyParams.beautyRuddy = param.beautyRuddy;
                    if (faceUnityManager != null) {
                        // 美白
                        faceUnityManager.setFaceBeautyWhite(param.beautyWhite / 100);
                        // 红润
                        faceUnityManager.setFaceBeautyRuddy(param.beautyRuddy / 100);
                        // 磨皮
                        faceUnityManager.setFaceBeautyBuffing(param.beautyBuffing / 10);
                    }
                }
            }
        });
        // 点击back按钮
        beautyFaceDetailChooser.setOnBackClickListener(new OnViewClickListener() {
            @Override
            public void onClick() {
                isbeautyDetailShowing = false;
                isbeautyDetailBack = true;
                beautyFaceDetailChooser.dismiss();
                beautyEffectChooser.show(getFragmentManager(), TAG_BEAUTY_CHOOSER);
            }
        });
        // 空白区域点击
        beautyFaceDetailChooser.setOnBlankClickListener(new BeautyDetailSettingView.OnBlanckViewClickListener() {
            @Override
            public void onBlankClick() {
                isbeautyDetailShowing = false;
            }
        });
        beautyFaceDetailChooser.setDismissListener(new DialogVisibleListener() {
            @Override
            public void onDialogDismiss() {
                // 如果是点击微调界面中的back按钮, controlview的底部view仍要保持隐藏状态
                if (isbeautyDetailBack) {
                    mControlView.setEffectSelViewShow(true);
                } else {
                    mControlView.setEffectSelViewShow(false);
                }
                saveBeautyParams(currentBeautyFacePosition, beautyParams);
                isbeautyDetailBack = false;
            }

            @Override
            public void onDialogShow() {}
        });
        beautyFaceDetailChooser.setBeautyParams(beautyParams);
        beautyEffectChooser.dismiss();
        isbeautyDetailShowing = true;
        beautyFaceDetailChooser.show(getFragmentManager(), TAG_BEAUTY_DETAIL_FACE_CHOOSER);
    }


    /**
     * 显示美肌微调dialog
     */
    private void showBeautySkinDetailDialog() {
        beautyParams = getBeautyParams(currentBeautySkinPosition);
        if (beautyParams == null) {
            beautyParams = rememberParamList.get(currentBeautySkinPosition);
        }

        beautySkinDetailChooser = new BeautySkinDetailChooser();
        beautySkinDetailChooser.setBeautyLevel(currentBeautySkinPosition);
        beautySkinDetailChooser.setOnBeautyParamsChangeListener(
        new OnBeautyParamsChangeListener() {
            @Override
            public void onBeautyChange(BeautyParams param) {
                if (beautyParams != null && param != null) {

                    beautyParams.beautyBigEye = param.beautyBigEye;
                    beautyParams.beautySlimFace = param.beautySlimFace;
                    if (faceUnityManager != null) {
                        //大眼
                        faceUnityManager.setFaceBeautyBigEye(param.beautyBigEye / 100);
                        //瘦脸
                        faceUnityManager.setFaceBeautySlimFace(param.beautySlimFace / 100 * 1.5f);
                    }
                    saveBeautyParams(currentBeautySkinPosition, beautyParams);

                }
            }
        });

        // 点击back按钮
        beautySkinDetailChooser.setOnBackClickListener(new OnViewClickListener() {
            @Override
            public void onClick() {
                isbeautyDetailShowing = false;
                beautySkinDetailChooser.dismiss();
                isbeautyDetailBack = true;
                beautyEffectChooser.show(getFragmentManager(), TAG_BEAUTY_CHOOSER);
            }
        });

        // 空白区域点击
        beautySkinDetailChooser.setOnBlankClickListener(new BeautyDetailSettingView.OnBlanckViewClickListener() {
            @Override
            public void onBlankClick() {
                isbeautyDetailShowing = false;
            }
        });

        beautySkinDetailChooser.setDismissListener(new DialogVisibleListener() {
            @Override
            public void onDialogDismiss() {
                // 如果是点击微调界面中的back按钮, controlview的底部view仍要保持隐藏状态
                if (isbeautyDetailBack) {
                    mControlView.setEffectSelViewShow(true);
                } else {
                    mControlView.setEffectSelViewShow(false);
                }
                saveBeautyParams(currentBeautySkinPosition, AliyunSVideoRecordView.this.beautyParams);
                isbeautyDetailBack = false;
            }

            @Override
            public void onDialogShow() {

            }
        });
        beautySkinDetailChooser.setBeautyParams(beautyParams);
        beautyEffectChooser.dismiss();
        isbeautyDetailShowing = true;
        beautySkinDetailChooser.show(getFragmentManager(), TAG_BEAUTY_DETAIL_SKIN_CHOOSER);
    }

    private void initRecorder() {
        recorder = AliyunRecorderCreator.getRecorderInstance(getContext());
        recorder.setDisplayView(mSurfaceView);
        clipManager = recorder.getClipManager();
        recorder.setFocusMode(CameraParam.FOCUS_MODE_CONTINUE);
        clipManager.setMaxDuration(MAX_RECORD_TIME);
        clipManager.setMinDuration(getMaxRecordTime());
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setVideoWidth(TEST_VIDEO_WIDTH);
        mediaInfo.setVideoHeight(TEST_VIDEO_HEIGHT);
        //mediaInfo.setHWAutoSize(true);//硬编时自适应宽高为16的倍数
        recorder.setMediaInfo(mediaInfo);
        cameraType = recorder.getCameraCount() == 1 ? com.aliyun.svideo.sdk.external.struct.recorder.CameraType.BACK
                     : cameraType;
        recorder.setCamera(cameraType);
        recorder.setBeautyStatus(false);
        initOritationDetector();
        recorder.setOnFrameCallback(new OnFrameCallBack() {
            @Override
            public void onFrameBack(byte[] bytes, int width, int height, Camera.CameraInfo info) {
                //原始数据回调 NV21,这里获取原始数据主要是为了faceUnity高级美颜使用
                frameBytes = bytes;
                frameWidth = width;
                frameHeight = height;
            }

            @Override
            public Camera.Size onChoosePreviewSize(List<Camera.Size> supportedPreviewSizes,
                                                   Camera.Size preferredPreviewSizeForVideo) {

                return null;
            }

            @Override
            public void openFailed() {
                Log.e(AliyunTag.TAG, "openFailed----------");
                isOpenFailed = true;
            }
        });

        recorder.setRecordCallback(new RecordCallback() {
            @Override
            public void onComplete(final boolean validClip, final long clipDuration) {
                Log.e(TAG, "onComplete   duration : " + clipDuration +
                      ", clipManager.getDuration() = " + clipManager.getDuration());
                tempIsComplete = true;
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "onComplete    isStopToCompleteDuration:" + isStopToCompleteDuration);

                        isStopToCompleteDuration = false;
                        handleStopCallback(validClip, clipDuration);
                        if (isMaxDuration && validClip) {
                            finishRecording();
                        }

                    }
                });

            }

            /**
             * 合成完毕的回调
             * @param outputPath
             */
            @Override
            public void onFinish(final String outputPath) {
                Log.e(TAG, "onFinish:" + outputPath);

                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCompleteListener != null) {
                            final int duration = clipManager.getDuration();
                            //deleteAllPart();
                            // 选择音乐后, 在录制完合成过程中退后台
                            // 保持在后台情况下, sdk合成完毕后, 会仍然执行跳转代码, 此时会弹起跳转后的页面
                            if (activityStoped) {
                                pendingCompseFinishRunnable =  new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!isStopToCompleteDuration) {
                                            mCompleteListener.onComplete(outputPath, duration);
                                        }
                                    }
                                };
                            } else {
                                if (!isStopToCompleteDuration) {
                                    mCompleteListener.onComplete(outputPath, duration);
                                }
                            }
                        }
                    }
                });

            }

            @Override
            public void onProgress(final long duration) {
                final int currentDuration = clipManager.getDuration();
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isAllowChangeMv = false;
                        recordTime = 0;
                        //设置录制进度
                        if (mRecordTimeView != null) {
                            mRecordTimeView.setDuration((int)duration);
                        }

                        recordTime = (int) (currentDuration + duration);
                        if (recordTime <= clipManager.getMaxDuration() && recordTime >= clipManager.getMinDuration()) {
                            // 2018/7/11 让下一步按钮可点击
                            mControlView.setCompleteEnable(true);
                        } else {
                            mControlView.setCompleteEnable(false);
                        }
                        if (mControlView != null && mControlView.getRecordState().equals(RecordState.STOP)) {
                            return;
                        }
                        if (mControlView != null) {
                            mControlView.setRecordTime(TimeFormatterUtils.formatTime(recordTime));
                        }

                    }
                });

            }

            @Override
            public void onMaxDuration() {
                Log.e(TAG, "onMaxDuration:");
                isMaxDuration = true;
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mControlView != null) {
                            mControlView.setCompleteEnable(false);
                            mControlView.setRecordState(RecordState.STOP);
                            mControlView.updataCutDownView(false);
                        }
                    }
                });

            }

            @Override
            public void onError(int errorCode) {
                Log.e(TAG, "onError:" + errorCode);
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recordTime = 0;
                        tempIsComplete = true;
                        handleStopCallback(false, 0);
                    }
                });
            }

            @Override
            public void onInitReady() {
                Log.e(TAG, "onInitReady");
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        restoreConflictEffect();
                        if (effectPaster != null) {
                            addEffectToRecord(effectPaster.getPath());
                        }

                    }
                });
            }

            @Override
            public void onDrawReady() {

            }

            @Override
            public void onPictureBack(final Bitmap bitmap) {

            }

            @Override
            public void onPictureDataBack(final byte[] data) {

            }

        });
        recorder.setOnTextureIdCallback(new OnTextureIdCallBack() {
            @Override
            public int onTextureIdBack(int textureId, int textureWidth, int textureHeight, float[] matrix) {

                //******************************** start ******************************************
                //这块代码会影响到标准版的faceUnity功能 改动的时候要关联app gradle 一起改动
                if (faceInitResult && currentBeautyFaceMode == BeautyMode.Advanced && faceUnityManager != null) {
                    /**
                     * faceInitResult fix bug:反复退出进入会出现黑屏情况,原因是因为release之后还在调用渲染的接口,必须要保证release了之后不能再调用渲染接口
                     */
                    return faceUnityManager.draw(frameBytes, mFuImgNV21Bytes, textureId, frameWidth, frameHeight, mFrameId++, mControlView.getCameraType().getType());
                }
                //******************************** end ********************************************
                return textureId;
            }

            OpenGLTest test;

            @Override
            public int onScaledIdBack(int scaledId, int textureWidth, int textureHeight, float[] matrix) {
                //if (test == null) {
                //    test = new OpenGLTest();
                //}

                return scaledId;
            }

            @Override
            public void onTextureDestroyed() {
                // sdk3.7.8改动, 自定义渲染（第三方渲染）销毁gl资源，以前GLSurfaceView时可以通过GLSurfaceView.queueEvent来做，
                // 现在增加了一个gl资源销毁的回调，需要统一在这里面做。
                if (faceUnityManager != null && faceInitResult) {
                    faceUnityManager.release();
                    faceInitResult = false;
                }
            }
        });

        recorder.setEncoderInfoCallback(new EncoderInfoCallback() {
            @Override
            public void onEncoderInfoBack(EncoderInfo info) {
            }
        });
        recorder.setFaceTrackInternalMaxFaceCount(2);
    }

    /**
     * 美颜默认选中高级, 3档 美白: 0.6 红润: 0.6 磨皮: 6 大眼: 0.6 瘦脸: 0.6 * 1.5 (总范围0~1.5)
     */
    private void faceunityDefaultParam() {
        beautyService = new BeautyService();
        beautyService.bindFaceUnity(getContext(), faceUnityManager);

    }

    public void setFaceTrackModePath() {
        ThreadUtils.runOnSubThread(new Runnable() {
            @Override
            public void run() {
                String path = StorageUtils.getCacheDirectory(getContext()).getAbsolutePath() + File.separator + Common.QU_NAME + File.separator;
                if (recorder != null) {
                    recorder.needFaceTrackInternal(true);
                    recorder.setFaceTrackInternalModelPath(path + "/model");
                }
            }
        });

    }

    public void setRecordMute(boolean recordMute) {
        if (recorder != null) {
            recorder.setMute(recordMute);
        }
    }

    private void initOritationDetector() {
        orientationDetector = new OrientationDetector(getContext().getApplicationContext());
        orientationDetector.setOrientationChangedListener(new OrientationDetector.OrientationChangedListener() {
            @Override
            public void onOrientationChanged() {
                rotation = getPictureRotation();
                recorder.setRotation(rotation);
            }
        });
    }

    private int getPictureRotation() {
        int orientation = orientationDetector.getOrientation();
        int rotation = 90;
        if ((orientation >= 45) && (orientation < 135)) {
            rotation = 180;
        }
        if ((orientation >= 135) && (orientation < 225)) {
            rotation = 270;
        }
        if ((orientation >= 225) && (orientation < 315)) {
            rotation = 0;
        }
        if (cameraType == com.aliyun.svideo.sdk.external.struct.recorder.CameraType.FRONT) {
            if (rotation != 0) {
                rotation = 360 - rotation;
            }
        }
        return rotation;
    }


    private boolean activityStoped;
    private Runnable pendingCompseFinishRunnable;

    /**
     * 开始预览
     */
    public void startPreview() {
        activityStoped = false;
        if (pendingCompseFinishRunnable != null) {
            pendingCompseFinishRunnable.run();
        }
        pendingCompseFinishRunnable = null;
        if (recorder != null) {
            recorder.startPreview();
            if (isAllowChangeMv) {
                restoreConflictEffect();
            }
            //            recorder.setZoom(scaleFactor);
            if (clipManager.getDuration() >= clipManager.getMinDuration()) {
                // 2018/7/11 让下一步按钮可点击
                mControlView.setCompleteEnable(true);
            } else {
                mControlView.setCompleteEnable(false);
            }
        }
        if (orientationDetector != null && orientationDetector.canDetectOrientation()) {
            orientationDetector.enable();
        }

        mCountDownView.setOnCountDownFinishListener(new AlivcCountDownView.OnCountDownFinishListener() {
            @Override
            public void onFinish() {
                FixedToastUtils.show(getContext(), "开始录制");
                startRecord();
            }
        });

    }

    /**
     * 结束预览
     */
    public void stopPreview() {
        activityStoped = true;
        if (mControlView != null && mCountDownView != null && mControlView.getRecordState().equals(RecordState.READY)) {
            mCountDownView.cancle();
            mControlView.setRecordState(RecordState.STOP);
            mControlView.setRecording(false);
        }

        if (mControlView != null && mControlView.getRecordState().equals(RecordState.RECORDING)) {
            recorder.stopRecording();
        }
        recorder.stopPreview();

        if (beautyEffectChooser != null) {
            beautyEffectChooser.dismiss();
        }

        if (mFaceUnityTask != null) {
            mFaceUnityTask.cancel(true);
            mFaceUnityTask = null;
        }
        if (orientationDetector != null) {
            orientationDetector.disable();
        }

        if (mControlView != null && mControlView.getFlashType() == FlashType.ON
                && mControlView.getCameraType() == CameraType.BACK) {
            mControlView.setFlashType(FlashType.OFF);
        }
    }

    /**
     * 销毁录制，在activity或者fragment被销毁时调用此方法
     */
    public void destroyRecorder() {

        if (finishRecodingTask != null) {
            finishRecodingTask.cancel(true);
            finishRecodingTask = null;
        }

        if (faceTrackPathTask != null) {
            faceTrackPathTask.cancel(true);
            faceTrackPathTask = null;
        }

        if (beautyParamCopyTask != null) {
            beautyParamCopyTask.cancel(true);
            beautyParamCopyTask = null;
        }

        if (recorder != null) {
            recorder.destroy();
            Log.i(TAG, "destroy");
        }

        if (orientationDetector != null) {
            orientationDetector.setOrientationChangedListener(null);
        }

    }

    /**
     * 结束录制，并且将录制片段视频拼接成一个视频 跳转editorActivity在合成完成的回调的方法中，{@link AlivcSvideoRecordActivity#onResume()}
     */
    private void finishRecording() {
        //弹窗提示
        if (progressBar == null) {
            progressBar = new ProgressDialog(getContext());
            progressBar.setMessage("视频合成中....");
            progressBar.setCanceledOnTouchOutside(false);
            progressBar.setCancelable(false);
            progressBar.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER);
        }
        progressBar.show();
        mControlView.setCompleteEnable(false);
        finishRecodingTask = new FinishRecodingTask(this).executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * 录制结束的AsyncTask
     */
    public static class FinishRecodingTask extends AsyncTask<Void, Void, Void> {
        WeakReference<AliyunSVideoRecordView> weakReference;

        FinishRecodingTask(AliyunSVideoRecordView recordView) {
            weakReference = new WeakReference<>(recordView);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (weakReference == null) {
                return null;
            }

            AliyunSVideoRecordView recordView = weakReference.get();
            if (recordView != null) {
                recordView.recorder.finishRecording();
                Log.e(TAG, "finishRecording");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (weakReference == null) {
                return;
            }
            AliyunSVideoRecordView recordView = weakReference.get();
            if (recordView != null) {
                if (recordView.progressBar != null) {
                    recordView.progressBar.dismiss();
                }
            }
        }
    }

    /**
     * 片段录制完成的回调处理
     *
     * @param isValid
     * @param duration
     */
    private void handleStopCallback(final boolean isValid, final long duration) {
        post(new Runnable() {
            @Override
            public void run() {

                mControlView.setRecordState(RecordState.STOP);
                if (mControlView != null) {
                    mControlView.setRecording(false);
                }

                if (!isValid) {
                    if (mRecordTimeView != null) {
                        mRecordTimeView.setDuration(0);

                        if (mRecordTimeView.getTimelineDuration() == 0) {
                            mControlView.setHasRecordPiece(false);
                        }
                    }
                    return;
                }

                if (duration > 200) {
                    if (mRecordTimeView != null) {
                        mRecordTimeView.setDuration((int)duration);
                        mRecordTimeView.clipComplete();
                    }
                    mControlView.setHasRecordPiece(true);
                    isAllowChangeMv = false;
                } else {

                    if (mRecordTimeView != null) {
                        mRecordTimeView.setDuration(0);
                    }
                    //todo 小于200毫秒的视频会导致合成视频出现异常，这里会做删除
                    clipManager.deletePart();
                    if (clipManager.getDuration() == 0) {
                        isAllowChangeMv = true;
                        mControlView.setHasRecordPiece(false);
                    }
                    isMaxDuration = false;
                }

            }
        });
    }

    /**
     * addSubView 添加子view到布局中
     *
     * @param view 子view
     */
    private void addSubView(View view) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(view, params);//添加到布局中
    }

    /**
     * 录制界面返回按钮click listener
     */
    private OnBackClickListener mBackClickListener;

    public void setBackClickListener(OnBackClickListener listener) {
        this.mBackClickListener = listener;
    }

    private OnFinishListener mCompleteListener;

    @Override
    public void onDialogDismiss() {
        if (mControlView != null) {
            mControlView.setEffectSelViewShow(false);
        }
    }

    @Override
    public void onDialogShow() {
        if (mControlView != null) {
            mControlView.setEffectSelViewShow(true);
        }
    }

    /**
     * 删除所有录制文件
     */
    public void deleteAllPart() {
        if (clipManager != null) {
            clipManager.deleteAllPart();
            if (clipManager.getDuration() < clipManager.getMinDuration() && mControlView != null) {
                mControlView.setCompleteEnable(false);
            }
            if (clipManager.getDuration() == 0) {
                // 音乐可以选择
                //                    musicBtn.setVisibility(View.VISIBLE);
                //                    magicMusic.setVisibility(View.VISIBLE);
                //recorder.restartMv();
                mRecordTimeView.clear();
                mControlView.setHasRecordPiece(false);
                isAllowChangeMv = true;
            }
        }
    }

    private float lastScaleFactor;
    private float scaleFactor;

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float factorOffset = detector.getScaleFactor() - lastScaleFactor;
        scaleFactor += factorOffset;
        lastScaleFactor = detector.getScaleFactor();
        if (scaleFactor < 0) {
            scaleFactor = 0;
        }
        if (scaleFactor > 1) {
            scaleFactor = 1;
        }
        recorder.setZoom(scaleFactor);
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        lastScaleFactor = detector.getScaleFactor();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    /**
     * 返回按钮事件监听
     */
    public interface OnBackClickListener {
        void onClick();
    }

    /**
     * 录制完成事件监听
     */
    public interface OnFinishListener {
        void onComplete(String path, int duration);
    }

    public void setCompleteListener(OnFinishListener mCompleteListener) {
        this.mCompleteListener = mCompleteListener;
    }

    public void setActivity(FragmentActivity mActivity) {
        this.mActivity = mActivity;
    }

    /**
     * 获取最大录制时长
     *
     * @return
     */
    public int getMaxRecordTime() {
        if (maxRecordTime < MIN_RECORD_TIME) {
            return MIN_RECORD_TIME;
        } else if (maxRecordTime > MAX_RECORD_TIME) {
            return MAX_RECORD_TIME;
        } else {

            return maxRecordTime;
        }

    }

    /**
     * 设置录制时长
     *
     * @param maxRecordTime
     */
    public void setMaxRecordTime(int maxRecordTime) {
        this.maxRecordTime = maxRecordTime;
        if (clipManager != null) {
            clipManager.setMaxDuration(getMaxRecordTime());
        }
        if (mRecordTimeView != null) {
            mRecordTimeView.setMaxDuration(getMaxRecordTime());
        }
    }

    /**
     * 设置最小录制时长
     *
     * @param minRecordTime
     */
    public void setMinRecordTime(int minRecordTime) {
        this.minRecordTime = minRecordTime;
        if (clipManager != null) {
            clipManager.setMinDuration(minRecordTime);
        }
        if (mRecordTimeView != null) {
            mRecordTimeView.setMinDuration(minRecordTime);
        }
    }

    /**
     * 设置码率
     *
     * @param mBitrate
     */
    public void setBitrate(int mBitrate) {
        this.mBitrate = mBitrate;
        if (recorder != null) {
            recorder.setVideoBitrate(mBitrate);
        }
    }

    /**
     * 设置Gop
     *
     * @param mGop
     */
    public void setGop(int mGop) {
        this.mGop = mGop;
        if (recorder != null) {
            recorder.setGop(mGop);
        }
    }

    /**
     * 设置视频质量
     *
     * @param mVideoQuality
     */
    public void setVideoQuality(VideoQuality mVideoQuality) {
        this.mVideoQuality = mVideoQuality;
        if (recorder != null) {
            recorder.setVideoQuality(mVideoQuality);
        }
    }

    /**
     * 设置视频比例
     *
     * @param mRatioMode
     */
    public void setRatioMode(int mRatioMode) {
        this.mRatioMode = mRatioMode;
        if (recorder != null) {
            recorder.setMediaInfo(getMediaInfo());

        }
        if (mSurfaceView != null) {
            LayoutParams params = (LayoutParams)mSurfaceView.getLayoutParams();
            int screenWidth = ScreenUtils.getRealWidth(getContext());
            int height = 0;
            int top;
            switch (mRatioMode) {
            case AliyunSnapVideoParam.RATIO_MODE_1_1:
                //视频比例为1：1的时候，录制界面向下移动，移动位置为顶部菜单栏的高度
                top = getContext().getResources().getDimensionPixelSize(R.dimen.alivc_record_title_height);
                params.setMargins(0, top, 0, 0 );
                height = screenWidth;
                break;
            case AliyunSnapVideoParam.RATIO_MODE_3_4:
                //视频比例为3：4的时候，录制界面向下移动，移动位置为顶部菜单栏的高度
                top = getContext().getResources().getDimensionPixelSize(R.dimen.alivc_record_title_height);
                params.setMargins(0, top, 0, 0 );
                height = screenWidth * 4 / 3;
                break;
            case AliyunSnapVideoParam.RATIO_MODE_9_16:

                int screenHeight = ScreenUtils.getRealHeight(getContext());
                float screenRatio = screenWidth / (float)screenHeight;
                if (screenRatio >= 9 / 16f) {
                    //胖手机宽高比小于9/16
                    params.width = screenWidth;
                    height = screenWidth * 16 / 9;
                } else {
                    height = screenHeight;
                    params.width = screenHeight * 9 / 16;
                }
                Log.e("RealHeight", "height:" + screenHeight + "width:" + screenWidth);
                params.gravity = Gravity.CENTER;
                break;
            default:
                height = screenWidth * 16 / 9;
                break;
            }
            params.height = height;
            mSurfaceView.setLayoutParams(params);
        }

    }

    /**
     * 设置视频编码方式
     *
     * @param mVideoCodec
     */
    public void setVideoCodec(VideoCodecs mVideoCodec) {
        this.mVideoCodec = mVideoCodec;
        if (recorder != null) {
            recorder.setMediaInfo(getMediaInfo());
        }

    }

    /**
     * 设置视频码率
     *
     * @param mResolutionMode
     */
    public void setResolutionMode(int mResolutionMode) {
        this.mResolutionMode = mResolutionMode;
        if (recorder != null) {
            recorder.setMediaInfo(getMediaInfo());
        }
    }

    /**
     * 设置生产录制视频的地址
     * @param path
     */
    public void setOutputVideoPath(String path){
        videoPath = path;
        File parentFile = new File(videoPath).getParentFile();
        if (parentFile!=null&&!parentFile.exists()) {
            parentFile.mkdirs();
        }
        recorder.setOutputPath(videoPath);
    }
    private void saveBeautyParams(int position, BeautyParams beautyParams) {
        if (beautyParams != null) {
            Gson gson = new Gson();

            rememberParamList.set(position, beautyParams);
            rememberBeautyBean.setBeautyList(rememberParamList);
            String jsonString = gson.toJson(rememberBeautyBean);

            if (!TextUtils.isEmpty(jsonString)) {
                SharedPreferenceUtils.setBeautyParams(getContext(), jsonString);
            }
        }
    }

    /**
     * 获取美颜美肌参数
     */
    private BeautyParams getBeautyParams(int position) {
        String jsonString = SharedPreferenceUtils.getBeautyParams(getContext());
        if (TextUtils.isEmpty(jsonString)) {
            return null;
        }
        Gson gson = new Gson();
        RememberBeautyBean rememberBeautyBean = gson.fromJson(jsonString, RememberBeautyBean.class);
        List<BeautyParams> beautyList = rememberBeautyBean.getBeautyList();
        if (beautyList == null) {
            return null;
        }
        return beautyList.get(position);
    }

    private MediaInfo getMediaInfo() {
        MediaInfo info = new MediaInfo();
        info.setFps(35);
        info.setVideoWidth(getVideoWidth());
        info.setVideoHeight(getVideoHeight());
        info.setVideoCodec(mVideoCodec);
        info.setCrf(0);
        return info;
    }

    /**
     * 获取拍摄视频宽度
     *
     * @return
     */
    private int getVideoWidth() {
        int width = 0;
        switch (mResolutionMode) {
        case AliyunSnapVideoParam.RESOLUTION_360P:
            width = 360;
            break;
        case AliyunSnapVideoParam.RESOLUTION_480P:
            width = 480;
            break;
        case AliyunSnapVideoParam.RESOLUTION_540P:
            width = 540;
            break;
        case AliyunSnapVideoParam.RESOLUTION_720P:
            width = 720;
            break;
        default:
            width = 540;
            break;
        }

        return width;
    }

    private int getVideoHeight() {
        int width = getVideoWidth();
        int height = 0;
        switch (mRatioMode) {
        case AliyunSnapVideoParam.RATIO_MODE_1_1:
            height = width;
            break;
        case AliyunSnapVideoParam.RATIO_MODE_3_4:
            height = width * 4 / 3;
            break;
        case AliyunSnapVideoParam.RATIO_MODE_9_16:
            height = width * 16 / 9;
            break;
        default:
            height = width;
            break;
        }
        return height;
    }
    public void setOnMusicSelectListener(MusicSelectListener musicSelectListener) {
        this.mOutMusicSelectListener =  musicSelectListener;
    }

}

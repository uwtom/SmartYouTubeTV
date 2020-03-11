package com.liskovsoft.smartyoutubetv.flavors.exoplayer.interceptors;

import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv.CommonApplication;
import com.liskovsoft.smartyoutubetv.misc.myquerystring.MyUrlEncodedQueryString;
import com.liskovsoft.smartyoutubetv.prefs.SmartPreferences;

public class BackgroundActionManager {
    private static final String TAG = BackgroundActionManager.class.getSimpleName();
    private static final long SAME_VIDEO_NO_INTERACTION_TIMEOUT_MS = 1_000;
    private static final String PARAM_VIDEO_ID = "video_id";
    private static final String PARAM_PLAYLIST_ID = "list";
    private static final String PARAM_MIRROR = "ytr";
    /**
     * fix playlist advance bug<br/>
     * create time window (1sec) where get_video_info isn't allowed<br/>
     * see {@link ExoInterceptor#intercept(String)} method
     */
    private long mExitTime;
    private boolean mIsOpened;
    private String mCurrentUrl;
    private boolean mSameVideo;
    private final SmartPreferences mPrefs;

    public BackgroundActionManager() {
        mPrefs = CommonApplication.getPreferences();
    }

    public boolean cancelPlayback() {
        if (mCurrentUrl == null || !mCurrentUrl.contains(ExoInterceptor.URL_VIDEO_DATA)) {
            Log.d(TAG, "Cancel playback: No video data.");
            return true;
        }

        if (getVideoId(mCurrentUrl) == null) {
            Log.d(TAG, "Cancel playback: Supplied url doesn't contain video info.");
            return true;
        }


        if (mSameVideo && mIsOpened) {
            Log.d(TAG, "Cancel playback: Same video.");
            return true;
        }

        boolean closedRecently = (System.currentTimeMillis() - mExitTime) < SAME_VIDEO_NO_INTERACTION_TIMEOUT_MS;

        if (closedRecently) {
            Log.d(TAG, "Cancel playback: Video closed recently.");
            return true;
        }

        return false;
    }

    public void onClose() {
        Log.d(TAG, "Video is closed");
        mExitTime = System.currentTimeMillis();
        //mPrevVideoId = null;
        mIsOpened = false;
    }

    public void onCancel() {
        Log.d(TAG, "Video is canceled");
        //mPrevVideoId = null;
        mIsOpened = false;
    }

    public void init(String url) {
        //onInitMeasure();
        recordUrl(url);

        mCurrentUrl = url;
    }

    private void recordUrl(String url) {
        String prevVideoId = getVideoId(mCurrentUrl);
        String currentVideoId = getVideoId(url);

        if (prevVideoId != null) {
            mSameVideo = prevVideoId.equals(currentVideoId);
        }

        if (mSameVideo) {
            Log.d(TAG, "The same video encountered");
        }
    }

    public void onOpen() {
        Log.d(TAG, "Video has been opened");
        mIsOpened = true;
    }

    public boolean isOpened() {
        return mIsOpened;
    }

    private boolean isMirroring(String url) {
        String mirrorDeviceName = MyUrlEncodedQueryString.parse(url).get(PARAM_MIRROR);

        if (mirrorDeviceName != null && !mirrorDeviceName.isEmpty()) { // any response is good
            Log.d(TAG, "The video is mirroring from the phone or tablet");
            return true;
        }

        return false;
    }

    public boolean isMirroring() {
        if (mCurrentUrl == null) {
            return false;
        }

        return isMirroring(mCurrentUrl);
    }

    public String getVideoId(String url) {
        if (url == null) {
            return null;
        }

        return MyUrlEncodedQueryString.parse(url).get(PARAM_VIDEO_ID);
    }

    public String getPlaylistId(String url) {
        if (url == null) {
            return null;
        }

        return MyUrlEncodedQueryString.parse(url).get(PARAM_PLAYLIST_ID);
    }
}

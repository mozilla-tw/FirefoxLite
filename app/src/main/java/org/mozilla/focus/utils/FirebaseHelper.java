/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Looper;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import org.jetbrains.annotations.NotNull;
import org.mozilla.fileutils.FileUtils;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.home.FeatureSurveyViewHelper;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.notification.RocketMessagingService;
import org.mozilla.focus.screenshot.ScreenshotManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.rocket.content.NewsSourceManager;
import org.mozilla.rocket.periodic.FirstLaunchWorker;
import org.mozilla.threadutils.ThreadUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import static org.mozilla.rocket.widget.NewsSourcePreference.PREF_INT_NEWS_PRIORITY;

/**
 * Implementation for FirebaseWrapper. It's job:
 * 1. Call init() to start the wrapper in a background thread
 * 2. Implement getRemoteConfigDefault to provide Remote Config default value
 */
final public class FirebaseHelper extends FirebaseWrapper {

    private static final String TAG = "FirebaseHelper";

    // keys for remote config default value
    public static final String RATE_APP_DIALOG_TEXT_TITLE = "rate_app_dialog_text_title";
    public static final String RATE_APP_DIALOG_TEXT_CONTENT = "rate_app_dialog_text_content";
    public static final String RATE_APP_DIALOG_TEXT_POSITIVE = "rate_app_dialog_text_positive";
    public static final String RATE_APP_DIALOG_TEXT_NEGATIVE = "rate_app_dialog_text_negative";

    // Key for local broadcast
    public static final String FIREBASE_READY = "Firebase_ready";

    static final String RATE_APP_DIALOG_THRESHOLD = "rate_app_dialog_threshold";
    static final String RATE_APP_NOTIFICATION_THRESHOLD = "rate_app_notification_threshold";
    static final String SHARE_APP_DIALOG_THRESHOLD = "share_app_dialog_threshold";
    static final String ENABLE_MY_SHOT_UNREAD = "enable_my_shot_unread";
    static final String ENABLE_PRIVATE_MODE = "enable_private_mode";
    static final String BANNER_MANIFEST = "banner_manifest";
    static final String SCREENSHOT_CATEGORY_MANIFEST = "screenshot_category_manifest";
    static final String FEATURE_SURVEY = "feature_survey";
    static final String VPN_RECOMMENDER_URL = "vpn_recommender_url";
    static final String VPN_RECOMMENDER_PACKAGE = "vpn_recommender_package";
    static final String FIRST_LAUNCH_TIMER_MINUTES = "first_launch_timer_minutes";
    static final String FIRST_LAUNCH_NOTIFICATION_MESSAGE = "first_launch_notification_message";

    private static final String FIREBASE_WEB_ID = "default_web_client_id";
    private static final String FIREBASE_DB_URL = "firebase_database_url";
    private static final String FIREBASE_CRASH_REPORT = "google_crash_reporting_api_key";
    private static final String FIREBASE_APP_ID = "google_app_id";
    private static final String FIREBASE_API_KEY = "google_api_key";
    private static final String FIREBASE_PROJECT_ID = "project_id";

    static final String STR_SHARE_APP_DIALOG_TITLE = "str_share_app_dialog_title";
    static final String STR_SHARE_APP_DIALOG_CONTENT = "str_share_app_dialog_content";
    static final String STR_SHARE_APP_DIALOG_MSG = "str_share_app_dialog_msg";

    private HashMap<String, Object> remoteConfigDefault;

    @Nullable
    private static BlockingEnablerCallback enablerCallback;

    // the file name to used when you want to set the default value of RemoteConfig
    private static final String REMOTE_CONFIG_JSON = "remote_config.json";

    private FirebaseHelper() {
    }

    // inject delay to BlockingEnabler
    @VisibleForTesting
    public static void injectEnablerCallback(BlockingEnablerCallback callback) {
        enablerCallback = callback;
    }

    public static void init(final Context context, boolean enabled) {

        if (Companion.getInstance() == null) {
            Companion.initInternal(new FirebaseHelper());
        }
        checkIfApiReady(context);
        enableFirebase(context.getApplicationContext(), enabled);
    }

    private static void checkIfApiReady(Context context) {
        if (AppConstants.isBuiltWithFirebase()) {
            final String webId = getStringResourceByName(context, FIREBASE_WEB_ID);
            final String dbUrl = getStringResourceByName(context, FIREBASE_DB_URL);
            final String crashReport = getStringResourceByName(context, FIREBASE_CRASH_REPORT);
            final String appId = getStringResourceByName(context, FIREBASE_APP_ID);
            final String apiKey = getStringResourceByName(context, FIREBASE_API_KEY);
            final String projectId = getStringResourceByName(context, FIREBASE_PROJECT_ID);
            if (webId.isEmpty() || dbUrl.isEmpty() || crashReport.isEmpty() ||
                    appId.isEmpty() || apiKey.isEmpty() || projectId.isEmpty()) {
                throw new IllegalStateException("Firebase related keys are not set");
            }
        }
    }

    /**
     * @param applicationContext Should be application context. It's used for component enable/disable, and
     * @param enable  A boolean to determine if we should enable Firebase or not
     * @return Return true if a new runnable is created. otherwise return false.I need this return value for testing (as the return value of bind() method)
     */
    private static boolean enableFirebase(final Context applicationContext, final boolean enable) {


        Companion.setDeveloperModeEnabled(AppConstants.isFirebaseBuild());
        FirebaseApp.initializeApp(applicationContext);

        Companion.enableAnalytics(applicationContext, enable);
        Companion.enableCloudMessaging(applicationContext, RocketMessagingService.class.getName(), true);
        Companion.enableRemoteConfig(applicationContext, () -> {
            ThreadUtils.postToBackgroundThread(() -> {
                final String pref = applicationContext.getString(R.string.pref_s_news);
                final String source = Companion.getRcString(applicationContext, pref);
                final Settings settings = Settings.getInstance(applicationContext);
                final boolean canOverride = settings.canOverride(PREF_INT_NEWS_PRIORITY, Settings.PRIORITY_FIREBASE);
                Log.d(NewsSourceManager.TAG, "Remote Config fetched");
                if (!TextUtils.isEmpty(source) && (canOverride || TextUtils.isEmpty(settings.getNewsSource()))) {
                    Log.d(NewsSourceManager.TAG, "Remote Config is used:" + source);
                    settings.setPriority(PREF_INT_NEWS_PRIORITY, Settings.PRIORITY_FIREBASE);
                    settings.setNewsSource(source);
                    NewsSourceManager.getInstance().setNewsSource(source);
                }
            });
        });

        if (!enable) {
            new BlockingEnabler().execute();

        }
        return true;
    }


    // an interface for testing code to inject delay to BlockingEnabler
    public interface BlockingEnablerCallback {
        void runDelayOnExecution();
    }

    // AsyncTask is useful cause we don't need to write a specific idling resource for it.
    public static class BlockingEnabler extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            // this is only for testing. So we can simulate slow network..etc
            final BlockingEnablerCallback callback = FirebaseHelper.enablerCallback;
            if (callback != null) {
                callback.runDelayOnExecution();
            }

            // this methods is blocking.
            Companion.deleteInstanceId();

            return null;
        }

    }

    @NotNull
    @Override
    public HashMap<String, Object> getRemoteConfigDefault$firebase_firebase(@NotNull Context context) {

        if (remoteConfigDefault == null) {
            final boolean mayUseLocalFile = AppConstants.isDevBuild() || AppConstants.isBetaBuild();
            if (mayUseLocalFile && Looper.myLooper() != Looper.getMainLooper()) {
                // this only happens during init with
                remoteConfigDefault = fromFile(context);
            } else {
                remoteConfigDefault = fromResourceString(context);
            }
        }

        return remoteConfigDefault;
    }

    private HashMap<String, Object> fromFile(Context context) {

        // If we don't have read external storage permission, just don't bother reading the config file.
        if (FileUtils.canReadExternalStorage(context)) {
            try {
                return FileUtils.fromJsonOnDisk(REMOTE_CONFIG_JSON);
            } catch (Exception e) {
                Log.w(TAG, "Some problem when reading RemoteConfig file from local disk: ", e);
                // For any exception, we read the default resource file.
                return fromResourceString(context);
            }
        }

        return fromResourceString(context);
    }

    // This is the default value from resource string ( so we can leverage l10n)
    private HashMap<String, Object> fromResourceString(Context context) {
        final HashMap<String, Object> map = new HashMap<>();
        if (context != null) {
            map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_TITLE, context.getString(R.string.rate_app_dialog_text_title, context.getString(R.string.app_name)));
            map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_CONTENT, context.getString(R.string.rate_app_dialog_text_content));
            map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_POSITIVE, context.getString(R.string.rate_app_dialog_btn_go_rate));
            map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_NEGATIVE, context.getString(R.string.rate_app_dialog_btn_feedback));
            map.put(FirebaseHelper.FIRST_LAUNCH_NOTIFICATION_MESSAGE, context.getString(R.string.preference_default_browser) + "?\uD83D\uDE0A");
            // Share App
            map.put(FirebaseHelper.STR_SHARE_APP_DIALOG_TITLE, context.getString(R.string.share_app_dialog_text_title,
                    context.getString(R.string.app_name)));
            map.put(FirebaseHelper.STR_SHARE_APP_DIALOG_CONTENT, context.getString(R.string.share_app_dialog_text_content));
            final String shareAppDialogMsg = context.getString(R.string.share_app_promotion_text,
                    context.getString(R.string.app_name), context.getString(R.string.share_app_google_play_url),
                    context.getString(R.string.mozilla));
            map.put(FirebaseHelper.STR_SHARE_APP_DIALOG_MSG, shareAppDialogMsg);

        }
        map.put(FirebaseHelper.RATE_APP_DIALOG_THRESHOLD, DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_DIALOG);
        map.put(FirebaseHelper.RATE_APP_NOTIFICATION_THRESHOLD, DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION);
        map.put(FirebaseHelper.SHARE_APP_DIALOG_THRESHOLD, DialogUtils.APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG);
        map.put(FirebaseHelper.ENABLE_MY_SHOT_UNREAD, MainActivity.ENABLE_MY_SHOT_UNREAD_DEFAULT);
        map.put(FirebaseHelper.BANNER_MANIFEST, HomeFragment.BANNER_MANIFEST_DEFAULT);
        map.put(FirebaseHelper.ENABLE_PRIVATE_MODE, AppConfigWrapper.PRIVATE_MODE_ENABLED_DEFAULT);
        map.put(FirebaseHelper.FEATURE_SURVEY, RemoteConfigConstants.INSTANCE.getFEATURE_SURVEY_DEFAULT());
        map.put(FirebaseHelper.SCREENSHOT_CATEGORY_MANIFEST, ScreenshotManager.SCREENSHOT_CATEGORY_MANIFEST_DEFAULT);
        map.put(FirebaseHelper.VPN_RECOMMENDER_PACKAGE, FeatureSurveyViewHelper.Constants.PACKAGE_RECOMMEND_VPN);
        map.put(FirebaseHelper.VPN_RECOMMENDER_URL, FeatureSurveyViewHelper.Constants.LINK_RECOMMEND_VPN);
        map.put(FirebaseHelper.FIRST_LAUNCH_TIMER_MINUTES, FirstLaunchWorker.TIMER_DISABLED);

        return map;
    }

    @NonNull
    private static String getStringResourceByName(Context context, String aString) {
        String packageName = context.getPackageName();
        int resId = context.getResources()
                .getIdentifier(aString, "string", packageName);
        if (resId == 0) {
            return "";
        } else {
            return context.getString(resId);
        }
    }
}

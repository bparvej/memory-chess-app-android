package com.example.colormemorychess.ads;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Clean AdManager handling Adaptive Banner, Interstitial, and Rewarded Ads using Google Test Ad IDs.
 */
public class AdManager {

    private static final String TAG = "AdManager";

    // Google Test Ad Unit IDs
    public static final String BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917";

    private static AdManager instance;

    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;

    private boolean isInterstitialLoading = false;
    private boolean isRewardedLoading = false;

    private AdManager() {}

    public static synchronized AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }

    public void initialize(Context context) {
        MobileAds.initialize(context, initializationStatus -> {
            Log.d(TAG, "AdMob MobileAds initialized.");
            loadInterstitial(context);
            loadRewarded(context);
        });
    }

    // ==========================================
    // 1. ADAPTIVE BANNER AD
    // ==========================================

    public void loadAdaptiveBanner(Activity activity, FrameLayout container) {
        if (activity == null || container == null) return;

        AdView adView = new AdView(activity);
        adView.setAdUnitId(BANNER_TEST_ID);
        container.removeAllViews();
        container.addView(adView);

        AdSize adSize = getAdSize(activity, container);
        adView.setAdSize(adSize);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private AdSize getAdSize(Activity activity, FrameLayout container) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;
        float adWidthPixels = container.getWidth();

        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }

        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    // ==========================================
    // 2. INTERSTITIAL AD (Shown on Game Over)
    // ==========================================

    public void loadInterstitial(Context context) {
        if (interstitialAd != null || isInterstitialLoading) return;

        isInterstitialLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, INTERSTITIAL_TEST_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        isInterstitialLoading = false;
                        Log.d(TAG, "Interstitial Ad Loaded successfully.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialAd = null;
                        isInterstitialLoading = false;
                        Log.e(TAG, "Interstitial Ad Failed to Load: " + loadAdError.getMessage());
                    }
                });
    }

    public void showInterstitial(Activity activity, Runnable onAdDismissed) {
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    interstitialAd = null;
                    loadInterstitial(activity);
                    if (onAdDismissed != null) onAdDismissed.run();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    interstitialAd = null;
                    loadInterstitial(activity);
                    if (onAdDismissed != null) onAdDismissed.run();
                }
            });
            interstitialAd.show(activity);
        } else {
            loadInterstitial(activity);
            if (onAdDismissed != null) onAdDismissed.run();
        }
    }

    // ==========================================
    // 3. REWARDED AD (Peek Hint)
    // ==========================================

    public void loadRewarded(Context context) {
        if (rewardedAd != null || isRewardedLoading) return;

        isRewardedLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(context, REWARDED_TEST_ID, adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        isRewardedLoading = false;
                        Log.d(TAG, "Rewarded Ad Loaded successfully.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        isRewardedLoading = false;
                        Log.e(TAG, "Rewarded Ad Failed to Load: " + loadAdError.getMessage());
                    }
                });
    }

    public void showRewarded(Activity activity, Runnable onRewardEarned, Runnable onFailed) {
        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    rewardedAd = null;
                    loadRewarded(activity);
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    rewardedAd = null;
                    loadRewarded(activity);
                    if (onFailed != null) onFailed.run();
                }
            });

            rewardedAd.show(activity, rewardItem -> {
                Log.d(TAG, "User earned reward: " + rewardItem.getType());
                if (onRewardEarned != null) onRewardEarned.run();
            });
        } else {
            loadRewarded(activity);
            if (onFailed != null) onFailed.run();
        }
    }

    public boolean isRewardedLoaded() {
        return rewardedAd != null;
    }
}

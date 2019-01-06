package com.getkeepsafe.mopubanr

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*

class ActivityA : AppCompatActivity(), BannerAdView {
    private val compositeDisposable = CompositeDisposable()
    private val bannerAdPresenter: BannerAdPresenter by lazy { BannerAdPresenter(this) }
    private var bannerAdView: MoPubBannerAdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        open_activity.setOnClickListener { startActivity(Intent(this, ActivityB::class.java))}
    }

    override fun onResume() {
        super.onResume()
        compositeDisposable.add(App.adService.mopubInitializedSubject
            //.delay(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = {
                if (it) {
                    bannerAdPresenter.resume()
                    Toast.makeText(this@ActivityA, "sdk initialized; loading ad", LENGTH_LONG).show()
                }
            }))
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun loadBannerAd(listener: MoPubBannerAdView.Listener?) {
        if (bannerAdView != null) {
            return
        }

        bannerAdView = createBannerAd(coordinator_layout, dependent_view, listener)
        if (bannerAdView == null) {
            return
        }

        if (!isFinishing) {
            // There is a race condition where adding / removing views from the CoordinatorLayout
            // while its in its layout pass will throw an IndexOutOfBoundsException. So we ensure
            // it has finished it's layout pass before we add the ad view
            coordinator_layout.onLaidOut {
                coordinator_layout.addView(bannerAdView)
                bannerAdView!!.loadAd()
            }
        }
    }

    override fun removeBannerAd() {
        if (bannerAdView == null) {
            return
        }

        // There is a race condition where adding / removing views from the CoordinatorLayout
        // while its in its layout pass will throw an IndexOutOfBoundsException. So we ensure
        // it has finished it's layout pass before we remove the ad view
        coordinator_layout.onLaidOut {
            try {
                coordinator_layout.removeView(bannerAdView)
            } catch (ignored: Throwable) {
                // If the view was never attached, we don't care
            }
        }
    }
}

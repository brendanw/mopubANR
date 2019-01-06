package com.getkeepsafe.mopubanr

import android.app.Application
import com.mopub.common.MoPub
import com.mopub.common.SdkConfiguration
import com.mopub.common.SdkInitializationListener
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*
import java.util.concurrent.TimeUnit

class App : Application() {
    companion object {
        val adService: AdService by lazy { AdService() }
    }

    override fun onCreate() {
        super.onCreate()
        mimicCpuActivity()
        Observable.timer(5, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onComplete = {
                initializeMopub()
            })
    }

    private fun mimicCpuActivity() {
        val random = Random()
        for (i in 0..100) {
            Completable.fromCallable {
                Thread.sleep(random.nextInt(10000).toLong())
            }.subscribeOn(Schedulers.io())
                .subscribe()
        }
    }

    private fun initializeMopub() {
        // This can be any valid ad unit ID
        val sdkConfiguration = SdkConfiguration.Builder(AdService.AD_UNIT_1).build()
        MoPub.initializeSdk(this, sdkConfiguration, adService)
    }
}

class AdService : SdkInitializationListener {
    companion object {
        const val AD_UNIT_1 = "6aa8cdfd780b4ab68bd4444a1c4bfdfe"
    }

    @Volatile
    var isMopubLoaded = false
    /**
     * Be careful modifying this code as the mopub library is susceptible to deadlock due to multiple
     * public methods the library provides (initialiazeSdk, loadAd, etc) calling synchronized blocks
     * in its internal networking class. You need to ensure calls are temporally spaced out to avoid
     * deadlock. If you run into deadlock, use the debugger and step through mopub's stacktrace to
     * find the source.
     */
    var mopubInitializedSubject: BehaviorSubject<Boolean> = BehaviorSubject.create<Boolean>()

    override fun onInitializationFinished() {
        isMopubLoaded = true
        mopubInitializedSubject.onNext(true)
    }
}
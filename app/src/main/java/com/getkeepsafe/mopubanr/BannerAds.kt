package com.getkeepsafe.mopubanr

import android.content.Context
import android.graphics.Color
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import com.mopub.mobileads.MoPubErrorCode
import com.mopub.mobileads.MoPubView

/**
 * Creates and properly configures a banner ad view that will be contained within the given
 * parent layout.
 *
 * @param parent The frame layout that will house the banner ad
 * @param dependentView If given, the bottom padding value of this view will be adjusted to
 *                      accommodate the banner ad
 * @param adUnit The ad ID you want to use
 * @param listener If given, the mediaPageListener will be attached to the ad lifecycle
 * @return The created banner ad
 */
@JvmOverloads
fun createBannerAd(
    parent: CoordinatorLayout,
    dependentView: View? = null,
    listener: MoPubBannerAdView.Listener? = null,
    adUnit: String = AdService.AD_UNIT_1
): MoPubBannerAdView? {
    val context = parent.context
    val dependentBottomPadding = dependentView?.paddingBottom ?: 0
    val layoutParams = CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(50.0f).toInt())
    layoutParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM

    val bannerAdView: MoPubBannerAdView
    try {
        bannerAdView = MoPubBannerAdView(context)
    } catch (ignored: Exception) {
        // MoPub doesn't account for outdated Google Play services and or the absence of
        // the webview component. The absence could mean that its not available permanently, or it
        // is simply being updated and is unavailable during the time this code is called.
        // Either way, we ignore this exception and show no ad rather than crashing the entire app.
        return null
    }

    with(bannerAdView) {
        adUnitId = adUnit
        setLayoutParams(layoutParams)
        visibility = View.GONE
        setPadding(0, paddingTop, paddingBottom, 0)
        testing = true
        setListener(object : MoPubBannerAdView.Listener {
            override fun onBannerClosed() {
                dependentView?.setPadding(
                    dependentView.paddingLeft, dependentView.paddingTop,
                    dependentView.paddingRight, dependentBottomPadding
                )
                listener?.onBannerClosed()
            }

            override fun onBannerExpanded(banner: MoPubView) {
                listener?.onBannerExpanded(banner)
            }

            override fun onBannerFailed(banner: MoPubView, errorCode: MoPubErrorCode) {
                visibility = View.GONE
                dependentView?.setPadding(
                    dependentView.paddingLeft, dependentView.paddingTop,
                    dependentView.paddingRight, dependentBottomPadding
                )
                listener?.onBannerFailed(banner, errorCode)
            }

            override fun onBannerClicked(banner: MoPubView) {
                listener?.onBannerClicked(banner)
            }

            override fun onBannerLoaded(banner: MoPubView) {
                visibility = View.VISIBLE
                dependentView?.setPadding(
                    dependentView.paddingLeft, dependentView.paddingTop,
                    dependentView.paddingRight, Math.max(dependentBottomPadding, measuredHeight)
                )
                listener?.onBannerLoaded(banner)
            }

            override fun onBannerCollapsed(banner: MoPubView) {
                listener?.onBannerCollapsed(banner)
            }
        })
    }

    return bannerAdView
}

interface BannerAdView {
    fun loadBannerAd(listener: MoPubBannerAdView.Listener? = null)
    fun removeBannerAd()
}

class BannerAdPresenter(private val view: BannerAdView) {
    init {
        view.loadBannerAd(object : MoPubBannerAdView.Listener {
            override fun onBannerClosed() {
                view.removeBannerAd()
            }
        })
    }

    fun resume() {

    }
}

class MoPubBannerAdView(context: Context) : MoPubView(context) {
    private val closeView: ImageView = ImageView(context)
    private var listener: Listener? = null

    init {
        with(closeView) {
            val dim = context.dp(18.0f).toInt()
            val params = FrameLayout.LayoutParams(dim, dim)
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutParams = params
            visibility = GONE
            setImageResource(R.drawable.ic_close_black_24_dp)
            setBackgroundColor(Color.parseColor("#000000"))
            setColorFilter(Color.WHITE)
            setOnClickListener {
                destroy()
                listener?.onBannerClosed()
            }
        }
    }

    fun setListener(newListener: Listener) {
        listener = newListener
        bannerAdListener = listener
    }

    override fun adLoaded() {
        super.adLoaded()
        closeView.visibility = VISIBLE
    }

    override fun adFailed(errorCode: MoPubErrorCode?) {
        super.adFailed(errorCode)
        closeView.visibility = GONE
    }

    override fun setAdContentView(view: View) {
        super.setAdContentView(view)
        // Super removes all views from this layout, so we need to re-add the close view
        view.onLaidOut {
            with(closeView) {
                val params = layoutParams as FrameLayout.LayoutParams
                params.leftMargin = view.width / 2 - params.width / 2
                layoutParams = params
                val parentGroup = parent
                if (parentGroup is ViewGroup) {
                    parentGroup.removeView(this)
                }
                try {
                    addView(this)
                } catch (ignored: IllegalStateException) {
                }
            }
        }
    }

    interface Listener : BannerAdListener {
        override fun onBannerLoaded(banner: MoPubView) {}
        override fun onBannerFailed(banner: MoPubView, errorCode: MoPubErrorCode) {}
        override fun onBannerClicked(banner: MoPubView) {}
        override fun onBannerExpanded(banner: MoPubView) {}
        override fun onBannerCollapsed(banner: MoPubView) {}
        fun onBannerClosed() {}
    }
}

inline fun View.onLaidOut(crossinline block: () -> Unit) {
    if (ViewCompat.isLaidOut(this)) {
        block()
        return
    }

    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            block()
        }
    })
}

fun Context.dp(dim: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dim, resources.displayMetrics)

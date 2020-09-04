package io.flutter.plugins.webviewflutter.hybrid

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory


/**
 * Create by TaiJL
 * on 2020/8/23
 * desc: Android  HybridWebView
 */
class HybridWebViewFactory(private val messenger: BinaryMessenger, private val mActivity: Activity) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    private var hybridWebView: HybridWebView? = null

//    init {
//        initQbSdk()
//    }
//
//    private fun initQbSdk() {
//        //搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核。
//        val cb: PreInitCallback = object : PreInitCallback {
//            override fun onViewInitFinished(arg0: Boolean) {
//                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
//                Log.d("app", " onViewInitFinished is $arg0")
//            }
//
//            override fun onCoreInitFinished() {
//                // TODO Auto-generated method stub
//            }
//        }
//        //x5内核初始化接口
//        QbSdk.initX5Environment(mActivity, cb)
//    }

    override fun create(context: Context?, viewId: Int, args: Any): PlatformView? {
        val params = args as Map<String, Any>
        hybridWebView = HybridWebView(viewId, params, mActivity, messenger)
        return hybridWebView
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        hybridWebView?.onActivityResult(requestCode, resultCode, data)
    }
}
package dotest.module.do_TencentWebView.debug;

import android.app.Application;
import android.util.Log;

import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsListener;

public class DoApplication extends Application {

	private static DoApplication instance;
	private String TAG = "TencentX5";

	@Override
	public void onCreate() {
		super.onCreate();

//		Log.i(TAG, "onCreate()");
//		instance = this;
//		initTbs();
	}

	private void initTbs() {
		//搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核。
		QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {

			@Override
			public void onViewInitFinished(boolean arg0) {
				Log.i(TAG, "View是否初始化完成:" + arg0);
			}

			@Override
			public void onCoreInitFinished() {
				Log.i(TAG, "X5内核初始化完成");
			}
		};

		QbSdk.setTbsListener(new TbsListener() {
			@Override
			public void onDownloadFinish(int i) {
				Log.i(TAG, "腾讯X5内核 下载结束");
			}

			@Override
			public void onInstallFinish(int i) {
				Log.i(TAG, "腾讯X5内核 安装完成");
			}

			@Override
			public void onDownloadProgress(int i) {
				Log.i(TAG, "腾讯X5内核 下载进度:%" + i);
			}
		});

		QbSdk.initX5Environment(getApplicationContext(), cb);
	}

	public static DoApplication getInstance() {
		return instance;
	}
	
}

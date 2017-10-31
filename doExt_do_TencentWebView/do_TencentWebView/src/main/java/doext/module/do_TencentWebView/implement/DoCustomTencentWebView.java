package doext.module.do_TencentWebView.implement;

import android.content.Context;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.ProgressBar;

import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;

import java.util.Map;

/**
 * Created by feng_ on 2017/2/7.
 */

public class DoCustomTencentWebView extends WebView {
	private ProgressBar mProgressBar;

	private OnDataCallback dataCallback;

	public interface OnDataCallback {
		public void onData(String value);
	}

	public void setOnDataCallback(OnDataCallback dataCallback) {
		this.dataCallback = dataCallback;
	}

	private OnJsCallJavaDCallback jsCallJavaCallback;

	public interface OnJsCallJavaDCallback {
		public void onData(String value);
	}

	public void setOnJsCallJavaCallback(OnJsCallJavaDCallback dataCallback) {
		this.jsCallJavaCallback = dataCallback;
	}

	private boolean addedJavascriptInterface;

	public DoCustomTencentWebView(Context context) {
		super(context);
		init(context);
	}

	public DoCustomTencentWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public DoCustomTencentWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		mProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
		mProgressBar.setLayoutParams(new android.widget.AbsoluteLayout.LayoutParams(LayoutParams.MATCH_PARENT, 8, 0, 0));
		mProgressBar.setVisibility(View.GONE);
		addView(mProgressBar);
		this.setOnTouchListener(new OnTouchListener() {
			private float startx;
			private float starty;
			private float offsetx;
			private float offsety;

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				switch (arg1.getAction()) {
				case MotionEvent.ACTION_DOWN:
					arg0.getParent().requestDisallowInterceptTouchEvent(true);
					startx = arg1.getX();
					starty = arg1.getY();
					break;
				case MotionEvent.ACTION_MOVE:
					offsetx = Math.abs(arg1.getX() - startx);
					offsety = Math.abs(arg1.getY() - starty);
					if (offsetx > offsety) {
						arg0.getParent().requestDisallowInterceptTouchEvent(true);
					} else {
						arg0.getParent().requestDisallowInterceptTouchEvent(false);
					}
					break;
				default:
					break;
				}
				return false;
			}
		});
	}

	public void setLoadingProgressBarColor(int backgroundColor, int progressColor) {
		// mProgressBar.setLayoutParams(new
		// android.widget.AbsoluteLayout.LayoutParams(LayoutParams.MATCH_PARENT,
		// 8, 0, 0));
		// Background
		ClipDrawable bgClipDrawable = new ClipDrawable(new ColorDrawable(backgroundColor), Gravity.LEFT, ClipDrawable.HORIZONTAL);
		bgClipDrawable.setLevel(10000);
		// Progress
		ClipDrawable progressClip = new ClipDrawable(new ColorDrawable(progressColor), Gravity.LEFT, ClipDrawable.HORIZONTAL);
		// Setup LayerDrawable and assign to progressBar
		Drawable[] progressDrawables = { bgClipDrawable, progressClip/* second */, progressClip };
		LayerDrawable progressLayerDrawable = new LayerDrawable(progressDrawables);
		progressLayerDrawable.setId(0, android.R.id.background);
		progressLayerDrawable.setId(1, android.R.id.secondaryProgress);
		progressLayerDrawable.setId(2, android.R.id.progress);
		mProgressBar.setProgressDrawable(progressLayerDrawable);
	}

	public ProgressBar getProgressbar() {
		return this.mProgressBar;
	}

	/**
	 * Pass only a DoWebChromeClient instance.
	 */
	@Override
	public void setWebChromeClient(WebChromeClient client) {
		super.setWebChromeClient(client);
	}

	@Override
	public void loadData(String data, String mimeType, String encoding) {
		addJavascriptInterface();
		super.loadData(data, mimeType, encoding);
	}

	@Override
	public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
		addJavascriptInterface();
		super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
	}

	@Override
	public void loadUrl(String url) {
		addJavascriptInterface();
		super.loadUrl(url);
	}

	@Override
	public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
		addJavascriptInterface();
		super.loadUrl(url, additionalHttpHeaders);
	}

	private void addJavascriptInterface() {
		if (!addedJavascriptInterface) {
			// Add javascript interface to be called when the video ends (mustbe
			// done before page load)
			addJavascriptInterface(this, "_DoCustomTencentWebView"); // Must
			addJavascriptInterface(this, "do_TencentWebView"); // Must
			addedJavascriptInterface = true;
		}
	}

	@JavascriptInterface
	private void notifyVideoEnd() {

	}

	@JavascriptInterface
	public void onData(String value) {
		if (dataCallback != null) {
			dataCallback.onData(value);
		}
	}

	@JavascriptInterface
	public void fire(String value) {
		if (jsCallJavaCallback != null) {
			jsCallJavaCallback.onData(value);
		}
	}

	public int getContentWidth() {
		return this.computeHorizontalScrollRange();
	}

}

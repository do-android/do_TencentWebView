package doext.module.do_TencentWebView.implement;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Browser;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsListener;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoJsonHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoUIModule;
import doext.module.do_TencentWebView.define.do_TencentWebView_IMethod;


/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,do_TencentWebView_IMethod接口
 * ； #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_TencentWebView_View extends RelativeLayout implements DoIUIModuleView, do_TencentWebView_IMethod {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_TencentWebView_Model model;
	protected DoCustomTencentWebView webView;
	private DoIScriptEngine scriptEngine;
	private Context ctx;
	private final String _TAG = "doTencentX5";
	public final static String NO_CACHE = "no_cache";
	public final static String NORMAL = "normal";

	public do_TencentWebView_View(Context context) {

		super(context);
		ctx = context;
		initTbs();
		webView = new DoCustomTencentWebView(context);
		WebSettings webSetting = webView.getSettings();

		webSetting.setJavaScriptEnabled(true);
		webSetting.setJavaScriptCanOpenWindowsAutomatically(true);
		webSetting.setAppCacheEnabled(true);
		webSetting.setDomStorageEnabled(true);
		webSetting.setRenderPriority(WebSettings.RenderPriority.HIGH);
		webSetting.setCacheMode(WebSettings.LOAD_NO_CACHE);
		webSetting.setDisplayZoomControls(false);
		webSetting.setDatabaseEnabled(true);
		webSetting.setLightTouchEnabled(true);

		// 重新开启一个session会话
		CookieManager.getInstance().removeSessionCookie();

		String dir = this.ctx.getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
		webSetting.setDatabasePath(dir);
		webSetting.setGeolocationEnabled(true); // 启用地理定位
		webSetting.setGeolocationDatabasePath(dir); // 设置定位的数据库路径

		webView.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
				Uri _uri = Uri.parse(url);
				Intent _intent = new Intent(Intent.ACTION_VIEW, _uri);
				ctx.startActivity(_intent);
			}

		});

		webView.setWebViewClient(new WebViewClient() {
			public void onLoadResource(WebView view, String url) {
				DoServiceContainer.getLogEngine().writeDebug("Load resource=" + url);
			}

			public boolean shouldOverrideUrlLoading(WebView view, String url) {

				if (url.startsWith("tel:")) { // 拨打电话
					Uri uri = Uri.parse(url);
					Intent dial = new Intent("android.intent.action.DIAL", uri);
					((Activity) model.getCurrentPage().getPageView()).startActivity(dial);
					return true;
				}

				if (url.startsWith("mailto:")) { // 发送邮件
					Uri uri = Uri.parse(url);
					Intent email = new Intent("android.intent.action.SENDTO", uri);
					((Activity) model.getCurrentPage().getPageView()).startActivity(email);
					return true;
				}

				// 重写此方法表明点击网页里面的链接还是在当前的webview里跳转，不跳到浏览器那边
				try {
					try {
						// 加载视频过程中需要跳转到tenvideo2开头的临时链接,URL加载时候会报错,判断兼容一下
						if (!url.startsWith("tenvideo2://")) {
							new URL(url); // 检查url是否合法
							view.loadUrl(url);
						}
					} catch (MalformedURLException e) {
						// 打开类似于myapp://等开头的路径
						Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
						intent.addCategory(Intent.CATEGORY_BROWSABLE);
						intent.setComponent(null);
						intent.putExtra(Browser.EXTRA_APPLICATION_ID, view.getContext().getPackageName());
						view.getContext().startActivity(intent);
					}
				} catch (Exception e) {
					DoServiceContainer.getLogEngine().writeError("DoTencentWebView : shouldOverrideUrlLoading\n", e);
				}
				return true;
			}

			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				DoInvokeResult jsonResult = new DoInvokeResult(model.getUniqueKey());
				jsonResult.setResultText("加载网页失败：url:" + failingUrl + " ,errorCode:" + errorCode + " ,description：" + description);
				model.getEventCenter().fireEvent("failed", jsonResult);
				DoServiceContainer.getLogEngine().writeError("执行Web脚本错误", new Exception(failingUrl + " 发生" + errorCode + "错误:" + description));
			}

			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				webBrowser_View_DocumentCompleted(url);
				do_TencentWebView_View.this.requestFocus(View.FOCUS_UP | View.FOCUS_DOWN);
			}

			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				webBrowser_View_DocumentStart(url);
			}

			@Override
			public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(webView.getContext());
				builder.setMessage("证书不安全！");
				builder.setPositiveButton("继续访问", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						handler.proceed(); // Ignore SSL certificate errors
					}
				});
				builder.setNegativeButton("关闭", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						handler.cancel();
					}
				});
				final AlertDialog dialog = builder.create();
				dialog.show();
			}

		});

		webView.setOnJsCallJavaCallback(new DoCustomTencentWebView.OnJsCallJavaDCallback() {
			@Override
			public void onData(String value) {
				DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
				_invokeResult.setResultText(value);
				model.getEventCenter().fireEvent("eventFromHtml", _invokeResult);
			}
		});
	}

	private void initTbs() {
		// 搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核。
		QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {

			@Override
			public void onViewInitFinished(boolean arg0) {
				Log.i(_TAG, "View是否初始化完成:" + arg0);
			}

			@Override
			public void onCoreInitFinished() {
				Log.i(_TAG, "X5内核初始化完成");
			}
		};

		QbSdk.setTbsListener(new TbsListener() {
			@Override
			public void onDownloadFinish(int i) {
				Log.i(_TAG, "腾讯X5内核 下载结束");
			}

			@Override
			public void onInstallFinish(int i) {
				Log.i(_TAG, "腾讯X5内核 安装完成");
			}

			@Override
			public void onDownloadProgress(int i) {
				Log.i(_TAG, "腾讯X5内核 下载进度:%" + i);
			}
		});

		QbSdk.initX5Environment(ctx, cb);
	}

	/**
	 * 初始化加载view准备,_doUIModsetLoadingProgressColorule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_TencentWebView_Model) _doUIModule;
		webView.setWebChromeClient(new DoTencentWebChromeClient(webView, model, DoTextHelper.strToBool(_doUIModule.getPropertyValue("isShowLoadingProgress"), false)));
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		webView.setBackgroundColor(Color.TRANSPARENT);
		this.addView(webView, layoutParams);
	}

	private void webBrowser_View_DocumentCompleted(String url) {
		try {
			DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
			JSONObject _value = new JSONObject();
			_value.put("url", url);
			_invokeResult.setResultNode(_value);
			this.model.getEventCenter().fireEvent("loaded", _invokeResult);
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("调用loaded错误", e);
		}
	}

	private void webBrowser_View_DocumentStart(String url) {
		try {
			DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
			JSONObject _value = new JSONObject();
			_value.put("url", url);
			_invokeResult.setResultNode(_value);
			this.model.getEventCenter().fireEvent("start", _invokeResult);
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("调用start错误", e);
		}
	}

	private void navigate(String _fullUrl) {
		if (TextUtils.isEmpty(_fullUrl)) {
			return;
		}
		if (_fullUrl.startsWith("http:") || _fullUrl.startsWith("https:") || _fullUrl.startsWith("file:")) {
			webView.loadUrl(_fullUrl);
		} else {
			try {
				_fullUrl = DoIOHelper.getLocalFileFullPath(this.model.getCurrentPage().getCurrentApp(), _fullUrl);
				if (DoIOHelper.isAssets(_fullUrl)) {
					_fullUrl = "/android_asset/" + DoIOHelper.getAssetsRelPath(_fullUrl);
				}
				webView.loadUrl("file://" + _fullUrl);
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("DoTencentWebView loadUrl \n\t", e);
			}
		}
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 *
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 *
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("userAgent")) {
			webView.getSettings().setUserAgentString(_changedValues.get("userAgent"));
		}

		if (_changedValues.containsKey("bgColor")) {
			int _bgColor = DoUIModuleHelper.getColorFromString(_changedValues.get("bgColor"), Color.TRANSPARENT);
			webView.setBackgroundColor(_bgColor);
		}

		if (_changedValues.containsKey("cacheType")) {
			String _cacheType = _changedValues.get("cacheType");
			if (null != _cacheType && NORMAL.equals(_cacheType)) {
				webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
			} else {
				webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
			}
		}

		if (_changedValues.containsKey("url")) {
			this.navigate(_changedValues.get("url"));
		}

		if (_changedValues.containsKey("zoom")) {
			String zoom = _changedValues.get("zoom");
			boolean bool = DoTextHelper.strToBool(zoom, false);
			webView.getSettings().setSupportZoom(bool);
			webView.getSettings().setUseWideViewPort(bool);
			webView.getSettings().setBuiltInZoomControls(bool);
		}
		if (_changedValues.containsKey("enabled")) {
			if ("true".equals(_changedValues.get("enabled")))
				this.setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
			else
				this.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
		}
	}

	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("back".equals(_methodName)) {
			this.back(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("forward".equals(_methodName)) {
			this.forward(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("reload".equals(_methodName)) {
			this.reload(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("stop".equals(_methodName)) {
			this.stop(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("canForward".equals(_methodName)) {
			this.canForward(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("canBack".equals(_methodName)) {
			this.canBack(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("setCookie".equals(_methodName)) {
			setCookie(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("setLoadingProgressColor".equals(_methodName)) {
			setLoadingProgressColor(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("getContentSize".equals(_methodName)) {
			getContentSize(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		if ("loadString".equals(_methodName)) {
			loadString(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		if ("eval".equals(_methodName)) {
			eval(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		if (this.scriptEngine != null) {
			this.scriptEngine.dispose();
			this.scriptEngine = null;
		}
		if (webView != null) {
			ViewGroup _viewGroup = (ViewGroup) getRootView();
			_viewGroup.removeView(webView);
			webView.clearCache(true);
			webView.removeAllViews();
			webView.onPause();
			webView.destroy();
			webView = null;
		}
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	/**
	 * 回退；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void back(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		webView.goBack();
		model.setPropertyValue("url", webView.getUrl());
	}

	/**
	 * 是否可后退；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void canBack(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		boolean canGoBack = webView.canGoBack();
		_invokeResult.setResultBoolean(canGoBack);
	}

	/**
	 * 是否可继续前进；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void canForward(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		boolean canGoForward = webView.canGoForward();
		_invokeResult.setResultBoolean(canGoForward);
	}

	/**
	 * 执行JavaScript函数；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void eval(JSONObject _dictParas, final DoIScriptEngine _scriptEngine, final String _callbackFuncName) {
		webView.setOnDataCallback(new DoCustomTencentWebView.OnDataCallback() {
			@Override
			public void onData(String value) {
				DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
				_invokeResult.setResultText(value);
				_scriptEngine.callback(_callbackFuncName, _invokeResult);
			}
		});
		// 加载html字符串
		try {
			final String jsStr = DoJsonHelper.getString(_dictParas, "code", "");
			Activity activity = (Activity) this.model.getCurrentPage().getPageView();
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					webView.loadUrl("javascript:window._DoCustomTencentWebView.onData((" + jsStr + "))");
				}
			});
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("执行eval错误", e);
		}
	}

	/**
	 * 前进；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void forward(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		webView.goForward();
		model.setPropertyValue("url", webView.getUrl());
	}

	/**
	 * 获取加载页面内容尺寸；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void getContentSize(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		double _w = webView.getContentWidth() / model.getXZoom();
		double _h = (webView.getContentHeight() * webView.getScale()) / model.getYZoom();
		JSONObject _node = new JSONObject();
		_node.put("height", _h);
		_node.put("width", _w);
		_invokeResult.setResultNode(_node);
	}

	/**
	 * 加载html字符串；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void loadString(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		// 加载html字符串
		try {
			final String htmlStr = DoJsonHelper.getString(_dictParas, "text", "");
			Activity activity = (Activity) this.model.getCurrentPage().getPageView();
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					webView.loadData(htmlStr, "text/html; charset=UTF-8", null);// 这种写法可以正确解码
				}
			});
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("执行loadString错误", e);
		}
	}

	/**
	 * 重新加载；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void reload(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		webView.reload();
	}

	/**
	 * 设置cookie；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void setCookie(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _url = DoJsonHelper.getString(_dictParas, "url", "");

		if (TextUtils.isEmpty(_url)) {
			throw new Exception("url不能为空!");
		}
		String _value = DoJsonHelper.getString(_dictParas, "value", "");

		if (TextUtils.isEmpty(_value)) {
			throw new Exception("value不能为空!");
		}

		CookieManager.getInstance().setCookie(_url, _value);
	}

	/**
	 * 设置默认转码；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void setDefaultEncodingURL(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {

	}

	/**
	 * 设置顶部进度条颜色；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void setLoadingProgressColor(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _color = DoJsonHelper.getString(_dictParas, "color", "");
		webView.setLoadingProgressBarColor(Color.TRANSPARENT, DoUIModuleHelper.getColorFromString(_color, Color.parseColor("#55C0E9")));
	}

	/**
	 * 停止刷新；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void stop(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		webView.stopLoading();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				getParent().requestDisallowInterceptTouchEvent(true);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				break;
		}
		return false;
	}
}
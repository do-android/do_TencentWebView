package dotest.module.do_TencentWebView.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import core.DoServiceContainer;
import core.object.DoUIModule;
import doext.module.do_TencentWebView.implement.do_TencentWebView_Model;
import doext.module.do_TencentWebView.implement.do_TencentWebView_View;
import dotest.module.do_TencentWebView.R;
import dotest.module.do_TencentWebView.debug.DoService;

/**
 * webview组件测试样例
 */
public class WebViewSampleTestActivty extends DoTestActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initModuleModel() throws Exception {
        this.model = new do_TencentWebView_Model();
    }

    @Override
    protected void initUIView() throws Exception {
        do_TencentWebView_View view = new do_TencentWebView_View(this);
        ((DoUIModule) this.model).setCurrentUIModuleView(view);
        ((DoUIModule) this.model).setCurrentPage(currentPage);
        view.loadView((DoUIModule) this.model);
        LinearLayout uiview = (LinearLayout) findViewById(R.id.uiview);
        uiview.addView(view);
    }

    //http://oj8so80jf.bkt.clouddn.com/1.mp4
    @Override
    public void doTestProperties(View view) {
        //DoService.setPropertyValue(this.model, "url", "http://oj8so80jf.bkt.clouddn.com/1.mp4");
        DoService.setPropertyValue(this.model, "url", "https://v.qq.com/");
    }

    @Override
    protected void doTestSyncMethod() {
//        Map<String, String> _paras_back = new HashMap<String, String>();
//        DoService.syncMethod(this.model, "back", _paras_back);
    }

    @Override
    protected void doTestAsyncMethod() {
//        Map<String, String> _paras_loadString = new HashMap<String, String>();
//        _paras_loadString.put("text", "<b>百度</b>");
//        DoService.asyncMethod(this.model, "loadString", _paras_loadString, new DoService.EventCallBack() {
//            @Override
//            public void eventCallBack(String _data) {// 回调函数
//                DoServiceContainer.getLogEngine().writeDebug("异步方法回调：" + _data);
//            }
//        });
//        Map<String, String> _paras_loadString = new HashMap<String, String>();
//        _paras_loadString.put("code", "getbtn('evaltest')");
//        DoService.asyncMethod(this.model, "eval", _paras_loadString, new DoService.EventCallBack() {
//            @Override
//            public void eventCallBack(String _data) {// 回调函数
//                DoServiceContainer.getLogEngine().writeDebug("异步方法回调：" + _data);
//            }
//        });
        DoService.setPropertyValue(this.model, "url", "https://www.baidu.com");
    }

    @Override
    protected void onEvent() {
        // 系统事件订阅
        DoService.subscribeEvent(this.model, "loaded", new DoService.EventCallBack() {
            @Override
            public void eventCallBack(String _data) {
                DoServiceContainer.getLogEngine().writeDebug("系统事件回调：name = loaded, data = " + _data);
                Toast.makeText(WebViewSampleTestActivty.this, "系统事件回调：loaded", Toast.LENGTH_LONG).show();
            }
        });
        // 自定义事件订阅
        DoService.subscribeEvent(this.model, "_messageName", new DoService.EventCallBack() {
            @Override
            public void eventCallBack(String _data) {
                DoServiceContainer.getLogEngine().writeDebug("自定义事件回调：name = _messageName, data = " + _data);
                Toast.makeText(WebViewSampleTestActivty.this, "自定义事件回调：_messageName", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void doTestFireEvent(View view) {
        // fire 自定义事件
//		DoInvokeResult invokeResult = new DoInvokeResult(this.model.getUniqueKey());
//		this.model.getEventCenter().fireEvent("_messageName", invokeResult);
//		Map<String, String> _paras_back = new HashMap<String, String>();
//		DoService.syncMethod(this.model, "back", _paras_back);

//        InputStream stream = getApplicationContext().getClass().getClassLoader().getResourceAsStream("assets/" + "abc.html");
//
//        try {
//            StringBuffer _sb = new StringBuffer();
//            BufferedInputStream _bis = new BufferedInputStream(stream);
//            byte[] _bytes = new byte[1024 * 4];
//            int _len = 0;
//            while ((_len = _bis.read(_bytes)) != -1) {
//                _sb.append(new String(_bytes, 0, _len));
//            }
//            _bis.close();
//
//
//            Map<String, String> _paras_loadString = new HashMap<String, String>();
//            _paras_loadString.put("text", _sb.toString());
//            DoService.asyncMethod(this.model, "loadString", _paras_loadString, new DoService.EventCallBack() {
//                @Override
//                public void eventCallBack(String _data) {// 回调函数
//                    DoServiceContainer.getLogEngine().writeDebug("异步方法回调：" + _data);
//                }
//            });
//
//        } catch (Exception ex) {
//
//        }
        Map<String, String> _paras_back = new HashMap<String, String>();
        _paras_back.put("color", "FF0000FF");
        DoService.syncMethod(this.model, "setLoadingProgressColor", _paras_back);

    }

}

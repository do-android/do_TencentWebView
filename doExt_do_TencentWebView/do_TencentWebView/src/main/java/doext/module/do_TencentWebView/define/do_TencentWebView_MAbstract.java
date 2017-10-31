package doext.module.do_TencentWebView.define;

import core.object.DoUIModule;
import core.object.DoProperty;
import core.object.DoProperty.PropertyDataType;


public abstract class do_TencentWebView_MAbstract extends DoUIModule{

	protected do_TencentWebView_MAbstract() throws Exception {
		super();
	}
	
	/**
	 * 初始化
	 */
	@Override
	public void onInit() throws Exception{
        super.onInit();
        //注册属性
		this.registProperty(new DoProperty("bounces", PropertyDataType.Bool, "true", false));
		this.registProperty(new DoProperty("cacheType", PropertyDataType.String, "no_cache", false));
		this.registProperty(new DoProperty("isShowLoadingProgress", PropertyDataType.Bool, "false", true));
		this.registProperty(new DoProperty("url", PropertyDataType.String, "", false));
		this.registProperty(new DoProperty("userAgent", PropertyDataType.String, "", false));
		this.registProperty(new DoProperty("zoom", PropertyDataType.Bool, "false", true));
		this.registProperty(new DoProperty("enabled", PropertyDataType.Bool, "true", false));
	}
}
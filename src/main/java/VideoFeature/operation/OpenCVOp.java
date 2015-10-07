package VideoFeature.operation;

import java.io.IOException;
import java.util.Map;

import vStormConfig.VStormConfig;

import VideoFeature.model.BaseModel;
import VideoFeature.utils.NativeUtils;
import backtype.storm.task.TopologyContext;

/**
 * 涉及调用opencv方法的Operation抽象类，通过libName加载opencv动态链接库。所有调用opencv的操作都需继承它。
 * 
 * @author liangzhaohao
 *
 */
public abstract class OpenCVOp<Output extends BaseModel> implements IOperation<Output>{

	private static final long serialVersionUID = -7758652109335765844L;

	private String libName;
	
	protected String getLibName(){
		return this.libName;
	}
	
	@SuppressWarnings("rawtypes")
	public void prepare(Map stormConf, TopologyContext context) throws Exception{
		loadOpenCV(stormConf);
		this.prepareOpenCVOp(stormConf, context);
	}
	
	@SuppressWarnings("rawtypes")
	protected void loadOpenCV( Map stormConf) throws RuntimeException, IOException{
		this.libName = (String)stormConf.get(VStormConfig.STORMCV_OPENCV_LIB);
		if(libName == null) NativeUtils.load();
		else NativeUtils.load(libName);
	}
	
	@SuppressWarnings("rawtypes")
	protected abstract void prepareOpenCVOp(Map stormConf, TopologyContext context) throws Exception;
	
}

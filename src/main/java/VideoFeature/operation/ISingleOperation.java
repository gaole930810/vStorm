package VideoFeature.operation;

import java.util.List;

import VideoFeature.model.BaseModel;


/**
 * 单帧操作接口 ，继承公共组件接口 {@link IOperation}，输入为一个单帧
 * 
 * @author liangzhaohao
 *
 */
public interface ISingleOperation <Output extends BaseModel> extends IOperation<Output>{

	public List<Output> execute(BaseModel baseModel) throws Exception;
	
}

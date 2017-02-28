package VideoFeature.operation;

import java.util.List;

import VideoFeature.model.BaseModel;


/**
 * 批量输入处理接口，相对于{@link ISingleOperation} ,输入是一系列的帧
 * 
 *
 * @param <Output>
 */
public interface IBatchOperation<Output extends BaseModel> extends IOperation<Output> {

	public List<Output> execute(List<BaseModel> input) throws Exception;
	
}

package VideoFeature.operation;

import java.io.Serializable;
import java.util.Map;

import VideoFeature.model.BaseModel;
import VideoFeature.model.serializer.BaseModelSerializer;
import backtype.storm.task.TopologyContext;


/**
 * 所有帧处理组件的操作接口
 * 
 * @author liangzhaohao on 15/4/15.
 */
public interface IOperation <Output extends BaseModel> extends Serializable{

//    public List<Output> exec(BaseModel baseModel);

    public void prepare(Map stormConf, TopologyContext context) throws Exception;

	/**
	 * Called when the topology is halted and can be used to clean up resources used
	 */
	public void deactivate();
	
	public BaseModelSerializer<Output> getSerializer();
	
//    public String getFields();
//
//    public String getUpperFields();
}

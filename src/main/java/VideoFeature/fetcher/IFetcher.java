package VideoFeature.fetcher;


import java.io.Serializable;
import java.util.Map;

import VideoFeature.model.BaseModel;
import VideoFeature.model.serializer.BaseModelSerializer;
import backtype.storm.task.TopologyContext;

/**
 * Created by liangzhaohao on 15/4/16.
 */
public interface IFetcher<Output extends BaseModel> extends Serializable  {

	@SuppressWarnings("rawtypes")
	public void prepare(Map stormConf, TopologyContext context) throws Exception;

    public Output fetchData();
    
//	public void prepare(Map stormConf, TopologyContext context) throws Exception;
	
	public BaseModelSerializer<Output> getSerializer();
	
	public void activate();

	public void deactivate() ;
}

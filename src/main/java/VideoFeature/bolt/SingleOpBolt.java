package VideoFeature.bolt;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import VideoFeature.model.BaseModel;
import VideoFeature.operation.ISingleOperation;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;

/**
 * 单个帧输入处理节点
 * 
 */
public class SingleOpBolt extends BaseOpBolt {

	
	private static final long serialVersionUID = 1L;
	private ISingleOperation<? extends BaseModel> operation;
	
    private OutputCollector collector;
//    private IOperation operation;

    public SingleOpBolt(ISingleOperation<? extends BaseModel> operation) {
        this.operation = operation;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context) throws Exception {
//        this.collector = collector;
        //System.out.println("------------------" + stormConf.get("storm.id"));
        try {
			operation.prepare(stormConf, context);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("Unale to prepare Operation ", e);
		}
    }

//    @Override
//    public void execute(Tuple input) {
////        System.out.println("bolt:=======" + input.toString()+"------classname " + operation.getFields());
//        Object object = input.getValueByField(operation.getUpperFields());
//        if(null == object){
//            return;
//        }
//        BaseModel baseModel = (BaseModel)object;
//        //Frame baseModel = (Frame)object;
////        System.out.println(baseModel.toString());
//
//        List<BaseModel> next = operation.exec(baseModel);
//        collector.ack(input);
//        if(null != next) {
//            collector.emit(new Values(next));
//        }
//    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(operation.getSerializer().getFields());
    }

	@Override
	List<? extends BaseModel> execute(BaseModel input) throws Exception {
		// TODO Auto-generated method stub
//		System.out.println("single"+"_"+input.getSeqNumber());
		List<? extends BaseModel> results = operation.execute(input);
		// copy metadata from input to output if configured to do so
		if(results !=null){
			for(BaseModel s : results){
				for(String key : input.getMetadata().keySet()){
					if(!s.getMetadata().containsKey(key)){
						s.getMetadata().put(key, input.getMetadata().get(key));
					}
				}
			}
		}

		return results;
	
	}
}

package VideoFeature.spout;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import VideoFeature.fetcher.IFetcher;
import VideoFeature.model.BaseModel;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

/**
 * 从流中读取帧数据
 */
public class VideoStreamSpout implements IRichSpout{

	private static final long serialVersionUID = 1L;
	private Logger logger = LoggerFactory.getLogger(VideoStreamSpout.class);
	
    SpoutOutputCollector collector;
	private boolean faultTolerant = false; // is not Fault Tolerant 
	private IFetcher fetcher;
	
    //just use local file to test
    public VideoStreamSpout(IFetcher fetcher){
        this.fetcher = fetcher;
    }
    
	public VideoStreamSpout setFaultTolerant(boolean faultTolerant){//设置可靠性
		this.faultTolerant = faultTolerant;
		return this;
	}

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(fetcher.getSerializer().getFields());
        //outputFieldsDeclarer.declare(fetcher.getSerializer().getFields());
    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {   	
		this.collector = spoutOutputCollector;
		if(map.containsKey("SetFaultTolerant")){
			faultTolerant = (Boolean) map.get("SetFaultTolerant");
		}
		if(faultTolerant){
//			long timeout = conf.get(StormCVConfig.STORMCV_CACHES_TIMEOUT_SEC) == null ? 30 : (Long)conf.get(StormCVConfig.STORMCV_CACHES_TIMEOUT_SEC);
//			int maxSize = conf.get(StormCVConfig.STORMCV_CACHES_MAX_SIZE) == null ? 500 : ((Long)conf.get(StormCVConfig.STORMCV_CACHES_MAX_SIZE)).intValue();
//			tupleCache = CacheBuilder.newBuilder()
//					.maximumSize(maxSize)
//					.expireAfterAccess(timeout, TimeUnit.SECONDS)
//					.build();
			//先设置为默认值...
		}

		// pass configuration to subclasses
		try {
			fetcher.prepare(map, topologyContext);
		} catch (Exception e) {
			logger.warn("Unable to configure spout due to ", e);
		}

        // init fetcher
//        fetcher.init();
    }

    @Override
    public void nextTuple() {
        BaseModel frameData = fetcher.fetchData();
//        BaseModel frameData2 = fetcher.fetchData();
//        int counter = 50;
//        while (null == frameData) {
//            frameData = fetcher.fetchData();
//            counter--;
//            if(0 == counter){
//                break;
//            }
//        }
//        while (null == frameData2) {
//            frameData2 = fetcher.fetchData();
//            counter--;
//            if(0 == counter){
//                break;
//            }
//        }
        //Frame frameData = fetcher.fetchData();
        
		if(frameData != null){
			try {
//				System.out.println("fetch frame :"+frameData.getSeqNumber());
				Values values = fetcher.getSerializer().toTuple(frameData);
				String id = frameData.getStreamId()+"_"+frameData.getSeqNumber();
	//			if(faultTolerant && tupleCache != null) tupleCache.put(id, values);
				collector.emit(values, id);
			}catch (IOException e) {
				logger.warn("Unable to fetch next frame from queue due to: "+e.getMessage());
			}
		}else {
//			System.out.println("catch the end");
		} 
//        Values values = new Values(frameData);
//        collector.emit(values);
//        if(null != frameData || null != frameData2) {
//            try {
//                Values values;
//                //if(frameData.getSeqNumber() % 2 == 0) {
//                    values = new Values(frameData, frameData2);
//                //} else {
//                //    values = new Values(frameData2, frameData);
//                //}
//                //Values values = new Values(frame.getImageType(), frame.getImageBytes(), frame.getBounding());
//                collector.emit(values);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }

    }
    
	@Override
	public void close() {
//		if(faultTolerant && tupleCache != null){
//			tupleCache.cleanUp();
//		}
		fetcher.deactivate();
	}

	@Override
	public void activate() {
		fetcher.activate();
	}

	@Override
	public void deactivate() {
		fetcher.deactivate();
	}

	@Override
	public void ack(Object msgId) {
//		if(faultTolerant && tupleCache != null){
//			tupleCache.invalidate(msgId);
//		}
	}

	@Override
	public void fail(Object msgId) {
//		logger.debug("Fail of: "+msgId);
//		if(faultTolerant && tupleCache != null && tupleCache.getIfPresent(msgId) != null){
//			collector.emit((Values)tupleCache.getIfPresent(msgId), msgId);
//		}
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}
}

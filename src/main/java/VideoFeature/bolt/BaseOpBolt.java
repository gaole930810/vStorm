package VideoFeature.bolt;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import VideoFeature.model.BaseModel;
import VideoFeature.model.Frame;
import VideoFeature.model.serializer.BaseModelSerializer;
import backtype.storm.Config;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import clojure.lang.PersistentArrayMap;

/**
 * StormCV's basic BaseRichBolt implementation that supports the use of {@link CVParticle} objects.
 * This bolt supports fault tolerance if it is configured to do so and supports the serialization of model objects.
 * 
 * @author Corne Versloot
 *
 */
public abstract class BaseOpBolt extends BaseRichBolt{

	private static final long serialVersionUID = -5421951488628303992L;
	
	protected Logger logger = LoggerFactory.getLogger(BaseOpBolt.class);
	protected HashMap<String, BaseModelSerializer<? extends BaseModel>> serializers = new HashMap<String, BaseModelSerializer<? extends BaseModel>>();
	protected OutputCollector collector;
	protected String boltName;
	protected long idleTimestamp = -1;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.boltName = context.getThisComponentId();
		
		try{
			PersistentArrayMap map = (PersistentArrayMap)conf.get(Config.TOPOLOGY_KRYO_REGISTER);
			for(Object className : map.keySet()){
				serializers.put((String)className, (BaseModelSerializer<? extends BaseModel>)Class.forName((String)map.get(className)).newInstance());
			}
		}catch(Exception e){
			logger.error("Unable to prepare CVParticleBolt due to ",e);
		}
		
		try {
			this.prepare(conf, context);
			idleTimestamp = System.currentTimeMillis();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	@SuppressWarnings({ "rawtypes" })
	@Override
	public void execute(Tuple input) {
		try{
			BaseModel cvt = deserialize(input);
			List<? extends BaseModel> results = execute(cvt);
			if(results !=null){
				for(BaseModel output : results){
					BaseModelSerializer serializer = serializers.get(output.getClass().getName());
					Frame frame = (Frame)output;
					if(serializers.containsKey(output.getClass().getName())){
						Values value = serializer.toTuple(output);						
						collector.emit(input, value);
//						System.out.println(output.toString());
					}else{
						// TODO: what else?
					}
				}
			}

			collector.ack(input);
		}catch(Exception e){
			logger.warn("Unable to process input", e);
			collector.fail(input);
		}
		
	}
	
	/**
	 * Deserializes a Tuple into a CVParticle type
	 * @param tuple
	 * @return
	 * @throws IOException 
	 */
	protected BaseModel deserialize(Tuple tuple) throws IOException{

		String typeName = tuple.getStringByField(BaseModelSerializer.TYPE);
		return serializers.get(typeName).fromTuple(tuple);
	}
	
	/**
	 * @return 组件当前运行时间（ms）
	 */
	public long getProcessTime(){
		return System.currentTimeMillis() - idleTimestamp;
	}
	/**
	 * Subclasses must implement this method which is responsible for analysis of 
	 * received CVParticle objects. A single input object may result in zero or more
	 * resulting objects which will be serialized and emitted by this Bolt.
	 * 
	 * @param input
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	abstract List<? extends BaseModel> execute(BaseModel input) throws Exception;
	
	@SuppressWarnings("rawtypes")
	abstract void prepare(Map stormConf, TopologyContext context) throws Exception;
}

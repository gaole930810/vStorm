package VideoFeature.model;

import java.util.HashMap;


import VideoFeature.model.serializer.BaseModelSerializer;
import backtype.storm.tuple.Tuple;

/**
 * 基本模型，所有其他Model类的基类
 * <li>streamId 流Id</li>
 * <li>seqNumber 帧序号</li>
 * <li>tuple </li>
 */
public abstract class BaseModel implements Comparable<BaseModel> {

    private String streamId;
    private long seqNumber;
    private Tuple  tuple;
	private HashMap<String, Object> metadata = new HashMap<String, Object>();
	
	/**
	 * Constructs a generic type based on the provided tuple. The tuple must contain streamID and sequenceNR
	 * values. 
	 * @param tuple
	 */
	@SuppressWarnings("unchecked")
	public BaseModel(Tuple tuple){
		this(tuple.getStringByField(BaseModelSerializer.STREAMID), tuple.getLongByField(BaseModelSerializer.SEQNUMBER));
		this.tuple = tuple;
		this.setMetadata((HashMap<String, Object>)tuple.getValueByField(BaseModelSerializer.METADATA));
	}
	
    public BaseModel(String streamId, long seqNumber) {
        this.streamId = streamId;
        this.seqNumber = seqNumber;
    }

    public long getSeqNumber() {
        return seqNumber;
    }

    public void setSeqNumber(long seqNumber) {
        this.seqNumber = seqNumber;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }
    
	public Tuple getTuple() {
		return tuple;
	}
	public HashMap<String, Object> getMetadata() {
		return metadata;
	}
	
	public void setMetadata(HashMap<String, Object> metadata) {
		if(metadata != null){
			this.metadata = metadata;
		}
	}

    @Override
    public int compareTo(BaseModel o) {
        return (int)(this.getSeqNumber() - o.getSeqNumber());
    }
}

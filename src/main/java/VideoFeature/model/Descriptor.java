package VideoFeature.model;

import java.awt.Rectangle;

import backtype.storm.tuple.Tuple;

/**
 * This {@link CVParticle} implementation represents a sparse descriptor which is part of a {@link Feature} and has the following fields:
 * <ul>
 * <li>bounding: an optionally zero dimensional rectangle indicating the region in the image this Descriptor describes</li>
 * <li>duration: the duration of the descriptor which may apply to temporal features like STIP</li>
 * <li>values: the float array used to store the actual descriptor (for example 128 values describing a SIFT point)</li>
 * </ul>  
 * 
 * @author Corne Versloot
 *
 */
public class Descriptor extends BaseModel {

	private Rectangle bounding;
	private long duration;
	private float[] values;
	
	public Descriptor(String streamId, long seqNumber, Rectangle bounding, long duration, float[] values) {
		super(streamId, seqNumber);
		this.bounding = bounding;
		this.values = values;
		this.duration = duration;
	}
	
	public Descriptor(Tuple tuple, Rectangle boundingBox, long duration, float[] values) {
		super(tuple);
		this.bounding = boundingBox;
		this.duration = duration;
		this.values = values;
	}

	public Rectangle getBounding() {
		return bounding;
	}

	public void setBounding(Rectangle bounding){
		this.bounding = bounding;
	}
	
	public long getDuration(){
		return duration;
	}

	public float[] getValues() {
		return values;
	}
	
	public void translate(int x, int y){
		this.bounding.x += x;
		this.bounding.y += y;
	}
	
	public Descriptor deepCopy(){
		float[] valuesCopy = new float[values.length];
		for(int i=0;i<values.length; i++){
			valuesCopy[i] = values[i];
		}
		Descriptor copy = new Descriptor(new String(this.getStreamId()), this.getSeqNumber(), new Rectangle(this.getBounding()), this.getDuration(), valuesCopy);
		copy.setMetadata(this.getMetadata());
		return copy;
	}
	
	public String toString(){
		return "Descriptor {stream:"+getStreamId()+", seqNumber:"+getSeqNumber()+", box:"+bounding+" duration: "+duration+" values"+values+"}";
	}
}
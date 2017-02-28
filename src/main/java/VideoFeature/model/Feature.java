package VideoFeature.model;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import backtype.storm.tuple.Tuple;

/**
 * This {@link CVParticle} implementation represents a single feature calculated for {@link Frame} and has the following fields:
 * <ul>
 * <li>name: the name of the feature like 'SIFT', 'SURF', 'HOG' etc</li>
 * <li>duration: the duration of the feature in case it describes a temporal aspect of multiple frames</li>
 * <li>sparseDescriptors: a list with {@link Descriptor} objects used to described sparse features like SIFT</li>
 * <li>denseDescriptors: a three dimensional float array much like the OpenCV Mat object which can be used to represent 
 * dense features like dense Optical Flow</li>
 * </ul>
 * It is not always clear how a specific descriptor should be stored and it is typically up to the characteristics of the 
 * topology and context what is the best way to go.  
 *  
 *
 */
public class Feature extends BaseModel{

	private String name;
	private long duration;
	private List<Descriptor> sparseDescriptors = new ArrayList<Descriptor>();
	private float[][][] denseDescriptors = new float[0][0][0];
//	private Rectangle bounding;	//对应的帧大小
	private int overlapPixel = 0;//如帧是部分数据块，指明重叠区域大小
	
	public Feature(String streamId, long seqNumber, String name, long duration, List<Descriptor> sparseDescriptors, float[][][] denseDescriptors/*,Rectangle bounding*/) {
		super(streamId, seqNumber);
		this.name = name;
		this.duration = duration;
//		this.bounding = bounding;
		if(sparseDescriptors != null){
			this.sparseDescriptors = sparseDescriptors;
		}
		if(denseDescriptors != null){
			this.denseDescriptors = denseDescriptors;
		}
	}
	
	public Feature(Tuple tuple, String name, long duration, List<Descriptor> sparseDescriptors, float[][][] denseDescriptors/*,Rectangle bounding*/) {
		super(tuple);
		this.name = name;
		this.duration = duration;
//		this.bounding = bounding;
		if(sparseDescriptors != null){
			this.sparseDescriptors = sparseDescriptors;
		}
		if(denseDescriptors != null){
			this.denseDescriptors = denseDescriptors;
		}
	}

	public String getName() {
		return name;
	}

	public List<Descriptor> getSparseDescriptors() {
		return sparseDescriptors;
	}
	
	public float[][][] getDenseDescriptors(){
		return denseDescriptors;
	}
	
	public long getDuration(){
		return this.duration;
	}
	
//	public Rectangle getBounding() {
//		return bounding;
//	}

//	public void setBounding(Rectangle bounding) {
//		this.bounding = bounding;
//	}
	
	public Feature deepCopy(){
		float[][][] denseCopy = new float[denseDescriptors.length][][];
		for(int x=0; x<denseDescriptors.length; x++){
			denseCopy[x] = new float[denseDescriptors[x].length][];
			for(int y=0; y<denseDescriptors[x].length; y++){
				denseCopy[x][y] = Arrays.copyOf(denseDescriptors[x][y], denseDescriptors[x][y].length);
			}
		}
		
		List<Descriptor> sparseCopy = new ArrayList<Descriptor>(this.sparseDescriptors.size());
		for(Descriptor d : sparseDescriptors){
			sparseCopy.add(d.deepCopy());
		}
		
		Feature copyFeature = new Feature(new String(this.getStreamId()), this.getSeqNumber(), new String(this.getName()), this.getDuration(), 
				sparseCopy, denseCopy/*,this.getBounding()*/).OverlapPixel(overlapPixel);
		copyFeature.setMetadata(this.getMetadata());
		return copyFeature;
	}
	
	public int getOverlapPixel() {
		return overlapPixel;
	}

	public Feature OverlapPixel(int overlapPixel) {
		this.overlapPixel = overlapPixel;
		return this;
	}

	public String toString(){
		return "Feature {stream:"+getStreamId()+", nr:"+getSeqNumber()+", name: "+name+", descriptors: "+sparseDescriptors+"}";
	}
}

package VideoFeature.operation;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import VideoFeature.model.BaseModel;
import VideoFeature.model.Descriptor;
import VideoFeature.model.Feature;
import VideoFeature.model.Frame;
import VideoFeature.model.serializer.BaseModelSerializer;
import VideoFeature.model.serializer.FeatureSerializer;
import VideoFeature.model.serializer.FrameSerializer;
import backtype.storm.task.TopologyContext;


/**
 * The TilesRecombinerOperation combines a set of tiles ({@link Frame} objects) originating from the same frame.
 * Tiling can be used to lower the load on image processing steps by decreasing the number of pixels to be analyzed 
 * at once. Tiles are Frames which can have {@link Feature}'s and {@link Descriptor}'s. This operation attempts to recombine
 * the tiles back into the original frame with the right features as if the tiling never took place. Hence Features are
 * merged as well and the 'local' locations of descriptors is translated to the 'global' location within the original
 * frame.  
 * 
 * @author Corne Versloot
 *
 */
public class PartitionCombinerOp implements IBatchOperation<BaseModel>{

	private static final long serialVersionUID = 7348857709867467970L;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private boolean outputFrame = false;
	@SuppressWarnings("rawtypes")
	private BaseModelSerializer serializer = new FeatureSerializer();
	private String imageType;

	/**
	 * 是否把特征点画在图像上
	 * @param frame
	 * @return
	 */
	public PartitionCombinerOp outputFrame(boolean frame){
		this.outputFrame = frame;
		if(outputFrame){
			this.serializer  = new FrameSerializer();
		}else{
			this.serializer = new FeatureSerializer();
		}
		return this;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context)	throws Exception {
		if(stormConf.containsKey("FreamEncoding")){
			imageType = (String)stormConf.get("FreamEncoding");
		}
	}

	@Override
	public void deactivate() {	}

	@SuppressWarnings("unchecked")
	@Override
	public BaseModelSerializer<BaseModel> getSerializer() {
		return this.serializer;
	}

	@Override
	public List<BaseModel> execute(List<BaseModel> input) throws Exception {
		Map<String, Feature> featureNameMap = new HashMap<String, Feature>();
		
		int width = 0, height = 0;
		for(BaseModel basemodel : input){
			if(basemodel instanceof Frame){
				Frame frame = (Frame)basemodel;
				width = (int)Math.max(width, frame.getBounding().getMaxX());
				height = (int)Math.max(height, frame.getBounding().getMaxY());
			}
		}
		
		Rectangle totalFrame = new Rectangle(0, 0, width, height);
		BufferedImage newImage = null;
		if(outputFrame){
			newImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		for(BaseModel basemodel : input){
			if(basemodel instanceof Frame){
				Frame frame = (Frame)basemodel;
				Rectangle box = frame.getBounding();
				for(Feature feature : frame.getFeatures()){
					merge(feature, featureNameMap, box, totalFrame);
				}
				if(outputFrame){
					newImage.getGraphics().drawImage(frame.getImage(), box.x, box.y, null);
				}
			}else{
				logger.warn("Can only operate on Frame but got "+particle.getClass().getName()+" else so input is dropped.");
			}
		}
		
		List<BaseModel> result = new ArrayList<BaseModel>();
		if(outputFrame){
			Frame newFrame = new Frame(input.get(0).getStreamId(), input.get(0).getSeqNumber()(), 
					imageType, newImage, ((Frame)input.get(0)).getTimeStamp(), totalFrame);
			newFrame.getFeatures().addAll(featureNameMap.values());
			result.add(newFrame);
		}else{
			result.addAll(featureNameMap.values());
		}
		return result;
	}
	
	/**
	 * Merges the new feature into the set with existing features
	 * @param newF
	 * @param features
	 * @param tile
	 */
	private void merge(Feature newF, Map<String, Feature> features, Rectangle tile, Rectangle frame){
		// first translate descriptors to new location given the boundingBox
		for(Descriptor descriptor : newF.getSparseDescriptors()){
			descriptor.translate(tile.x, tile.y);
		}
		
		// add newF to the set with features (possibly merging it with existing one which has the same name)
		Feature feature = features.get(newF.getName());
		if(feature == null){
			String streamId = newF.getStreamId();
			streamId = streamId.substring(0, streamId.lastIndexOf('_'));

			// add dense descriptor if present
			float[][][] dense;
			if(newF.getDenseDescriptors() != null && newF.getDenseDescriptors().length > 0){
				float[][][] oldDense = newF.getDenseDescriptors();
				dense = new float[(int)frame.getWidth()][(int)frame.getHeight()][oldDense[0][0].length];
				for(int x=0; x<oldDense.length; x++){
					for(int y=0; y<oldDense[x].length; y++){
						dense[tile.x+x][tile.y+y] = oldDense[x][y];
					}
				}
			}else{
				dense = null;
			}
			Feature combiFeature = new Feature(streamId, newF.getSequenceNr(), newF.getName(), newF.getDuration(), newF.getSparseDescriptors(), dense);
			features.put(newF.getName(), combiFeature);
		}else{
			feature.getSparseDescriptors().addAll(newF.getSparseDescriptors());
			Map<String, Object> metadata = feature.getMetadata();
			for(String key : newF.getMetadata().keySet()) if (!metadata.containsKey(key)){
				metadata.put(key, newF.getMetadata().get(key));
			}
			
			// add dense descriptors (if present)
			if(feature.getDenseDescriptors() != null && newF.getDenseDescriptors() != null){
				float[][][] oldDense = newF.getDenseDescriptors();
				float[][][] dense = feature.getDenseDescriptors();
				for(int x=0; x<oldDense.length; x++){
					for(int y=0; y<oldDense[x].length; y++){
						dense[tile.x+x][tile.y+y] = oldDense[x][y];
					}
				}
			}
		}
	}
	
}

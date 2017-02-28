package VideoFeature.operation;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
 * 把分块的特征合并
 * 
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
		int tileWidth=0,tileHeight=0;
		for(BaseModel basemodel : input){
			Rectangle box = null ;
			int overPixel = 0;
			if(basemodel instanceof Feature){
			  Feature ft  = (Feature)basemodel;
//				   box    = ft.getBounding();
				overPixel = ft.getOverlapPixel();
			}else if(basemodel instanceof Frame){
				Frame ft  = (Frame)basemodel;
				   box    = ft.getBounding();
				overPixel = ft.getOverlapPixel();
			}
			if(box!=null){
				width = (int)Math.max(width, box.getMaxX());	//取得完整帧的最大X坐标，即width
				height = (int)Math.max(height, box.getMaxY());	//取得完整帧的最大Y坐标，即height
				if(tileWidth == 0 && box.getMinX() == 0){
					tileWidth = (int) (box.getWidth() - overPixel);//取得分块数据宽度
				}
				if(tileHeight == 0 && box.getMinY() == 0){
					tileHeight = (int) (box.getHeight() - overPixel);//取得分块数据高度
				}
			}
		}
		
		System.out.println("Combiner sequence number:"+input.get(0).getSeqNumber()+" "+input.size()+"width:"+width+",height:"+height);
		Rectangle totalFrame = new Rectangle(0, 0, width, height);
		BufferedImage newImage = null;
		
		if(outputFrame){
			newImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		for(BaseModel basemodel : input){
			List<Rectangle> boxes = new ArrayList<Rectangle>();
			List<Feature> features = new ArrayList<Feature>();
			if(basemodel instanceof Frame){
				Frame f = (Frame) basemodel;
//				features = f.getFeatures();
				features.add( f.getFeature()) ;
				boxes.add(f.getBounding());
				if(outputFrame){
					newImage.getGraphics().drawImage(f.getImageBuf(), f.getBounding().x, f.getBounding().y, null);
				}
			}else{
				logger.warn("Can only operate on Frame but got "+input.getClass().getName()+" else so input is dropped.");
				if(basemodel instanceof Feature){
					features.add((Feature)basemodel);
					
				}
			}
			int i =0;
			for(Feature feature : features){
//				Rectangle box = feature.getBounding();
				
				merge(feature, featureNameMap, boxes.get(i++), totalFrame, tileWidth, tileHeight);//合并分块特征
			}
			
		}
		
		List<BaseModel> result = new ArrayList<BaseModel>();
		if(outputFrame){
			Frame newFrame = new Frame(input.get(0).getStreamId(), input.get(0).getSeqNumber(), 
					newImage,imageType, ((Frame)input.get(0)).getTimeStamp(), totalFrame);
//			newFrame.getFeatures().addAll(featureNameMap.values());
			newFrame.setFeature(featureNameMap.get(0));
			result.add(newFrame);
		}else{
			result.addAll(featureNameMap.values());
		}
		return result;
	}
	
	/**
	 * 合并分块数据的特征
	 * @param newF 分块的特征
	 * @param features 合并的特征
	 * @param tile  分块的大小
	 * @param frame 原图像的大小
	 */
	private void merge(Feature newF, Map<String, Feature> features, Rectangle tile, Rectangle frame, int tileWidth, int tileHeight){
		String streamId = newF.getStreamId();
		int tileIndex = Integer.parseInt(streamId.substring(streamId.lastIndexOf("_")+1,streamId.length()));
		int rTiles = (int) (frame.getWidth()/tileWidth + frame.getWidth()%tileWidth);
		int cTiles = (int) (frame.getHeight()/tileHeight + frame.getHeight()%tileHeight);
		List<Descriptor> descriptorList = newF.getSparseDescriptors();
		Iterator<Descriptor> it = descriptorList.iterator();
		// 将每个特征向量转换为原图像中的坐标,并移除分块数据中重叠区域的特征点
		while(it.hasNext()){
			Descriptor descriptor = it.next();
			descriptor.translate(tile.x, tile.y); //特征转换为原坐标
			int x = (int)descriptor.getBounding().getX() ;
			int y = (int)descriptor.getBounding().getX() ;
			//排除重叠区域
			if(x < (tileIndex % rTiles) * tileWidth || x > (tileIndex % rTiles +1) * tileWidth ){
				it.remove();
			}
			else if(y < (tileIndex % cTiles) * tileHeight || y > (tileIndex % cTiles +1) * tileHeight ){
				it.remove();
			}
		}
		
		// 合并第一块数据时features中没有对应数据
		Feature feature = features.get(newF.getName());
		if(feature == null){//第一块数据
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
			Feature combiFeature = new Feature(streamId, newF.getSeqNumber(), newF.getName(), newF.getDuration(), newF.getSparseDescriptors(), dense/*,newF.getBounding()*/);
			features.put(newF.getName(), combiFeature);
		}else{
			feature.getSparseDescriptors().addAll(newF.getSparseDescriptors());	//顺序如何保证?
			Map<String, Object> metadata = feature.getMetadata();
			for(String key : newF.getMetadata().keySet())
				if (!metadata.containsKey(key)){
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

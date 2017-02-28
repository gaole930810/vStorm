package VideoFeature.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

//import ch.qos.logback.classic.Logger;

import VideoFeature.model.BaseModel;
import VideoFeature.model.Descriptor;
import VideoFeature.model.Feature;
import VideoFeature.model.Frame;
import VideoFeature.model.serializer.BaseModelSerializer;
import VideoFeature.model.serializer.FeatureSerializer;
import VideoFeature.model.serializer.FrameSerializer;
import backtype.storm.task.TopologyContext;
import backtype.storm.utils.Utils;
import VideoFeature.operation.*;

/**
 * 特征匹配接口，输入为单帧特征 ，继承{@link ISingleOperation}
 * @param name 
 * @param imageUrl 带匹配的图片url 
 * 
 */

public class FeatureMatchOp extends OpenCVOp<BaseModel>  implements ISingleOperation<BaseModel>{
	
	private static final long serialVersionUID = -5543735411296339252L;
	private Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	private String name;
	private String imageUrl;
	private int detectorType;
	private int descriptorType;
	private Mat trainDescriptors;	//待匹配图像的SIFT描述符
	private int matchCounter = 0;
	private boolean outputFrame = false;
	@SuppressWarnings("rawtypes")
	private BaseModelSerializer serializer = new FeatureSerializer();
	
	public FeatureMatchOp(String name, int detectorType, int descriptorType,String imageUrl){
        this.name = name;
        this.descriptorType = descriptorType;
        this.detectorType = detectorType;
        this.imageUrl = imageUrl;
	}

	public FeatureMatchOp imageUrl(String imageUrl){
		this.imageUrl = imageUrl;
		return this;
	}
	
	public FeatureMatchOp outputFrame(boolean outputFrame){
		this.outputFrame = outputFrame;
		return this;
	}
	@Override
	public List<BaseModel> execute(BaseModel input) throws Exception 
	{
		Feature feature = null;
		if(input instanceof Frame){

			Frame f = (Frame)input;
//			for(Feature ft : f.getFeatures()){
//				feature = ft;
//			}
			feature = f.getFeature();
		}else if(input instanceof Feature){
			feature = (Feature)input;
		}
		if(feature == null){
			logger.warn("Can not got feature from "+input.getClass().getName()+" number:"+((Frame)input).getSeqNumber()+
					((Frame)input).getFlag()+" so input is dropped.");
			return null;
		}
		List<BaseModel> result = new ArrayList<BaseModel>();
		System.out.println("SIFT matching"+"_"+feature.getSeqNumber());
//		System.out.println("train image mat:"+trainDescriptors.rows()+"x"+trainDescriptors.cols()+"type-"+trainDescriptors.type());
		
		Mat descriptor = getDescriptorsFromFeature(feature);//帧的描述符
//		System.out.println("current image mat:"+descriptor.rows()+"x"+descriptor.cols()+"type-"+descriptor.type());
		
		MatOfDMatch matches = new MatOfDMatch();
	    DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
	    matcher.match(descriptor, trainDescriptors, matches);
	    
//	    System.out.println("matches.size() : "+matches.size());//匹配上的点
	   
	    MatOfDMatch matchesFiltered = new MatOfDMatch();	//筛选出较好的匹配点

	    List<DMatch> matchesList = matches.toList();
	    List<DMatch> bestMatches= new ArrayList<DMatch>();
	    
	    Double max_dist = 0.0;
	    Double min_dist = 100.0;

	    for (int i = 0; i < matchesList.size(); i++){
	        Double dist = (double) matchesList.get(i).distance;

	        if (dist < min_dist && dist != 0){
	            min_dist = dist;
	        }

	        if (dist > max_dist){
	            max_dist = dist;
	        }

	    }
	    System.out.println("min_distL:"+min_dist+",max_dist:"+max_dist);
	    /*
	     * 发送帧到下游
	     * 
	     */
	    if(outputFrame && input instanceof Frame){
        	result.add((Frame)input);
        }else{
        	result.add(feature);
        }
	    
	    if(min_dist > 50 ){
	        System.out.println("No match found");
	        return result;
	    }

	    double threshold = 3 * min_dist;
	    double threshold2 = 2 * min_dist;
	    if (threshold > 75)
	    {
	        threshold  = 75;
	    }
	    else if (threshold2 >= max_dist)
	    {
	        threshold = min_dist * 1.1;
	    }
	    else if (threshold >= max_dist)
	    {
	        threshold = threshold2 * 1.4;
	    }
	    System.out.println("Threshold : "+threshold);
	    for (int i = 0; i < matchesList.size(); i++)
	    {
	        Double dist = (double) matchesList.get(i).distance;
	        if (dist < threshold)
	        {
	            bestMatches.add(matches.toList().get(i));
	            //System.out.println(String.format(i + " best match added : %s", dist));
	        }
	    }
	    matchesFiltered.fromList(bestMatches);
	    System.out.println("matchesFiltered.size() : " + matchesFiltered.size());
        
	    
	    if(matchesFiltered.rows() >= 1)
	    {
	        ++matchCounter;

	        System.out.println("match found,total match frame:"+matchCounter);
	        return result;
	    }
	    else
	    {
	        return result;
	    }
	}

	@Override
	public void deactivate() 
	{
		
	}

	private Mat getDescriptorsFromFeature(Feature feature){	
		List<Descriptor> descrList = feature.getSparseDescriptors();
		int rows = descrList.size();
		int cols = descrList.get(0).getValues().length;
		Mat descriptorMat = new Mat(rows, cols ,org.opencv.core.CvType.CV_32F); 
		for(int r=0; r<rows; r++){
			for(int c=0; c<cols; c++){
				float data = descrList.get(r).getValues()[c];
				descriptorMat.put(r, c,data);
			}
		}
		return descriptorMat;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public BaseModelSerializer<BaseModel> getSerializer() 
	{
		if(outputFrame) this.serializer = new FrameSerializer();
		return this.serializer;
	}
	
	

	public void getTrainImageDescriptor(){
		//先计算待匹配图像的SIFT特征描述符
		Mat trainImage = Highgui.imread(imageUrl);
		FeatureDetector siftDetector = FeatureDetector.create(detectorType);
		MatOfKeyPoint mokp = new MatOfKeyPoint();
		siftDetector.detect(trainImage, mokp);
		
		this.trainDescriptors = new Mat();
		DescriptorExtractor extractor = DescriptorExtractor.create(descriptorType);
		extractor.compute(trainImage, mokp, this.trainDescriptors);
		
		
//		Utils.sleep(100);
        
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void prepareOpenCVOp(Map stormConf, TopologyContext context)
			throws Exception {
		getTrainImageDescriptor();
	}
	
}

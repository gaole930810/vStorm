package VideoFeature.operation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
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

import javax.imageio.ImageIO;
/**
 * 输入为一个帧 {@link Frame}.输出为这个帧的特征{@link Feature}，单个{@link BaseModel}输出
 * 
 * Depending on its configuration this operation can use non-free functions from the OpenCV library which <b><i>may be patented in
 *  some countries or have some other limitations on the use!</i></b> See <a href="http://docs.opencv.org/modules/nonfree/doc/nonfree.html">this page</a>.
 * 
 *
 */
public class SIFTExtractionOp extends OpenCVOp<BaseModel> implements ISingleOperation<BaseModel> {

	private static final long serialVersionUID = 3575211578480683490L;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private int detectorType;
	private int descriptorType;
	private String featureName;
	private boolean outputFrame = false;
	@SuppressWarnings("rawtypes")
	private BaseModelSerializer serializer = new FeatureSerializer();
	private String logFilePath;
	private long totalRunTime = 0;		//记录总耗时
	private FileWriter fwLog = null;
	private static Color[] colors = new Color[]{Color.RED, Color.BLUE, Color.GREEN, Color.PINK, Color.YELLOW, Color.CYAN, Color.MAGENTA};
	/**
	 * @param featureName the name of the feature (i.e. SIFT, SURF, ...) which will be put in the generated Feature's name field
	 * @param detectorType the keypoint detection algorithm to use, must be one of org.opencv.features2d.FeatureDetector constants 
	 * @param descriptorType the type of descriptor to use, must be one of <a href=org.opencv.features2d.DescriptorExtractor constants
	 * @see <a href="http://docs.opencv.org/java/index.html?org/opencv/features2d/FeatureDetector.html">OpenCV FeatureDetector</a>
	 * @see <a href="http://docs.opencv.org/java/index.html?org/opencv/features2d/FeatureDetector.html">OpenCV DescriptorExtractor</a>
	 */
	public SIFTExtractionOp(String featureName, int detectorType, int descriptorType,String logFilePath){
		this.featureName = featureName;
		this.detectorType = detectorType;
		this.descriptorType = descriptorType;
		this.logFilePath = logFilePath;
	}
	
	/**
	 * Sets the output of this Operation to be a {@link Frame} which contains all the features. If set to false
	 * this Operation will return a {@link Feature} object which means the Frame will no longer be available.
	 * Default value after construction is FALSE.
	 * @param frame
	 * @return
	 */
	public SIFTExtractionOp outputFrame(boolean frame){
		this.outputFrame = frame;
		if(outputFrame){
			this.serializer = new FrameSerializer();
		}else{
			this.serializer = new FeatureSerializer();
		}
		return this;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected void prepareOpenCVOp(Map stormConf, TopologyContext context) throws Exception {
		fwLog = new FileWriter(logFilePath+"/timelog.txt");
	}

	@Override
	public void deactivate() {	}

	@SuppressWarnings("unchecked")
	@Override
	public BaseModelSerializer<BaseModel> getSerializer() {
		return this.serializer;
	}

	@Override
	public List<BaseModel> execute(BaseModel frame) throws Exception {
		long start = System.currentTimeMillis();
		List<BaseModel> result = new ArrayList<BaseModel>();
		if(!(frame instanceof Frame)) return null;
		Frame sf = (Frame)frame;
		if(sf.getFlag() == -1){//流结束
			long runTime = getProcessTime();
			System.out.println("stream is end,sift extraction run time = "+runTime+" ms"+",real run time ="+totalRunTime+" ms");
			fwLog.write(totalRunTime+"\n");	//IO 耗时操作
			fwLog.flush();
		//	fwLog =new FileWriter(logFilePath+"/timelog.txt");
		//	fwLog.write("stream is end,sift extraction run time = "+runTime+" ms"+",real run time ="+totalRunTime+" ms"+"\n");
		//	fwLog =new FileWriter(logFilePath+"/log.txt");
		//	fwLog.write("stream is end,sift extraction run time = "+runTime+" ms"+",real run time ="+totalRunTime+" ms"+"\n");
			
			result.add(sf);
			return result;
		}
		if(sf.getImageType().equals(Frame.NO_IMAGE)){

			return null;
		}
		
		try{
			MatOfByte mob = new MatOfByte(sf.getImageBytes());
			Mat imageSrc = Highgui.imdecode(mob, Highgui.CV_LOAD_IMAGE_ANYCOLOR);
			Mat image_pre = new Mat(imageSrc.size(),org.opencv.core.CvType.CV_8UC1); //gao
			Mat image = new Mat(imageSrc.size(),org.opencv.core.CvType.CV_8UC1);
			
			
//			imageSrc.copyTo(image);
			
//			Imgproc.cvtColor(imageSrc, image, Imgproc.COLOR_BGR2GRAY);
			Imgproc.cvtColor(imageSrc, image_pre, Imgproc.COLOR_BGR2GRAY);   //gao
//gao			
			Imgproc.GaussianBlur(image_pre, image,new Size(3,3), 0);
			
			FeatureDetector siftDetector = FeatureDetector.create(detectorType);
			MatOfKeyPoint mokp = new MatOfKeyPoint();
			siftDetector.detect(image, mokp);
			List<KeyPoint> keypoints = mokp.toList();
			
			Mat descriptors = new Mat();
			DescriptorExtractor extractor = DescriptorExtractor.create(descriptorType);
			extractor.compute(image, mokp, descriptors);
			List<Descriptor> descrList = new ArrayList<Descriptor>();
			float[] tmp = new float[1];
			for(int r=0; r<descriptors.rows(); r++){
				float[] values = new float[descriptors.cols()];
				for(int c=0; c<descriptors.cols(); c++){
					descriptors.get(r, c, tmp);
					values[c] = tmp[0];
				}
				descrList.add(new Descriptor(sf.getStreamId(), sf.getSeqNumber(), new Rectangle((int)keypoints.get(r).pt.x, (int)keypoints.get(r).pt.y, 0, 0), 0, values));
			}
//			System.out.println("SIFT Extration"+"_"+frame.getSeqNumber());
			
			Feature feature = new Feature(sf.getStreamId(), sf.getSeqNumber(), featureName, 0, descrList, null).OverlapPixel(sf.getOverlapPixel());
			if(outputFrame){
//				sf.getFeatures().add(feature);
				sf.setFeature(feature);
//				if(sf.getFeatures().get(0) == null) System.out.println("sssssssssssssa!!!!!!!!!!!!");
				result.add(sf);
				
			}else{
				result.add(feature);
			}
/*			
            // 观察某个视频帧及其相邻两帧的特征点分布			
//			if(Math.abs(sf.getSeqNumber()-533)<=2)
				checkFeatureDistribute(sf,"D:\\video\\out\\avi5\\image\\"); */
//			saveFrameWithFeature(sf,"D:\\video\\out\\avi7\\Featureimage\\");
			
		}catch(Exception e){
			// catching exception at this point will prevent the sent of a fail! 
			logger.warn("Unable to extract features for frame!", e);
		}
		long t = System.currentTimeMillis() - start;
		totalRunTime += t;
//		logger.info("SIFT Extration"+"_"+frame.getSeqNumber()+"time = "+t+" ms.");
		return result;
	}

/* gaole 
 * 观察特征点在图像上的分布
 * */	
	public void checkFeatureDistribute(Frame frame,String imageSavePath) throws Exception{
		
		saveFrameAsPng(frame,imageSavePath);
		saveFrameWithFeature(frame,imageSavePath);
		saveFeatureLocation(frame,imageSavePath);
	}
	
	public void saveFrameAsPng(Frame frame,String imageSavePath) throws Exception{
				
		BufferedImage image = frame.getImageBuf();
		File f = new File(imageSavePath+frame.getSeqNumber()+".png");
		ImageIO.write(image, "png", f);
	}
	public void saveFrameWithFeature(Frame frame,String imageSavePath) throws Exception{
		BufferedImage image = frame.getImageBuf();
		Graphics2D graphics = image.createGraphics();
		int colorIndex = 0;
		Feature feature = frame.getFeature();
		graphics.setColor(colors[colorIndex % colors.length]);
		for(Descriptor descr : feature.getSparseDescriptors()){
			Rectangle box = descr.getBounding().getBounds();
			if(box.width == 0 ) box.width = 1;
			if(box.height == 0) box.height = 1;
			graphics.draw(box);
		}
		colorIndex++;
		File f = new File(imageSavePath+frame.getSeqNumber()+"_WithFeature.png");
		ImageIO.write(image, "png", f);

	}
	public void saveFeatureLocation(Frame frame,String imageSavePath){
		
		try {
			BufferedImage image = new BufferedImage(frame.getImageBuf().getWidth(),frame.getImageBuf().getHeight(),frame.getImageBuf().getType());
			
			Graphics2D graphics = image.createGraphics();
			int colorIndex = 0;
			Feature feature = frame.getFeature();
			graphics.setColor(colors[colorIndex % colors.length]);
			for(Descriptor descr : feature.getSparseDescriptors()){
				Rectangle box = descr.getBounding().getBounds();
				if(box.width == 0 ) box.width = 1;
				if(box.height == 0) box.height = 1;
				graphics.draw(box);
				colorIndex++;
				graphics.setColor(colors[colorIndex % colors.length]);
				
			}
			
			File f = new File(imageSavePath+frame.getSeqNumber()+"_SiftLocation.png");
			ImageIO.write(image, "png", f);
			
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("error to create new image...");
			e.printStackTrace();
		}

	}

}

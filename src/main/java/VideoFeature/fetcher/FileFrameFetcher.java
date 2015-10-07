package VideoFeature.fetcher;


import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import VideoFeature.model.BaseModel;
import VideoFeature.model.Frame;
import VideoFeature.model.serializer.BaseModelSerializer;
import VideoFeature.model.serializer.FrameSerializer;
import VideoFeature.utils.StreamReader;
import backtype.storm.task.TopologyContext;

/**
 * Created by liangzhaohao on 15/3/24.
 */
public class FileFrameFetcher implements IFetcher<BaseModel>{

	private static final long serialVersionUID = 1L;
	private Logger logger = LoggerFactory.getLogger(FileFrameFetcher.class);
    private final static int MAX_SIZE = 100;
    private StreamReader reader ;
    private String location; //单个流
	private int frameSkip = 1;//跳帧
	private int groupSize = 1;//
	private int sleepTime = 0;//
	private int batchSize = 1;//是否采用groupFrame
    private LinkedBlockingQueue<Frame> frameQueue;    // frames buffer
	private String imageType;//Encoding Type

    public FileFrameFetcher(String filePath) {
        this.location = filePath;
        frameQueue = new LinkedBlockingQueue<Frame>(MAX_SIZE);
    }

	public FileFrameFetcher frameSkip(int skip){
		this.frameSkip = skip;
		return this;
	}
	public FileFrameFetcher groupSize(int size){
		this.groupSize = size;
		return this;
	}
	public FileFrameFetcher sleep (int ms){
		this.sleepTime = ms;
		return this;
	}
	
	public FileFrameFetcher groupOfFramesOutput(int nrFrames){
		this.batchSize = nrFrames;
		return this;
	}
	
	@SuppressWarnings({ "rawtypes"})
	@Override
	public void prepare(Map conf, TopologyContext context) throws Exception {

		if(conf.containsKey("FreamEncoding")){
			imageType = (String)conf.get("FreamEncoding");
		}
		
	}
    // init
    public void init() {
        // set image type jpg temporary
        reader = new StreamReader(frameQueue, Frame.JPG_IMAGE, location);
        Thread thread = new Thread(reader);
        thread.start();
    }

    // get Frame data
    @Override
    public Frame fetchData(){
        Frame frame = frameQueue.poll();
        if(null != frame) {
            return frame;
        }
        return null;
    }

    // get serializer
    public BaseModelSerializer getSerializer(){
		if(batchSize <= 1) return new FrameSerializer();
		else return null;
    }

	@Override
	public void activate() {
		// TODO Auto-generated method stub

		if(reader != null){
			this.deactivate();
		}
		
		init();
	
	}

	@Override
	public void deactivate() {
		// TODO Auto-generated method stub
		if(reader != null) reader.stop();
		reader = null;
	}
}

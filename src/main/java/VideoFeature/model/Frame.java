package VideoFeature.model;


import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import VideoFeature.utils.ImageUtils;
import backtype.storm.tuple.Tuple;

/**
 * 帧模型，存储帧的实际数据，flag指定该帧是否为关键帧
 * flag = 0 普通帧,
 * flag = 1 关键帧,
 * flag = 2 取得关键帧的边界帧,
 * flag = 3 boundary边界帧,
 *
 */
public class Frame extends BaseModel{
    public final static String JPG_IMAGE = "jpg";
    public final static String PNG_IMAGE = "png";
    public final static String GIF_IMAGE = "gif";
    public final static String NO_IMAGE = "none";

	private long timeStamp;
    private String imageType = JPG_IMAGE;
    private byte[] imageBytes;
    private BufferedImage imageBuf;
    private Rectangle bounding;
    private int overlapPixel = 0;//如帧是部分数据块，指明重叠区域大小
    private short flag = 0; //是否为关键帧，是否为取得关键帧的边界

	private Feature feature ;// 特征列表,一个帧可以有多种特征
    
    
	public Frame(String streamId, long seqNumber, BufferedImage imageBuf, String imageType, long timeStamp, Rectangle bounding, Feature feature) throws IOException {
		this(streamId, seqNumber, imageBuf,  imageType,timeStamp, bounding);
		/*if(features != null)*/
		this.feature = feature;
	}
	
	public Frame(String streamId, long seqNumber, String imageType, byte[] imageBytes, long timeStamp, Rectangle bounding, Feature feature) {
		this(streamId, seqNumber, imageType, imageBytes, timeStamp, bounding);
		/*if(features != null)*/ 
		this.feature = feature;
	}
	
    public Frame(String steamId, long seqNumber, BufferedImage imageBuf, String imageType, long timeStamp,Rectangle bounding) throws IOException {
        super(steamId, seqNumber);
        this.imageType = imageType;
        setImageBuf(imageBuf);
        setTimeStamp(timeStamp);
        this.bounding = bounding;
    }

    public Frame(String steamId, long seqNumber, String imageType, byte[] imageBytes, long timeStamp,Rectangle bounding) {
        super(steamId, seqNumber);
        this.imageType  = imageType;
        this.imageBytes = imageBytes;
        this.setTimeStamp(timeStamp);
        this.bounding   = bounding;
    }

	public Frame(Tuple tuple, String imageType, byte[] image, long timeStamp, Rectangle bounding) {
		super(tuple);
		this.imageType = imageType;
		this.imageBytes = image;
		this.setTimeStamp(timeStamp);
		this.bounding = bounding;
	}
	
    public BufferedImage getImageBuf() throws IOException {
		if(null == imageBytes){
			imageType = NO_IMAGE;
			return null;
		}
    	
    	if(null == imageBuf) {
            return ImageUtils.bytesToImage(imageBytes, imageType);
        }
        return imageBuf;
    }

    public void setImageBuf(BufferedImage imageBuf) throws IOException {
        this.imageBuf = imageBuf;
		if(imageBuf != null){
			if(imageType.equals(NO_IMAGE)) imageType = JPG_IMAGE;
			this.imageBytes = ImageUtils.imageToBytes(imageBuf, imageType);		//waste much time??????? cccc
		}else{
			this.imageBytes = null;
			this.imageType = NO_IMAGE;
		}
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) throws IOException {
        String oldType = this.imageType  ;
        this.imageType = imageType;
        /*
         * change their image type
         */
		if(imageBuf != null && !imageType.equals(oldType)){
			imageBytes = ImageUtils.imageToBytes(imageBuf, imageType);  //waste much time ????? cccc
		}else{
			imageBuf = ImageUtils.bytesToImage(imageBytes);
			imageBytes = ImageUtils.imageToBytes(imageBuf, imageType);
		}
    }

    public byte[] getImageBytes() throws IOException {
        if(null == imageBytes) {
            return ImageUtils.imageToBytes(imageBuf, imageType);
        }
        return imageBytes;
    }

    public void setImageBuf(byte[] imageBytes,String imgType) {
        this.imageBytes = imageBytes;
		this.imageType = imgType;
		this.imageBuf = null;
    }

    public Rectangle getBounding() {
        return bounding;
    }

    public void setBounding(Rectangle bounding) {
        this.bounding = bounding;
    }

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public int getOverlapPixel() {
		return overlapPixel;
	}

	public Frame OverlapPixel(int overlapPixel) {
		this.overlapPixel = overlapPixel;
		return this;
	}

	public Feature getFeature() {
		return feature;
	}
	public void setFeature(Feature feature) {
		this.feature = feature;
	}
	
    public short getFlag() {
		return flag;
	}
    /*
	 * flag = 0 普通帧,
	 * flag = 1 关键帧,
	 * flag = 2 取得关键帧的边界帧,
	 * flag = 3 boundary边界帧,
	 * flag = -1 or 4 流结束,
	 */
	public Frame Flag(short flag) {
		this.flag = flag;
		return this;
	}
	
	public String toString(){
		String result= "Frame : {streamId:"+getStreamId()+", sequenceNr:"+getSeqNumber()+", timestamp:"+getTimeStamp()+", imageType:"+imageType;
//		for(Feature f : features) result += f.getName()+" = "+f.getSparseDescriptors().size()+", ";
		return result + " }";
	}

    
}

package VideoFeature.model;


import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import VideoFeature.utils.ImageUtils;
import backtype.storm.tuple.Tuple;

/**
 * Created by liangzhaohao on 15/3/24.
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
	
	public String toString(){
		String result= "Frame : {streamId:"+getStreamId()+", sequenceNr:"+getSeqNumber()+", timestamp:"+getTimeStamp()+", imageType:"+imageType;
//		for(Feature f : features) result += f.getName()+" = "+f.getSparseDescriptors().size()+", ";
		return result + " }";
	}

    
}

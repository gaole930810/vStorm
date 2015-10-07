package VideoFeature.model.serializer;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import VideoFeature.model.BaseModel;
import VideoFeature.model.Frame;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * 单帧序列化器，继承抽象帧序列化器{@link BaseModelSerializer} 
 * @author liangzhaohao on 15/4/15.
 */
public class FrameSerializer extends BaseModelSerializer<Frame> implements Serializable{

	private static final long serialVersionUID = 1L;
    public final static String IMAGE_TYPE = "IMAGE_TYPE";
    public final static String IMAGE_BYTES = "IMAGE_BYTES";
	public static final String TIME_STAMP = "TIME_STAMP";
    public final static String BOUNDING = "BOUNDING";


//    /**
//     * streamId
//     * seqNumber
//     * imageType
//     * length of images bytes
//     * images bytes
//     * bounding
//     * @param kryo
//     * @param output
//     * @param type
//     */
//    @Override
//    public void write(Kryo kryo, Output output, Frame type) {
//
//        output.writeString(type.getStreamId());
//        output.writeLong(type.getSeqNumber());
//        output.writeString(type.getImageType());
//
//        byte[] buffer = null;
//
//        try {
//            buffer = type.getImageBytes();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        if(buffer != null) {
//            output.writeInt(buffer.length);
//            output.writeBytes(buffer);
//        } else {
//            output.writeInt(0);
//        }
//
//    }
//
//    @Override
//    public Frame read(Kryo kryo, Input input, Class<Frame> aClass) {
//        String streamId = input.readString();
//        Long seqNumber = input.readLong();
//        String imageType = input.readString();
//
//        int len = input.readInt();
//        byte[] buffer = null;
//        if (len != 0) {
//            buffer = new byte[len];
//            input.readBytes(buffer);
//        }
//
//        Frame frame = new Frame(streamId, seqNumber, imageType, buffer, null);
//        return frame;
//
//    }
//
//    public Fields getFields() {
//        List<String> fields = new ArrayList<String>();
//        fields.add(IMAGE_TYPE);
//        fields.add(IMAGE_BYTES);
//        fields.add(BOUNDING);
//
//        return new Fields(fields);
//    }
    /*
     * serialized
     * @see VideoFeature.model.serializer.BaseModelSerializer#writeObject(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Output, VideoFeature.model.BaseModel)
     */
	@Override
	protected void writeObject(Kryo kryo, Output output, Frame frame)
			throws Exception {
		// TODO Auto-generated method stub
		output.writeLong(frame.getTimeStamp());
		output.writeString(frame.getImageType());
		byte[] buffer = frame.getImageBytes();
		if(buffer != null){
			output.writeInt(buffer.length);
			output.writeBytes(buffer);
		}else{
			output.writeInt(0);
		}
		output.writeFloat((float)frame.getBounding().getX());
		output.writeFloat((float)frame.getBounding().getY());
		output.writeFloat((float)frame.getBounding().getWidth());
		output.writeFloat((float)frame.getBounding().getHeight());
		
//		kryo.writeObject(output, frame.getFeatures());
	
		
	}
	/*
	 * deserialized
	 */
	@Override
	protected Frame readObject(Kryo kryo, Input input, Class<Frame> clas,
			String streamId, long sequenceNr) throws Exception {
		// TODO Auto-generated method stub

		long timeStamp = input.readLong();
		String imageType = input.readString();
		int buffSize = input.readInt();
		byte[] buffer = null;
		if(buffSize > 0){
			buffer = new byte[buffSize];
			input.readBytes(buffer);
		}
		Rectangle boundingBox = new Rectangle(Math.round(input.readFloat()), Math.round(input.readFloat()), 
				Math.round(input.readFloat()), Math.round(input.readFloat()));
//		List<Feature> features = kryo.readObject(input, ArrayList.class);
		
		return new Frame(streamId, sequenceNr, imageType, buffer, timeStamp, boundingBox);
	
	}

	@Override
	protected Frame createObject(Tuple tuple) throws IOException {
		// TODO Auto-generated method stub

		byte[] buffer = tuple.getBinaryByField(IMAGE_BYTES);
		Frame frame;
		if(buffer == null){
			frame = new Frame(tuple, tuple.getStringByField(IMAGE_TYPE), null, tuple.getLongByField(TIME_STAMP), (Rectangle)tuple.getValueByField(BOUNDING));
		}else{
			frame = new Frame(tuple, tuple.getStringByField(IMAGE_TYPE), buffer, tuple.getLongByField(TIME_STAMP), (Rectangle)tuple.getValueByField(BOUNDING));
		}
//		frame.getFeatures().addAll((List<Feature>)tuple.getValueByField(FEATURES));
		return frame;
	
	}

	@Override
	protected Values getValues(BaseModel object) throws IOException {
		// TODO Auto-generated method stub
		Frame frame = (Frame)object;
		BufferedImage image = frame.getImageBuf();
		if(image == null){
			return new Values(frame.getImageType(), (Object[])null, frame.getTimeStamp(), frame.getBounding());
		}else{
			return new Values(frame.getImageType(), frame.getImageBytes(), frame.getTimeStamp(), frame.getBounding());
		}
	}

	@Override
	protected List<String> getTypeFields() {
		// TODO Auto-generated method stub
		// be careful! 
		List<String> fields = new ArrayList<String>();
		fields.add(IMAGE_TYPE);
		fields.add(IMAGE_BYTES);
		fields.add(TIME_STAMP);
		fields.add(BOUNDING);
//		fields.add(FEATURES);
		return fields;
	}
}

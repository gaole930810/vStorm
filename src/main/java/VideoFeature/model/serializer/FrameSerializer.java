package VideoFeature.model.serializer;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import VideoFeature.model.BaseModel;
import VideoFeature.model.Feature;
import VideoFeature.model.Frame;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * 单帧序列化器，继承抽象帧序列化器{@link BaseModelSerializer} 
 */
public class FrameSerializer extends BaseModelSerializer<Frame> implements Serializable{

	private static final long serialVersionUID = 1L;
    public final static String IMAGE_TYPE = "IMAGE_TYPE";
    public final static String IMAGE_BYTES = "IMAGE_BYTES";
	public static final String TIME_STAMP = "TIME_STAMP";
    public final static String BOUNDING = "BOUNDING";
    public final static String OVERLAP_PIXEL = "OVERLAP_PIXEL";
    public final static String FLAG = "FLAG";
    public static final String FEATURE = "FEATURE";
    
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
		output.writeInt(frame.getOverlapPixel());
		output.writeShort(frame.getFlag());
		kryo.writeObject(output, frame.getFeature());

		
	}
	/*
	 * deserialized
	 */
	@SuppressWarnings("unchecked")
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
		
		int overlapPixel = input.readInt();
		short flag = input.readShort();
//		List<Feature> features = kryo.readObject(input, ArrayList.class);
		Feature feature = kryo.readObject(input, Feature.class);
		return new Frame(streamId, sequenceNr, imageType, buffer, timeStamp, boundingBox ,feature).OverlapPixel(overlapPixel).Flag(flag);
	
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Frame createObject(Tuple tuple) throws IOException {
		// TODO Auto-generated method stub

		byte[] buffer = tuple.getBinaryByField(IMAGE_BYTES);
		Frame frame;
		if(buffer == null){
			frame = new Frame(tuple, tuple.getStringByField(IMAGE_TYPE), null, tuple.getLongByField(TIME_STAMP), (Rectangle)tuple.getValueByField(BOUNDING))
							.OverlapPixel(tuple.getIntegerByField(OVERLAP_PIXEL))
							.Flag(tuple.getShortByField(FLAG));
		}else{
			frame = new Frame(tuple, tuple.getStringByField(IMAGE_TYPE), buffer, tuple.getLongByField(TIME_STAMP), (Rectangle)tuple.getValueByField(BOUNDING))
							.OverlapPixel(tuple.getIntegerByField(OVERLAP_PIXEL))
							.Flag(tuple.getShortByField(FLAG));
		}
		Feature feature = (Feature)tuple.getValueByField(FEATURE);
		if(feature == null && frame.getFlag() == 6){
//			System.out.println(" - "+frame.getSeqNumber()+" - "+tuple);//--null!!!!!!!
		}
//		if(list.size() == 1)
//			System.out.println(list.size()+" - "+frame.getSeqNumber()+" - "+tuple);//--null!!!!!!!
//		frame.getFeatures().addAll((List<Feature>)tuple.getValueByField(FEATURES));
		frame.setFeature(feature);
		return frame;
	
	}

	@Override
	protected Values getValues(BaseModel object) throws IOException {
		// TODO Auto-generated method stub
		Frame frame = (Frame)object;
		BufferedImage image = frame.getImageBuf();
		if(frame.getFeature() == null && frame.getFlag() == 6){
			System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~frame feature is null..."+frame.getSeqNumber());
		}
		if(image == null){
			return new Values(frame.getImageType(), (Object[])null, frame.getTimeStamp(), frame.getBounding(), frame.getOverlapPixel() ,frame.getFlag(),frame.getFeature());
		}else{
			return new Values(frame.getImageType(), frame.getImageBytes(), frame.getTimeStamp(), frame.getBounding(), frame.getOverlapPixel() , frame.getFlag(),frame.getFeature());
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
		fields.add(OVERLAP_PIXEL);
		fields.add(FLAG);
		fields.add(FEATURE);
		return fields;
	}
}

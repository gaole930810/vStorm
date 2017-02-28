package VideoFeature.model.serializer;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import VideoFeature.model.BaseModel;
import VideoFeature.model.Descriptor;
import VideoFeature.model.Feature;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
/**
 * 
 * 特征序列化器，继承抽象帧序列化器 {@link BaseModelSerializer}
 */

public class FeatureSerializer extends BaseModelSerializer<Feature> implements Serializable {

	private static final long serialVersionUID = 3121908157999500459L;
	public static final String NAME = "name";
	public static final String DURATION = "duration";
	public static final String SPARSE_DESCR = "sparse";
	public static final String DENSE_DESCR = "dense";
//	public final static String BOUNDING_F = "BOUNDING_F_FEATURE";
	public final static String OVERLAP_PIXEL_F = "OVERLAP_PIXEL_F_FEATURE";
	
	@SuppressWarnings("unchecked")
	@Override
	protected Feature createObject(Tuple tuple) throws IOException {
		List<Descriptor> sparseDescriptors = (List<Descriptor>) tuple.getValueByField(SPARSE_DESCR);
		float[][][] denseDescriptors = (float[][][])tuple.getValueByField(DENSE_DESCR);
		Feature feature = new Feature(tuple, tuple.getStringByField(NAME), tuple.getLongByField(DURATION), sparseDescriptors, denseDescriptors/*,(Rectangle)tuple.getValueByField(BOUNDING_F)*/).OverlapPixel(tuple.getIntegerByField(OVERLAP_PIXEL_F));
		return feature;
	}

	@Override
	protected Values getValues(BaseModel basemodel) throws IOException {
		Feature feature = (Feature)basemodel;
		return new Values(feature.getName(), feature.getDuration(), feature.getSparseDescriptors(), feature.getDenseDescriptors()/*, feature.getBounding()*/, feature.getOverlapPixel());
	}

	@Override
	protected List<String> getTypeFields() {
		List<String> fields = new ArrayList<String>();
		fields.add(NAME);
		fields.add(DURATION);
		fields.add(SPARSE_DESCR);
		fields.add(DENSE_DESCR);
//		fields.add(BOUNDING_F);
		fields.add(OVERLAP_PIXEL_F);
		return fields;
	}
	
	@Override
	protected void writeObject(Kryo kryo, Output output, Feature feature) throws Exception {
		output.writeString(feature.getName());
		output.writeLong(feature.getDuration());
		kryo.writeObject(output, feature.getSparseDescriptors());
		float[][][] m = feature.getDenseDescriptors();
		output.writeInt(m.length); // write x
		if(m.length == 0) return;
		
		output.writeInt(m[0].length); // write y
		output.writeInt(m[0][0].length); // write z
		for(int x=0; x<m.length; x++){
			for(int y=0; y<m[0].length; y++){
				for(int z=0; z<m[0][0].length; z++){
					output.writeFloat(m[x][y][z]);
				}
			}
		}
//		if(feature.getBounding() != null){
//			output.writeFloat((float)feature.getBounding().getX());
//			output.writeFloat((float)feature.getBounding().getY());
//			output.writeFloat((float)feature.getBounding().getWidth());
//			output.writeFloat((float)feature.getBounding().getHeight());
//		}
//		output.writeInt(feature.getOverlapPixel());
		
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Feature readObject(Kryo kryo, Input input, Class<Feature> clas, String streamId, long sequenceNr) throws Exception {
		String name = input.readString();
		long duration = input.readLong();
		List<Descriptor> sparseDescriptors = kryo.readObject(input, ArrayList.class);
		
		int xl = input.readInt();
		float[][][] denseDescriptor = null;
		if(xl > 0){
			int yl = input.readInt();
			int zl = input.readInt();
			denseDescriptor = new float[xl][yl][zl];
			for(int x=0; x<xl; x++){
				for(int y=0; y<yl; y++){
					for(int z=0; z<zl; z++){
						denseDescriptor[x][y][z] = input.readFloat();
					}
				}
			}
		}
//		int x1 = Math.round(input.readFloat());
//		int y1 = Math.round(input.readFloat());
//		int x2 = Math.round(input.readFloat());
//		int y2 = Math.round(input.readFloat());
//		
//		Rectangle boundingBox = new Rectangle(x1,y1,x2,y2);
//		int overlapPixel = input.readInt();
		int overlapPixel =0;
		Feature feature = new Feature(streamId, sequenceNr, name, duration, sparseDescriptors, denseDescriptor/*,boundingBox*/).OverlapPixel(overlapPixel);
		return feature;
	}

}

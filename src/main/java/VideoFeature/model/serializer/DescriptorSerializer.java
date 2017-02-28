package VideoFeature.model.serializer;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.awt.Rectangle;

import VideoFeature.model.BaseModel;
import VideoFeature.model.Descriptor;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
/**
 * 特征描述符序列化器，继承抽象帧序列化器 {@link BaseModelSerializer}
 */
public class DescriptorSerializer extends BaseModelSerializer<Descriptor> implements Serializable {

	private static final long serialVersionUID = 9035480740735532324L;
	public static final String BOUNDINGBOX = FrameSerializer.BOUNDING;
	public static final String DURATION = FeatureSerializer.DURATION;
	public static final String VALUES = "values";
	
	
	@Override
	protected Descriptor createObject(Tuple tuple) throws IOException {
		return new Descriptor(tuple, (Rectangle)tuple.getValueByField(BOUNDINGBOX), (Long)tuple.getValueByField(DURATION), (float[])tuple.getValueByField(VALUES));
	}

	@Override
	protected Values getValues(BaseModel particle) throws IOException {
		Descriptor descriptor = (Descriptor) particle;
		return new Values(descriptor.getBounding(), descriptor.getDuration(), descriptor.getValues());
	}

	@Override
	protected List<String> getTypeFields() {
		List<String> fields = new ArrayList<String>();
		fields.add(BOUNDINGBOX);
		fields.add(DURATION);
		fields.add(VALUES);
		return fields;
	}

	@Override
	protected void writeObject(Kryo kryo, Output output, Descriptor descriptor) {
		output.writeFloat((float)descriptor.getBounding().getX());
		output.writeFloat((float)descriptor.getBounding().getY());
		output.writeFloat((float)descriptor.getBounding().getWidth());
		output.writeFloat((float)descriptor.getBounding().getHeight());
		
		output.writeLong(descriptor.getDuration());
		
		output.writeInt(descriptor.getValues().length);
		for(Float f : descriptor.getValues()){
			output.writeFloat(f);
		}
		
	}

	@Override
	protected Descriptor readObject(Kryo kryo, Input input, Class<Descriptor> clas, String streamId, long sequenceNr) {
		Rectangle rectangle = new Rectangle(Math.round(input.readFloat()), Math.round(input.readFloat()), 
				Math.round(input.readFloat()), Math.round(input.readFloat()));
		long duration = input.readLong();
		int length = input.readInt();
		float[] values = new float[length];
		for(int i=0; i<length; i++){
			values[i] = input.readFloat();
		}
		return new Descriptor(streamId, sequenceNr, rectangle, duration, values);
	}

}

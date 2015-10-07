package VideoFeature.operation;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import VideoFeature.model.BaseModel;
import VideoFeature.model.Frame;
import VideoFeature.model.serializer.BaseModelSerializer;
import VideoFeature.model.serializer.FrameSerializer;
import VideoFeature.utils.ImageUtils;
import backtype.storm.task.TopologyContext;


/**
 * 把一帧分割成若干个部分再分发出去
 * 
 * @author liangzhaohao
 *
 */
public class PartitionOp implements ISingleOperation<Frame> {

	private static final long serialVersionUID = 1235734323465856261L;
	private FrameSerializer serialzier = new FrameSerializer();
	private int rows = 2;			// 行分割数
	private int cols = 2;			// 列分割数
	private int nOctaves = 3;       // 高斯金字塔组数，opencv库用的是多少还需要调查一下，int nOctaves = cvRound(log( (double)std::min( base.cols, base.rows ) ) / log(2.) - 2);
	private int blockingBaseLength = 1; // 数据分块的高度和宽度必须为此数的整数倍，为了防止下采样的误差,详见参考资料【1】
	private int pixelOverlap = 0;	// 重叠区宽度
	private String imageType;
	
	public PartitionOp(int rows, int cols){
		this.rows = rows;
		this.cols = cols;
		nOctaves = (int) Math.round( Math.log(2* (double)Math.min(rows, cols))/Math.log(2.) -2 );
		for(int i=0;i<nOctaves-1;++i)
			blockingBaseLength *= 2; 
	}
	
	public PartitionOp overlap(int pixels){
		this.pixelOverlap = pixels;
		return this;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map conf, TopologyContext context) throws Exception {
		if(conf.containsKey("FreamEncoding")){
			imageType = (String)conf.get("FreamEncoding");
		}
	}

	@Override
	public void deactivate() {	}

	@Override
	public BaseModelSerializer<Frame> getSerializer() {
		return serialzier;
	}

	@Override
	public List<Frame> execute(BaseModel baseModel) throws Exception {
		List<Frame> result = new ArrayList<Frame>();
		if(!(baseModel instanceof Frame)) return result;
		
		Frame sf = (Frame) baseModel;
		BufferedImage image = sf.getImageBuf();
		if(image == null) return result;
		if(image.getWidth()<2*cols || image.getHeight()<2*rows) return result;
		/*
		* 高度受限，每个分块必须是blockingBaseLength的整数倍，除了每行和每列的最后一列
		*/
		int Ru = image.getHeight()/blockingBaseLength;
		int Cu = image.getWidth()/blockingBaseLength;
		int width = (Cu / cols +1)* blockingBaseLength;
		int height= (Ru / rows +1)* blockingBaseLength;
		
		int tileIndex = 0;
		for(int r=0; r<rows; r++){
			for(int c=0; c<cols; c++){
				Rectangle box = new Rectangle(c*width, r*height, width + pixelOverlap, height + pixelOverlap);
				box = box.intersection(sf.getBounding());
				BufferedImage tile = image.getSubimage(box.x, box.y, box.width, box.height);
				byte[] buffer = ImageUtils.imageToBytes(tile, imageType);
				result.add(new Frame(sf.getStreamId()+"_"+tileIndex, sf.getSeqNumber(), imageType, buffer, sf.getTimeStamp(), box));
				tileIndex++;
			}
		}
		return result;
	}
}

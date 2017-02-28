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
 *
 */
public class PartitionOp implements ISingleOperation<Frame> {

	private static final long serialVersionUID = 1235734323465856261L;
	private FrameSerializer serialzier = new FrameSerializer();
	private int rows = 2;			// 行分割数
	private int cols = 2;			// 列分割数
	private int nOctaves = -1;       // 高斯金字塔组数，opencv库用的是多少还需要调查一下，int nOctaves = cvRound(log( (double)std::min( base.cols, base.rows ) ) / log(2.) - 2);
	private int blockingBaseLength = 1; // 数据分块的高度和宽度必须为此数的整数倍，为了防止下采样的误差,详见参考资料【1】
	private int pixelOverlap = 0;	// 重叠区宽度
	private String imageType;
	
	public PartitionOp(int rows, int cols){
		this.rows = rows;
		this.cols = cols;

	}
	
//	public PartitionOp overlap(int pixels){
//		this.pixelOverlap = pixels;
//		return this;
//	}
	
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
		if(nOctaves == -1) initializeArgs(sf.getBounding().width,sf.getBounding().height);

		BufferedImage image = sf.getImageBuf();
		if(image == null) return result;
		if(image.getWidth()<2*cols || image.getHeight()<2*rows) return result;
		/*
		* 高度宽度受限划分法，每个分块必须是blockingBaseLength的整数倍，除了每行和每列的最后一列
		*/
		int Ru = image.getHeight()/blockingBaseLength;
		int Cu = image.getWidth()/blockingBaseLength;
		int width = (Cu / cols +1)* blockingBaseLength;
		int height= (Ru / rows +1)* blockingBaseLength;
		
		int tileIndex = 0;	//序号按行划分
		
		for(int r=0; r<rows; ++r){
			for(int c=0; c<cols; ++c){
				int realX = Math.max(0, c*width - pixelOverlap);
				int realY = Math.max(0, r*height - pixelOverlap);
				Rectangle box = new Rectangle(realX, realY, width + pixelOverlap, height + pixelOverlap);
				box = box.intersection(sf.getBounding());
				BufferedImage tile = image.getSubimage(box.x, box.y, box.width, box.height);
				byte[] buffer = ImageUtils.imageToBytes(tile, imageType);
				result.add(new Frame(sf.getStreamId()+"_"+tileIndex, sf.getSeqNumber(), imageType, buffer, sf.getTimeStamp(), box));//分块的streamId为原streamId加上分块序号
				tileIndex++;
			}
		}
		return result;
	}
	
	/**
	 * 根据帧大小初始化分割参数
	 * @param rows 帧宽度
	 * @param cols 帧高度
	 */
	private void initializeArgs(int rows,int cols){
		nOctaves = (int) Math.round( Math.log(2* (double)Math.min(rows, cols))/Math.log(2.) -2 );
		for(int i=0;i<nOctaves-1;++i)
			blockingBaseLength *= 2; 
		pixelOverlap = blockingBaseLength;
	}
}

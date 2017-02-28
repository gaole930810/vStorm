package VideoFeature.operation;

import java.awt.Rectangle;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import VideoFeature.model.BaseModel;
import VideoFeature.model.Descriptor;
import VideoFeature.model.Feature;
import VideoFeature.model.Frame;
import VideoFeature.model.serializer.BaseModelSerializer;
import VideoFeature.model.serializer.FeatureSerializer;
import backtype.storm.task.TopologyContext;
import backtype.storm.utils.Utils;


/**
 * 数据库存储接口，输入为单帧特征 {@link ISingleOperation}
 * 
 */

public class MysqlOp implements ISingleOperation<Feature>{
	
	private static final long serialVersionUID = -5543735411296339252L;
	private String name;
	private Connection conn = null;
	private String form = "storm_test_sift_features";//表
	
	public MysqlOp(String name){
        this.name = name;
	}

	
	@Override
	public List<Feature> execute(BaseModel input) throws Exception 
	{
		Feature feature = (Feature)input;
		System.out.println("SIFT Storage"+"_"+feature.getSeqNumber());
		InsertDB(feature);
		return null;
	}

	@Override
	public void deactivate() 
	{
		
	}

	@Override
	public BaseModelSerializer<Feature> getSerializer() 
	{
		return new FeatureSerializer();
	}

	@Override
	public void prepare(Map stormConf, TopologyContext context)
			throws Exception {
		// TODO Auto-generated method stub
        LinkDB();//链接数据库
        Utils.sleep(500);
        
	}
	public void LinkDB(){
		System.out.println("prepare link to mysql!");
		String host_port = "localhost:3306";
		String database = "mysql";
		String username = "root";
		String password = "123456";
		String url = "jdbc:mysql://"+host_port+"/"+database;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(url,username,password);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Can't register driver!"); 
		}catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Can't connect to mysql!"); 
		}
	}

//	private void InsertDB(Feature feature) {
//		// TODO Auto-generated method stub
//		float[] color = feature.getSparseDescriptors().
//		get(0).getValues();
//		String sql = "replace into "+this.form+"(seqNr,desc,f2,f3)values('"+feature.getSeqNumber()+"',"+color[0]+","+color[1]+","+color[2]+")";
//		java.sql.Statement statement;
//		try {
//			statement = conn.createStatement();
//			statement.executeUpdate(sql);
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	private void InsertDB(Feature feature) {
		// TODO Auto-generated method stub
		List<Descriptor> descriptorList = feature.getSparseDescriptors();
		java.sql.Statement statement;
		try {
			statement = conn.createStatement();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
//		StringBuilder sql = new StringBuilder("replace into "+this.form+"(seqNr,descripseId,descripseValues)values('");
		int descrId = 0;
		for(Descriptor descr : descriptorList){
			long seqNr = descr.getSeqNumber();
			++descrId;
			String length = String.valueOf(descr.getValues().length);
			String sql = "replace into "+this.form+"(seqNr,descripseId,descripseValues)values('"+feature.getSeqNumber()+"',"+descrId+","+length+")";
			/*try {
				statement.executeUpdate(sql);
			} catch (SQLException e) {
				System.out.println("insert error!");
				continue;
			}*/
		}
		System.out.println("finish insert a feature.descript number "+descrId);

	}
}

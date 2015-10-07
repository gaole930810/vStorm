package VideoFeature.Topology;

import VideoFeature.bolt.SingleOpBolt;
import VideoFeature.fetcher.FileFrameFetcher;
import VideoFeature.model.Descriptor;
import VideoFeature.model.Feature;
import VideoFeature.model.Frame;
import VideoFeature.model.serializer.DescriptorSerializer;
import VideoFeature.model.serializer.FeatureSerializer;
import VideoFeature.model.serializer.FrameSerializer;
import VideoFeature.operation.ColorHistogramOp;
import VideoFeature.operation.FeatureMatchOp;
import VideoFeature.operation.MysqlOp;
import VideoFeature.operation.SIFTExtractionOp;

import VideoFeature.spout.VideoStreamSpout;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import backtype.storm.utils.Utils;


import org.opencv.core.Core;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;

/**
 * 提取特征Topology
 * 
 * @author liangzhaohao on 15/4/16.
 */
public class FeatureExtrationTopology {

    public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);		//加载Opencv库
        
        TopologyBuilder builder = new TopologyBuilder();
        
        String resourcePath = "/home/liang/VideoSource/";	//本地资源路径，测试阶段用
        builder.setSpout("frameSpout", new VideoStreamSpout(new FileFrameFetcher(resourcePath+"FaceSource/targetvideo2.mp4")), 1);  //从文件中读取
//        builder.setSpout("frameSpout", new VideoStreamSpout(
//        		new FileFrameFetcher("rtmp://live.hkstv.hk.lxdns.com/live/hks"))
//        		, 1);//从流中读取，并行度不能设置大于2,否则会重复读取
//      builder.setBolt("featureExtrationBolt", new SingleOpBolt(new ColorHistogramOp("colorFeatures")))	
//        .shuffleGrouping("frameSpout"); 					//提取颜色特征-颜色直方图
        
		//添加bolt提取SIFT特征
		builder.setBolt("featureExtrationBolt", new SingleOpBolt(
				new SIFTExtractionOp("sift", FeatureDetector.SIFT, DescriptorExtractor.SIFT).outputFrame(false))
				, 6)
			.setNumTasks(6)
			.shuffleGrouping("frameSpout");
		
//        builder.setBolt("mysqlBolt", new SingleOpBolt(new MysqlOp("mysqlop")),2)
//        	.setNumTasks(2)
//        	.shuffleGrouping("featureExtrationBolt");			//存储在数据库中
        
		
		builder.setBolt("matchBolt", new SingleOpBolt( 			//匹配Bolt
					new FeatureMatchOp("siftMatch", FeatureDetector.SIFT, DescriptorExtractor.SIFT,resourcePath+"FaceSource/target.jpg")
					)
				, 1).shuffleGrouping("featureExtrationBolt");
		
        Config conf = new Config();
        conf.put("video.name", "768x576.avi");
        conf.put("FreamEncoding", Frame.JPG_IMAGE);
        conf.setDebug(false);
        conf.setNumWorkers(4);
        conf.registerSerialization(Frame.class, FrameSerializer.class);		//注册单帧序列化器
        conf.registerSerialization(Feature.class, FeatureSerializer.class); //注册特征序列化器
        conf.registerSerialization(Descriptor.class, DescriptorSerializer.class);//特征描述符序列化器

        if(args != null && args.length > 0){
            conf.setNumWorkers(Integer.valueOf(args[2]));
            StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
        } else {
            LocalCluster cluster = new LocalCluster();		//本地模式
            cluster.submitTopology("test1", conf, builder.createTopology());
            Utils.sleep(100000);
            cluster.killTopology("test1");
            cluster.shutdown();
        }
    }
}

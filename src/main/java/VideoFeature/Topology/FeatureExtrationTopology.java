package VideoFeature.Topology;

import java.util.ArrayList;
import java.util.List;

import VideoFeature.Batcher.SeqNumberBatcher;
import VideoFeature.bolt.BatchOpBolt;
import VideoFeature.bolt.SingleOpBolt;
import VideoFeature.fetcher.FileFrameFetcher;
import VideoFeature.model.Descriptor;
import VideoFeature.model.Feature;
import VideoFeature.model.Frame;
import VideoFeature.model.GroupOfFrames;
import VideoFeature.model.serializer.DescriptorSerializer;
import VideoFeature.model.serializer.FeatureSerializer;
import VideoFeature.model.serializer.FrameSerializer;
import VideoFeature.model.serializer.GroupOfFramesSerializer;
import VideoFeature.operation.ColorHistogramOp;
import VideoFeature.operation.FeatureMatchOp;
import VideoFeature.operation.FramesToVideoOp;
import VideoFeature.operation.MatchStreamingOp;
import VideoFeature.operation.MysqlOp;
import VideoFeature.operation.PartitionCombinerOp;
import VideoFeature.operation.SIFTExtractionOp;

import VideoFeature.spout.VideoStreamSpout;
import VideoFeature.utils.FtpConnection;
import VideoFeature.utils.LocalFileConnection;
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
 */
public class FeatureExtrationTopology {

    public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException {

    	if(args.length <5){
    		System.out.println("too few arguments");
    		return ;
    	}
    	
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);		//加载Opencv库
        
        TopologyBuilder builder = new TopologyBuilder();
        String topologyName = args[0];			
        String streamURL = args[1];				//输入视频流
        String targetResourcePath = args[2];	//目标图像
        Integer numWorker = Integer.valueOf(args[3]);	
        Integer numTask   = Integer.valueOf(args[4]);
        
//        String resourcePath = "/home/liang/VideoSource/";	//本地资源路径，测试阶段用
//        builder.setSpout("frameSpout", new VideoStreamSpout(new FileFrameFetcher(resourcePath+"FaceSource/targetvideo2.mp4")), 1);  //从文件中读取
        builder.setSpout("frameSpout", new VideoStreamSpout(
        		new FileFrameFetcher(streamURL/*"rtmp://live.hkstv.hk.lxdns.com/live/hks"*/))
        		, 1);//从流中读取，并行度不能设置大于2,否则会重复读取
//      builder.setBolt("featureExtrationBolt", new SingleOpBolt(new ColorHistogramOp("colorFeatures")))	
//        .shuffleGrouping("frameSpout"); 					//提取颜色特征-颜色直方图
        
		//添加bolt提取SIFT特征
		builder.setBolt("featureExtrationBolt", new SingleOpBolt(
				new SIFTExtractionOp("sift", FeatureDetector.SIFT, DescriptorExtractor.SIFT,"???").outputFrame(true))
				, 2)
			.setNumTasks(2)
			.shuffleGrouping("frameSpout");
		
//        builder.setBolt("mysqlBolt", new SingleOpBolt(new MysqlOp("mysqlop")),2)
//        	.setNumTasks(2)
//        	.shuffleGrouping("featureExtrationBolt");			//存储在数据库中
        
		
		builder.setBolt("matchBolt", new SingleOpBolt( 			//匹配Bolt
					new FeatureMatchOp("siftMatch", FeatureDetector.SIFT, DescriptorExtractor.SIFT,targetResourcePath).outputFrame(true)
					)
				, 2)
				.setNumTasks(2)
				.shuffleGrouping("featureExtrationBolt");
		
		
//		builder.setBolt("restServiceBolt", new SingleOpBolt( 	//匹配结果显示在网页
//				new MatchStreamingOp().port(8085)
//				)
//			, 1).shuffleGrouping("matchBolt");
//		
		
//		builder.setBolt("framesToVideo", new BatchOpBolt( new SeqNumberBatcher(2) ,new FramesToVideoOp("file:///home/liang/VideoSource/",240) )
//						.ttl(59)
////						.maxCacheSize(512)
//						.groupBy(new Fields(FrameSerializer.STREAMID))
//						, 1)
//						.setNumTasks(1)
//						.shuffleGrouping("matchBolt");
		
		
		List<String> connections = new ArrayList<String>();
		connections.add(LocalFileConnection.class.getName());	//本地文件连接池
		connections.add(FtpConnection.class.getName());			//Ftp文件连接池
        Config conf = new Config();
        
        conf.put(Config.TOPOLOGY_RECEIVER_BUFFER_SIZE, 32); // sets the maximum number of messages to batch before sending them to executers
		conf.put(Config.TOPOLOGY_TRANSFER_BUFFER_SIZE, 32); // sets the size of the output queue for each worker.
        conf.put("video.name", "768x576.avi");
        conf.put("FreamEncoding", Frame.JPG_IMAGE);
        conf.put("vstorm.connections", connections);
        conf.setDebug(false);
        conf.setNumWorkers(2);
        conf.setMaxTaskParallelism(6); 
    	conf.put(Config.TOPOLOGY_ENABLE_MESSAGE_TIMEOUTS, false); // True if Storm should timeout messages or not.
		conf.put(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS , 5000); // The maximum amount of time given to the topology to fully process a message emitted by a spout (default = 30)
		 conf.registerSerialization(GroupOfFrames.class, GroupOfFramesSerializer.class);//注册多帧序列化器
		conf.registerSerialization(Frame.class, FrameSerializer.class);		//注册单帧序列化器
        conf.registerSerialization(Feature.class, FeatureSerializer.class); //注册特征序列化器
        conf.registerSerialization(Descriptor.class, DescriptorSerializer.class);//特征描述符序列化器

        if(!topologyName.endsWith("Local")){
            conf.setNumWorkers(numWorker);
            StormSubmitter.submitTopology(topologyName, conf, builder.createTopology());
        } else {
            LocalCluster cluster = new LocalCluster();		//本地模式
            cluster.submitTopology("test1", conf, builder.createTopology());
            Utils.sleep(100000);
            cluster.killTopology("test1");
            cluster.shutdown();
        }
    }
}

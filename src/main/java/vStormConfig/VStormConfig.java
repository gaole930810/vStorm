package vStormConfig;


import VideoFeature.model.Frame;
import VideoFeature.model.serializer.FrameSerializer;
import backtype.storm.Config;
import backtype.storm.tuple.Tuple;

/**
 * Defines the configuration parameters used by StormCV. It is possible to put other configuration in the StormCVConfig as well
 * (similar to Storm's Config). This class can also be used to register connectors which enable the platform to interact with remote files. 
 * The following connectors are registered by default: {@link LocalFileConnector}, {@link ClasspathConnector}, {@link S3Connector} and {@link FtpConnector}.
 * All references to locations and files require a prefix which is used to select the right {@link FileConnector} to be used:
 * <ul>
 * <li>file:// - is used to point to the local filesystem (typically used in local modes for testing)</li>
 * <li>s3:// - is used to point to an AWS S3 location as follows: S3://bucket/key</li>
 * <li>classpath:// - is used to point to a file on the classpath (can be inside a jar as well)</li>
 * <li>ftp:// - is used to point to a location on a FTP server</li>
 * </ul>
 * 
 *
 */
public class VStormConfig extends Config{

	private static final long serialVersionUID = 6290659199719921212L;

	/**
	 * <b>Boolean (default = false)</b> configuration parameter indicating if the spout must cache emitted tuples so they can be replayed
	 */
	public static final String STORMCV_SPOUT_FAULTTOLERANT = "vstorm.spout.faulttolerant";
	
	/**
	 * <b>String (default = "jpg" (Frame.JPG))</b> configuration parameter setting the image encoding for frames in the topology. It is up to Operation implementations
	 * to read this configuration parameter and use it properly.
	 */
	public static final String STORMCV_FRAME_ENCODING = "vstorm.frame.encoding";
	
	/**
	 * <b>Integer (default = 30)</b> configuration parameter setting the maximum time to live for items being cached within the topology (both spouts and bolts use this configuration)
	 */
	public static final String STORMCV_CACHES_TIMEOUT_SEC = "vstorm.caches.timeout";
	
	/**
	 * <b>Integer (default = 500)</b> configuration parameter setting the maximum number of elements being cached by spouts and bolts (used to avoid memory overload) 
	 */
	public static final String STORMCV_CACHES_MAX_SIZE = "vstorm.caches.maxsize";
	
	/**
	 * <b>List<Class) (default = NONE) </b> configuration parameter the available {@link FileConnector} in the topology
	 */
	public static final String STORMCV_CONNECTIONS = "vstorm.connections";
	
	/**
	 * <b>Integer (default = 30)</b> configuration parameter setting the maximum idle time in seconds after which the {@link StreamWriterOperation} will close the file
	 */
	public static final String STORMCV_MAXIDLE_SEC = "vstorm.streamwriter.maxidlesecs";
	
	/**
	 * <b>String</b> configuration parameter setting the library name of the OpenCV lib to be used
	 */
	public static final String STORMCV_OPENCV_LIB = "vstorm.opencv.lib";
	
	public static final String FTP_USERNAME= "stormcv.ftp.username";
	
	/**
	 * Configuration key used to set the FTP password in {@link StormCVConfig}
	 */
	public static final String FTP_PASSWORD = "stormcv.ftp.password";
	/**
	 * Creates a specific Configuration for StormCV.
	 * <ul>
	 * <li>Sets buffer sizes to 2 to optimize for few large size {@link Tuple}s instead of loads of small sized Tuples</li>
	 * <li>Registers known Kryo serializers for the Model. Other serializers can be added using the registerSerialization function.</li>
	 * <li>Registers known {@link FileConnector} implementations. New file connectors can be added through registerConnector</li>
	 * </ul>
	 */
	public VStormConfig(){
		super();
		// ------- Create StormCV specific config -------
		put(Config.TOPOLOGY_RECEIVER_BUFFER_SIZE, 2); // sets the maximum number of messages to batch before sending them to executers
		put(Config.TOPOLOGY_TRANSFER_BUFFER_SIZE, 2); // sets the size of the output queue for each worker.
		put(STORMCV_FRAME_ENCODING, Frame.JPG_IMAGE); // sets the encoding of frames which determines both serialization speed and tuple size
		
		// register the basic set Kryo serializers
		registerSerialization(Frame.class, FrameSerializer.class);

		
		// register FileConnectors
//		ArrayList<String> connectorList = new ArrayList<String>();
//		connectorList.add(LocalFileConnector.class.getName());
//		connectorList.add(S3Connector.class.getName());
//		connectorList.add(FtpConnector.class.getName());
//		connectorList.add(ClasspathConnector.class.getName());
//		put(VStormConfig.STORMCV_CONNECTORS, connectorList);
	}
	
	/**
	 * Registers an {@link FileConnector} class which can be used throughout the topology
	 * @param connectorClass
	 * @return
	 */
//	@SuppressWarnings("unchecked")
//	public VStormConfig registerConnector(Class<? extends FileConnector> connectorClass){
//		((ArrayList<String>)get(VStormConfig.STORMCV_CONNECTORS)).add(connectorClass.getName());
//		return this;
//	}
	
}

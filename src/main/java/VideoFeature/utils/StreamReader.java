package VideoFeature.utils;

import VideoFeature.model.Frame;
import backtype.storm.utils.Utils;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class StreamReader extends MediaListenerAdapter implements Runnable {

    // frames buffer
    private LinkedBlockingQueue<Frame> frameQueue;
    private String imageType = Frame.JPG_IMAGE;
    private String streamLocation;
    private String steamId;
    private long frameSeq;

    private boolean isRunning;
    public StreamReader(LinkedBlockingQueue<Frame> frameQueue, String imageType, String streamLocation) {
        this.frameQueue = frameQueue;
        this.imageType = imageType;
        this.streamLocation = streamLocation;
        this.steamId = "" + this.hashCode(); //steamId
    }

    @Override
    public void onVideoPicture(IVideoPictureEvent event) {
        try {
        	
            BufferedImage frame = event.getImage();
            Long timestamp = event.getTimeStamp(TimeUnit.MILLISECONDS);
            Frame newFrame = new Frame(steamId, frameSeq, frame, imageType, timestamp,new Rectangle(0, 0, frame.getWidth(), frame.getHeight()));
//            //时间戳转化为Sting或Date  
//            SimpleDateFormat format =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
//            String d = format.format(timestamp);  
//            Date date=format.parse(d);  
//            System.out.println("frameSeq : " + frameSeq+",timestamp:"+d);
            frameQueue.put(newFrame);

            // Queue is full -> sleep
            if(frameQueue.size() > 100) {
            	System.out.println(frameQueue.size()+" throttling...");
                Utils.sleep(frameQueue.size()-30);
            }
        } catch (Exception e) {
            System.err.println("Fail to add Frame to frameQueue: " + frameQueue.size() + " size " + e.getMessage());
            e.printStackTrace();
        }
        frameSeq++;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
    	isRunning = true;
        IContainer iContainer = IContainer.make();

        // open up the container
        if(iContainer.open(streamLocation, IContainer.Type.READ, null) < 0) {
            throw new IllegalArgumentException("could not open stream: " + streamLocation);
        }

        int videoStreamId = -1;
        IStreamCoder iStreamCoder = null;

        for(int i = 0; i < iContainer.getNumStreams(); i++) {
            // find the stream object
            IStream stream = iContainer.getStream(i);

            // get the pre-configure decoder that can decode this stream;
            IStreamCoder coder = stream.getStreamCoder();

            if(coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStreamId = i;
                iStreamCoder = coder;
                break;
            }

        }

        if(videoStreamId == -1) {
            throw new RuntimeException("could not find video stream in container: " + streamLocation);
        }

        // init frame sequence
        frameSeq = 0;

        IMediaReader reader = ToolFactory.makeReader(streamLocation);
        reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
        reader.addListener(this);
        IError ierror;
        while(null == (ierror = reader.readPacket()) && isRunning ){
            
        };
        if(ierror.getType() == IError.Type.ERROR_EOF){//遇到文件末尾
        	try {
				Frame endSignal = new Frame(steamId, frameSeq, null, imageType, 0L,new Rectangle(0, 0, 0, 0)).Flag((short) -1);//发送一个特殊帧，表示当前文件读取结束
				frameQueue.put(endSignal);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
    }
    public void stop(){
    	isRunning  = false;
    }
}

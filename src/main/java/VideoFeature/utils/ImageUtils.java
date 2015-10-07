package VideoFeature.utils;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.opencv_core.IplImage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by liangzhaohao on 15/3/24.
 */
public class ImageUtils {

    /**
     * Covert an image to byte[]
     * @param image
     * @param encoding
     * @return
     * @throws java.io.IOException
     */
    public static byte[] imageToBytes(BufferedImage image, String encoding) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, encoding, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Covert byte[] to an image
     * @param bytes
     * @param encoding
     * @return
     * @throws java.io.IOException
     */
    public static BufferedImage bytesToImage(byte[] bytes, String encoding) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        return ImageIO.read(byteArrayInputStream);
    }
    
	/**
	 * Converts the provided byte buffer into an BufferedImage
	 * @param buf byte[] of an image as it would exist on disk
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage bytesToImage(byte[] buf) throws IOException{
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		return ImageIO.read(bais);
	}
    
    public static IplImage BufferedImageToIplimage(BufferedImage image){
    	return IplImage.createFrom(image);
    }
}

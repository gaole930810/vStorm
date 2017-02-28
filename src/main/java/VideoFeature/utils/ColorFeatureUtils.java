package VideoFeature.utils;
import javax.imageio.ImageIO;

import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacpp.opencv_core.IplImage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.bytedeco.javacpp.opencv_core.*;  
import static org.bytedeco.javacpp.opencv_imgproc.*;  
import static org.bytedeco.javacpp.opencv_highgui.*; 

import org.bytedeco.javacpp.opencv_core.CvMat;

	public class ColorFeatureUtils {
	
	public static double h_mean=0;
	public static double s_mean=0;
	public static double v_mean=0; 
	
	public static ColorFeatureUtils getColorFeatureUtil(){
		ColorFeatureUtils util = new ColorFeatureUtils();
		return util;
	}
	
	public static Double[] calHSV(IplImage src){
		

	    Double[] HSV=new Double[9];
//	    IplImage src=cvLoadImage(filename);
	    IplImage Doublesrc = null;
	    IplImage Doublehsv = null;
	    IplImage h_plane = null;
	    IplImage s_plane = null;
	    IplImage v_plane = null;
	    CvSize size = cvGetSize( src );
	    
	  //先将图像转换成Double型的
	    Doublesrc = cvCreateImage( size, IPL_DEPTH_32F, 3 );
	    Doublehsv = cvCreateImage( size, IPL_DEPTH_32F, 3 );
	    h_plane = cvCreateImage( size, IPL_DEPTH_32F, 1 );
	    s_plane = cvCreateImage( size, IPL_DEPTH_32F, 1 );
	    v_plane = cvCreateImage( size, IPL_DEPTH_32F, 1 );
	    
	    cvConvertScale( src, Doublesrc, 1.0/255.0, 0 );//归一化之后方能够显示
	    
	  //将Double型图像 从BGR转换到HSV  如果需要转换到其他的颜色空间 那么改变CV_BGR2HSV即可
	    //cvCvtColor要求两个参数的类型必须完全相同，所以要转为Double型
	    cvCvtColor( Doublesrc, Doublehsv, CV_BGR2HSV );
	    
	    //将三通道图像 分解成3个单通道图像，H对应的通道时0，S、V对应的通道时1和2
	    //cvCvtPixToPlane(picHSV, h_plane, s_plane, v_plane, 0);
	    cvSplit( Doublehsv, h_plane, s_plane, v_plane, null);
	    Double mean[]=calMean(h_plane, s_plane, v_plane);
	    Double variance[]=calVariance(h_plane, s_plane, v_plane);
	    Double skewness[]=calSkewness(h_plane, s_plane, v_plane);
	    for(int i=0;i<3;i++){
		HSV[i]=mean[i];
		HSV[i+3]=variance[i];
		HSV[i+6]=skewness[i];
	    }
	    CvMat hsvMat=cvCreateMat(1, HSV.length,CV_32FC1);
	    CvMat dst=cvCreateMat(1, HSV.length,CV_32FC1);
	    for (int i=0;i<HSV.length;i++){
		hsvMat.put(0, i, HSV[i]);
	    }
	    cvNormalize(hsvMat,dst, 1, 0, CV_C, null );
	    for (int i=0;i<HSV.length;i++){
		HSV[i]=dst.get(0, i);
	    }
		return HSV;
	
	}
	private static Double[] calMean(IplImage h_plane,IplImage s_plane,IplImage v_plane){
		
	    int width=h_plane.width(); 
	    int height=h_plane.height();
	    
	    CvMat hMat = h_plane.asCvMat();
	    for(int i=0;i<height;i++){
		for(int j=0;j<width;j++){
		   // int temp=*((uchar*)(h_plane->imageData+i*h_plane->widthStep+j));
		    Double temp= hMat.get(i,j);
		    h_mean+=temp;
		}
	    }
	    h_mean=h_mean/(width*height);
	    CvMat sMat = s_plane.asCvMat();
	    for(int i=0;i<height;i++){
		for(int j=0;j<width;j++){
		    Double temp=sMat.get(i,j);
		    s_mean+=temp;
		}
	    }
	    s_mean=s_mean/(width*height);
	    CvMat vMat = v_plane.asCvMat();
	    for(int i=0;i<height;i++){
		for(int j=0;j<width;j++){
		    Double temp=vMat.get(i,j);
		    v_mean+=temp;
		}
	    }
	    v_mean=v_mean/(width*height);
	    return new Double[]{h_mean,s_mean,v_mean};
		
	}
	
	private static Double[] calVariance(IplImage h_plane,IplImage s_plane,IplImage v_plane){
	    int width=h_plane.width(); 
	    int height=h_plane.height();

	    double h_variance=0,s_variance=0,v_variance=0;
	    CvMat hMat = h_plane.asCvMat();
	    for(int i=0;i<height;i++){
			for(int j=0;j<width;j++){
			    Double temp= hMat.get(i,j);//h_plane->imageData+i*h_plane->widthStep+j));
			    h_variance+=(temp-h_mean)*(temp-h_mean);
			}
	    }
	    h_variance=(Double) Math.sqrt((Double)(h_variance/(width*height)));
	    
	    CvMat sMat = s_plane.asCvMat();
	    for(int i=0;i<height;i++){
			for(int j=0;j<width;j++){
			    Double temp= sMat.get(i,j);//h_plane->imageData+i*h_plane->widthStep+j));
			    s_variance+=(temp-s_mean)*(temp-s_mean);
			}
	    }
	    s_variance=(Double) Math.sqrt((Double)(s_variance/(width*height)));
	    
	    CvMat vMat = v_plane.asCvMat();
	    for(int i=0;i<height;i++){
			for(int j=0;j<width;j++){
			    Double temp= vMat.get(i,j);//h_plane->imageData+i*h_plane->widthStep+j));
			    v_variance+=(temp-v_mean)*(temp-v_mean);
			}
	    }
	    v_variance=(Double) Math.sqrt((Double)(v_variance/(width*height)));
	    return new Double[]{h_variance,s_variance,v_variance};
	}
	
	private static Double[] calSkewness(IplImage h_plane,IplImage s_plane,IplImage v_plane){
		int width=h_plane.width(); 
		int height=h_plane.height();
		
		double h_skewness=0,s_skewness=0,v_skewness=0;
		CvMat hMat = h_plane.asCvMat();
		for(int i=0;i<height;i++){
		for(int j=0;j<width;j++){
		    Double temp= hMat.get(i,j);//*((uchar*)(h_plane->imageData+i*h_plane->widthStep+j));
		    h_skewness+=Math.pow((Double)(temp-h_mean),3);
		}
		}
		h_skewness=(Double) ((h_skewness>=0)?Math.pow(h_skewness/(width*height),(Double)(1.0/3.0)):-Math.pow(-h_skewness/(width*height),(Double)(1.0/3.0)));
		
		CvMat sMat = s_plane.asCvMat();
		for(int i=0;i<height;i++){
		for(int j=0;j<width;j++){
		    Double temp= sMat.get(i,j);//*((uchar*)(h_plane->imageData+i*h_plane->widthStep+j));
		    s_skewness+=Math.pow((Double)(temp-s_mean),3);
		}
		}
		s_skewness=(Double) ((s_skewness>=0)?Math.pow(s_skewness/(width*height),(Double)(1.0/3.0)):-Math.pow(-s_skewness/(width*height),(Double)(1.0/3.0)));
		
		CvMat vMat = v_plane.asCvMat();
		for(int i=0;i<height;i++){
		for(int j=0;j<width;j++){
		    Double temp= vMat.get(i,j);//*((uchar*)(h_plane->imageData+i*h_plane->widthStep+j));
		    v_skewness+=Math.pow((Double)(temp-v_mean),3);
		}
		}
		v_skewness=(Double) ((v_skewness>=0)?Math.pow(v_skewness/(width*height),(Double)(1.0/3.0)):-Math.pow(-v_skewness/(width*height),(Double)(1.0/3.0)));
		return new Double[]{h_skewness,s_skewness,v_skewness};
	}
	
	public static double[] calColorHistogram(IplImage img) {
//	    	IplImage img=cvLoadImage(filename);//加载图像，图像放在Debug文件夹里，这里是相对路径
			CvMat imgmat = cvCreateMat(img.height(), img.width(), CV_8UC3 );
			cvConvert(img, imgmat);
			double[] colorHistogram = new double[24];
			for (int i = 0; i < img.height(); i++) {
			   for (int j = 0; j < img.width(); j++) {
			       colorHistogram[(int)(imgmat.get(i, j, 0)/32)] ++;
			       colorHistogram[(int)(imgmat.get(i, j, 1)/32+8)] ++;
			       colorHistogram[(int)(imgmat.get(i, j, 2)/32+16)] ++;
			   }
	       }
	       for (int i = 0; i < colorHistogram.length; i++) {
	           colorHistogram[i]=colorHistogram[i]/(img.height()*img.width());
	       }
	        CvMat hsvMat=cvCreateMat(1, colorHistogram.length,CV_32FC1);
		    CvMat dst=cvCreateMat(1, colorHistogram.length,CV_32FC1);
		    for (int i=0;i<colorHistogram.length;i++){
		    	hsvMat.put(0, i, colorHistogram[i]);
		    }
		    cvNormalize(hsvMat,dst, 1, 0, CV_C, null );
		    for (int i=0;i<colorHistogram.length;i++){
		    	colorHistogram[i]=dst.get(0, i);
		    }
		       return colorHistogram;
	   }
	
	
	}
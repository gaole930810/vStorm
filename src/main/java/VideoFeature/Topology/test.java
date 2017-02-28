package VideoFeature.Topology;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import VideoFeature.utils.NativeUtils;


public class test {

    public static void main(String[] args) {

        //InputStream inputStream = NativeUtils.class.getResourceAsStream("/haarcascade_frontalface_alt.xml");
        URL url = NativeUtils.class.getResource("/haarcascade_frontalface_alt.xml");
        System.out.println(url.getProtocol());
        System.out.println(url.getFile());
        //System.out.println(inputStream);
    }
}

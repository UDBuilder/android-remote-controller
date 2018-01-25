package org.udbuilder.remotecontroller.transfer;

/**
 * Created by xiongjianbo on 2018/1/18.
 */

public class Constant {

    public static final int PORT_SEND_DATA = 8008;

    public static final String IP_DEFAULT = "localhost";

    public static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    public static final int FRAME_RATE = 15; // 30 fps
    public static final int IFRAME_INTERVAL = 5; // 2 seconds between I-frames
    public static final int TIMEOUT_US = 10000;

    public static final int VIDEO_BITRATE = 500 * 8 * 1000; // 500Kbps

    public static final int MAX_QUEUE_CAPACITY = 50;

    public static final String BUNDLE_WIDTH = "width";
    public static final String BUNDLE_HEIGHT = "height";

}

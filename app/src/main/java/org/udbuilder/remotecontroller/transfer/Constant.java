package org.udbuilder.remotecontroller.transfer;

/**
 * Created by xiongjianbo on 2018/1/18.
 */

public class Constant {

    public static final int PORT_SEND_DATA = 8009;

    public static final String IP_DEFAULT = "127.0.0.1";

    public static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    public static final int FRAME_RATE = 30; // 30 fps
    public static final int IFRAME_INTERVAL = 2; // 2 seconds between I-frames
    public static final int TIMEOUT_US = 10000;

    public static final int VIDEO_WIDTH = 1280;
    public static final int VIDEO_HEIGHT = 720;
    public static final int VIDEO_DPI = 1;
    public static final int VIDEO_BITRATE = 500000; // 500Kbps

    public static final int MAX_QUEUE_CAPACITY = 50;
}

package org.udbuilder.remotecontroller.transfer;

/**
 * Created by xiongjianbo on 2018/1/26.
 */

public class Event {

    public static final byte EVENT_TYPE_TAP = (byte) 0x01;
    public static final byte EVENT_TYPE_SWIPE = (byte) 0x02;
    private byte type;
    private int pointX;
    private int pointY;
    private int toPointX;
    private int toPointY;

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getPointX() {
        return pointX;
    }

    public void setPointX(int pointX) {
        this.pointX = pointX;
    }

    public int getPointY() {
        return pointY;
    }

    public void setPointY(int pointY) {
        this.pointY = pointY;
    }

    public int getToPointX() {
        return toPointX;
    }

    public void setToPointX(int toPointX) {
        this.toPointX = toPointX;
    }

    public int getToPointY() {
        return toPointY;
    }

    public void setToPointY(int toPointY) {
        this.toPointY = toPointY;
    }

    public String getTypeString() {
        switch (type) {
            case EVENT_TYPE_SWIPE:
                return "EVENT_TYPE_SWIPE";
            case EVENT_TYPE_TAP:
                return "EVENT_TYPE_TAP";
            default:
                return "UNKNOWN_TYPE";
        }
    }

    @Override
    public String toString() {
        switch (type) {
            case EVENT_TYPE_SWIPE:
                return String.format("Event[type:EVENT_TYPE_SWIPE, x:%s, y:%s, toX:%s, toY:%s]",
                        pointX, pointY, toPointX, toPointY);
            case EVENT_TYPE_TAP:
                return String.format("Event[type:EVENT_TYPE_TAP, x:%s, y:%s]", pointX, pointY);
            default:
                return "UNKNOWN_TYPE";
        }
    }
}

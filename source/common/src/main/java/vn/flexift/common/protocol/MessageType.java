package vn.flexift.common.protocol;

/** Mọi loại message của giao thức FFT v1. Tên hằng chính là giá trị trường "t" trên đường truyền. */
public enum MessageType {
    // kênh điều khiển
    HELLO, WELCOME, PING, PONG,
    OFFER, ACCEPT, DECLINE, CHANNELS,
    CHUNK_ACK, CHUNK_NACK,
    PAUSE, RESUME, CANCEL, COMPLETE,
    ERROR, BYE,
    // khung đầu tiên của một kênh dữ liệu
    DATA_OPEN
}

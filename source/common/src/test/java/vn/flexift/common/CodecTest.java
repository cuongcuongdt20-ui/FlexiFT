package vn.flexift.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import vn.flexift.common.codec.ChunkFrame;
import vn.flexift.common.codec.FrameCodec;
import vn.flexift.common.codec.Hashes;
import vn.flexift.common.codec.JsonCodec;
import vn.flexift.common.protocol.Envelope;
import vn.flexift.common.protocol.ErrorCode;
import vn.flexift.common.protocol.MessageType;
import vn.flexift.common.protocol.Payloads;
import vn.flexift.common.protocol.ProtocolException;

class CodecTest {

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void twoFramesStuckTogetherAreReadSeparately() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        FrameCodec.write(out, utf8("mot"));
        FrameCodec.write(out, utf8("hai"));

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        assertArrayEquals(utf8("mot"), FrameCodec.read(in));
        assertArrayEquals(utf8("hai"), FrameCodec.read(in));
    }

    @Test
    void truncatedFrameThrowsEof() {
        byte[] bytes = {0, 0, 0, 10, 65, 66};                 // khai 10 byte, chỉ có 2
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        assertThrows(EOFException.class, () -> FrameCodec.read(in));
    }

    @Test
    void oversizedLengthIsRejected() {
        byte[] bytes = {0x7f, 0, 0, 0};
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        ProtocolException e = assertThrows(ProtocolException.class, () -> FrameCodec.read(in));
        assertEquals(ErrorCode.E_BAD_FRAME, e.code());
    }

    @Test
    void offerSurvivesEncodeAndDecode() throws Exception {
        Payloads.Offer offer = new Payloads.Offer("a41f", "bài giảng.pdf", 73_400_320L, 1 << 20, 70, "9c2e", "AUTO");
        Envelope sent = Envelope.of(MessageType.OFFER, JsonCodec.toData(offer)).withId(12);
        Envelope received = JsonCodec.decode(JsonCodec.encode(sent));

        assertEquals(MessageType.OFFER, received.t());
        assertEquals(12, received.id());
        assertEquals(offer, JsonCodec.fromData(received.d(), Payloads.Offer.class));
    }

    @Test
    void unknownTypeDecodesToNullType() throws Exception {
        Envelope received = JsonCodec.decode(utf8("{\"v\":1,\"t\":\"FLY_TO_MOON\",\"id\":7}"));
        assertNull(received.t());
        assertEquals(7, received.id());
    }

    @Test
    void malformedJsonIsBadFrame() {
        ProtocolException e = assertThrows(ProtocolException.class, () -> JsonCodec.decode(utf8("{not json")));
        assertEquals(ErrorCode.E_BAD_FRAME, e.code());
    }

    @Test
    void chunkFrameRoundTripAndEndMarker() throws Exception {
        byte[] payload = utf8("noi dung khoi so 3");
        byte[] sha = Hashes.sha256(payload, 0, payload.length);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        ChunkFrame.write(out, 3, sha, payload, payload.length);
        ChunkFrame.writeEnd(out);

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        ChunkFrame frame = ChunkFrame.read(in, 1024);
        assertEquals(3, frame.index());
        assertArrayEquals(sha, frame.sha256());
        assertArrayEquals(payload, frame.payload());
        assertNull(ChunkFrame.read(in, 1024));
    }

    @Test
    void chunkLongerThanAgreedSizeIsRejected() throws Exception {
        byte[] payload = new byte[200];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ChunkFrame.write(new DataOutputStream(buffer), 0, new byte[32], payload, payload.length);

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        ProtocolException e = assertThrows(ProtocolException.class, () -> ChunkFrame.read(in, 100));
        assertEquals(ErrorCode.E_BAD_FRAME, e.code());
    }

    @Test
    void transferIdIsStableForTheSameFile() {
        String a = Hashes.transferId("9c2e", 100, "a.txt");
        assertEquals(a, Hashes.transferId("9c2e", 100, "a.txt"));
        assertEquals(16, a.length());
    }
}

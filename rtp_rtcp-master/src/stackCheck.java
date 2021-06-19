import com.biasedbit.efflux.packet.AppDataPacket;
import com.biasedbit.efflux.packet.CompoundControlPacket;
import com.biasedbit.efflux.packet.ControlPacket;
import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.participant.RtpParticipant;
import com.biasedbit.efflux.participant.RtpParticipantInfo;
import com.biasedbit.efflux.session.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.util.HashedWheelTimer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class stackCheck {

    private static final byte N = 3;
    private static MultiParticipantSession[] sessions;

    public static void main(String[] args) {

        sessions = new MultiParticipantSession[N];
        final AtomicInteger[] counters = new AtomicInteger[N];
        final CountDownLatch latch = new CountDownLatch(N);

        for(byte i = 0; i < N; i++)
        {
            final int temp = i;
          //  HashedWheelTimer timer = new HashedWheelTimer(10, TimeUnit.SECONDS);
            RtpParticipant participant = RtpParticipant
                    .createReceiver(new RtpParticipantInfo(i), "127.0.0.1", 10000 + (i * 2), 20001 + (i * 2));
            sessions[i] = new MultiParticipantSession("session" + i, 8, participant);
            sessions[i].init();
            final AtomicInteger counter = new AtomicInteger();
            counters[i] = counter;
            sessions[i].addDataListener(new RtpSessionDataListener() {
                @Override
                public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
                    System.err.println(session.getId() + " received data from " + participant + ": " + packet);
                    if (counter.incrementAndGet() == ((N - 1) * 2)) {
                        latch.countDown();
                    }
                }
            });
            sessions[i].addControlListener(new RtpSessionControlListener() {
                @Override
                public void controlPacketReceived(RtpSession session, CompoundControlPacket packet) {
                        System.err.println("CompoundControlPacket received by session " + temp);
                }
                @Override
                public void appDataReceived(RtpSession session, AppDataPacket appDataPacket) {
                    System.err.println("CompoundControlPacket received by session " + temp);
                }
            });
        }

        for (byte i = 0; i < N; i++) {
            for (byte j = 0; j < N; j++) {
                if (j == i) {
                    continue;
                }
                RtpParticipant participant = RtpParticipant
                        .createReceiver(new RtpParticipantInfo(j), "127.0.0.1", 10000 + (j * 2), 20001 + (j * 2));
                System.err.println("Adding " + participant + " to session " + sessions[i].getId());
                sessions[i].addReceiver(participant);
            }
        }
        byte[] deadbeef = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        DataPacket packet = new DataPacket();
        packet.setData(deadbeef);
        packet.setTimestamp(0x45);
        packet.setMarker(false);
        for (byte i = 0; i < N; i++) {
            packet.setSsrc(i);
            packet.setSequenceNumber(5);
            sessions[i].sendDataPacket(packet);
            packet.setSequenceNumber(12);
            sessions[i].sendDataPacket(packet);
        }

        ControlPacket controlPacket = new ControlPacket(ControlPacket.Type.SOURCE_DESCRIPTION) {
            @Override
            public ChannelBuffer encode(int currentCompoundLength, int fixedBlockSize) {
                return null;
            }

            @Override
            public ChannelBuffer encode() {
                return null;
            }
        };

    }
}

import com.biasedbit.efflux.packet.*;
import com.biasedbit.efflux.participant.RtpParticipant;
import com.biasedbit.efflux.participant.RtpParticipantInfo;
import com.biasedbit.efflux.participant.SsrcGenerator;
import com.biasedbit.efflux.session.MultiParticipantSession;
import com.biasedbit.efflux.session.RtpSession;
import com.biasedbit.efflux.session.RtpSessionControlListener;
import com.biasedbit.efflux.session.RtpSessionDataListener;

public class Client_B {
    public static void main(String[] args) {
        final byte N = 1;
        final MultiParticipantSession[] sessions;
        sessions = new MultiParticipantSession[N];

//        for(byte i = 0; i < N; i++)
//        {
            long ssrc = SsrcGenerator.generateSsrc();
            System.err.println("CLient B ssrc: " + ssrc);
            final RtpParticipant participant = RtpParticipant
                    .createReceiver(new RtpParticipantInfo(ssrc), "127.0.0.1", 20000, 20001);
            sessions[0] = new MultiParticipantSession("Client B" , 8, participant);
            sessions[0].init();
            sessions[0].addDataListener(new RtpSessionDataListener() {
                @Override
                public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
                    System.err.println(session.getId() + " received data from " + participant.getSsrc() + " Data: " + packet);
                }
            });
            sessions[0].addControlListener(new RtpSessionControlListener() {
                @Override
                public void controlPacketReceived(RtpSession session, CompoundControlPacket packet) {

                    System.err.println("CompoundControlPacket received by session " + session.getId());

                    for(ControlPacket pkt: packet.getControlPackets())
                    {
                        if (pkt.getType() == ControlPacket.Type.SENDER_REPORT || pkt.getType() == ControlPacket.Type.RECEIVER_REPORT)
                        {
                            AbstractReportPacket abstractReportPacket = (AbstractReportPacket) pkt;
                            for (ReceptionReport receptionReport : abstractReportPacket.getReceptionReports()) {
                                if (receptionReport.getSsrc() == participant.getSsrc()) {
                                    System.out.println("Ext Highest Seq. No. Recvd: "+receptionReport.getExtendedHighestSequenceNumberReceived() + " from " + abstractReportPacket.getSenderSsrc());
                                    System.out.println("Pkts Recvd: " + receptionReport.getPacketsReceived());
                                }
                            }
                        }
                    }
                }
                @Override
                public void appDataReceived(RtpSession session, AppDataPacket appDataPacket) {
                    System.err.println("CompoundControlPacket received from " + session.getId());
                }
            });
//        }

        RtpParticipant server_participant = RtpParticipant
                .createReceiver(new RtpParticipantInfo(1), "127.0.0.1", 49742, 49743);
        System.err.println("Adding " + server_participant + " to session " + sessions[0].getLocalParticipant().getSsrc() + " as a receiver");
        sessions[0].addReceiver(server_participant);

        //invoke a thread to recieve the packets too


        byte[] deadbeef = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
//        DataPacket packet = new DataPacket();
//        packet.setData(deadbeef);
//        packet.setSequenceNumber(8);
//        sessions[0].sendDataPacket(packet);


        for (long i = 0; i < 1000; i++) {
            DataPacket packet = new DataPacket();
            packet.setData(deadbeef);
            packet.setSequenceNumber(i);
            sessions[0].sendDataPacket(packet);

            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}

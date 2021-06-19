import com.biasedbit.efflux.packet.*;
import com.biasedbit.efflux.participant.RtpParticipant;
import com.biasedbit.efflux.participant.RtpParticipantInfo;
import com.biasedbit.efflux.participant.SsrcGenerator;
import com.biasedbit.efflux.session.MultiParticipantSession;
import com.biasedbit.efflux.session.RtpSession;
import com.biasedbit.efflux.session.RtpSessionControlListener;
import com.biasedbit.efflux.session.RtpSessionDataListener;

public class Client_E
{
    public static void main(String[] args) {
        final byte N = 1;
        final MultiParticipantSession[] sessions;
        sessions = new MultiParticipantSession[N];

//        for(byte i = 0; i < N; i++)
//        {
            long ssrc = SsrcGenerator.generateSsrc();
            System.err.println("CLient E ssrc: " + ssrc);
            final RtpParticipant participant = RtpParticipant
                    .createReceiver(new RtpParticipantInfo(ssrc), "127.0.0.1", 30000, 30001);
            sessions[0] = new MultiParticipantSession("Client E" , 8, participant);
//            sessions[0].init();

            RtpParticipant server_participant = RtpParticipant
                    .createReceiver(new RtpParticipantInfo(300), "127.0.0.1", 58858, 58859);
            System.err.println("Adding " + server_participant + " to session " + sessions[0].getLocalParticipant().getSsrc() + " as a receiver");
            sessions[0].addReceiver(server_participant);
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

                    System.err.println("CompoundControlPacket received" );

                    for(ControlPacket pkt: packet.getControlPackets())
                    {

                        if (pkt.getType() == ControlPacket.Type.SENDER_REPORT || pkt.getType() == ControlPacket.Type.RECEIVER_REPORT)
                        {
                            AbstractReportPacket abstractReportPacket = (AbstractReportPacket) pkt;
                            try {
                                for (ReceptionReport receptionReport : abstractReportPacket.getReceptionReports()) {
                                    if (receptionReport.getSsrc() == participant.getSsrc()) {
                                        System.out.println("Ext Highest Seq. No. Recvd: " + receptionReport.getExtendedHighestSequenceNumberReceived() + " from " + abstractReportPacket.getSenderSsrc());
                                        System.out.println("Pkts Recvd: " + receptionReport.getPacketsReceived());
                                    }
                                }
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
                @Override
                public void appDataReceived(RtpSession session, AppDataPacket appDataPacket) {
                    System.err.println("CompoundControlPacket received from " + session.getId());
                }
            });

            sessions[0].startReceivingPackets();

//        }

//        RtpParticipant participant = RtpParticipant
//                .createReceiver(new RtpParticipantInfo(300), "192.168.43.11", 43042, 52893);
//        System.err.println("Adding " + participant + " to session " + sessions[0].getLocalParticipant().getSsrc() + " as a receiver");
//        sessions[0].addReceiver(participant);


//        byte[] deadbeef = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        byte[] deadbeef = new byte[1600];
        for(int i=0; i<1600; i++) deadbeef[i] = (byte) 0xde;
//        InetAddress address = null;
//        try {
//            address = InetAddress.getByName("192.168.43.11");
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
//        DatagramSocket socket= null;
//        try {
//            socket = new DatagramSocket();
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
long i = 0;
//        for (byte i = 0; i < 1000; i++) {
        while (true) {
            DataPacket packet = new DataPacket();
            packet.setData(deadbeef);
            packet.setSequenceNumber(i);

            try {
//                DatagramPacket pkt = new DatagramPacket(deadbeef, deadbeef.length, address, 34835);
//                socket.send(pkt);
//                System.err.println("sending data " + pkt +" " + i);
                sessions[0].sendDataPacket(packet);
//                System.err.println("sending data " + packet +" " + i);
                Thread.sleep(3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            i++;
        }
    }
}

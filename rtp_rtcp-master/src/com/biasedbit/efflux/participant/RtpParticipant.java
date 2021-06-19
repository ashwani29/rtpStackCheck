/*
 * Copyright 2010 Bruno de Carvalho
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.biasedbit.efflux.participant;

import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.packet.SdesChunk;
import com.biasedbit.efflux.util.TimeUtils;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Integer.valueOf;

/**
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public class RtpParticipant {

    // constants ------------------------------------------------------------------------------------------------------

    private static final int CYCLES = (1<<16);
    private static final int VALID_PACKETS_UNTIL_VALID_PARTICIPANT = 3;

    // configuration --------------------------------------------------------------------------------------------------

    private final RtpParticipantInfo info;

    // internal vars --------------------------------------------------------------------------------------------------

//    private SocketAddress dataDestination;
//    private SocketAddress controlDestination;
    private SocketAddress lastDataOrigin;
    private SocketAddress lastControlOrigin;
    private long lastReceptionInstant;
    private long byeReceptionInstant;
    private int lastSequenceNumber;

    //MY UPDATE
    private int maxSequenceNumber;

    private boolean receivedSdes;
    private final AtomicLong receivedByteCounter;
    private final AtomicLong receivedPacketCounter;
    private final AtomicInteger validPacketCounter;

    //UPDATE
    private int dataPort = 65536;
    private int controlPort = 65536;
    private String host = "";
//    private DatagramSocket dataSocket = null;
//    private DatagramSocket controlSocket = null;
    private SocketAddress dataDestination;
    private SocketAddress controlDestination;
//    private static int numParticipants = 0;

    // constructors ---------------------------------------------------------------------------------------------------

    private RtpParticipant(RtpParticipantInfo info) {
        // For internal use only.
        this.info = info;

        //MY UPDATE
        this.maxSequenceNumber = -1;

        this.lastSequenceNumber = -1;
        this.lastReceptionInstant = 0;
        this.byeReceptionInstant = 0;

        this.receivedByteCounter = new AtomicLong();
        this.receivedPacketCounter = new AtomicLong();
        this.validPacketCounter = new AtomicInteger();
    }

    // public static methods ------------------------------------------------------------------------------------------

    public static RtpParticipant createReceiver(String host, int dataPort, int controlPort) {
        RtpParticipant participant = new RtpParticipant(new RtpParticipantInfo());

        if ((dataPort < 0) || (dataPort > 65536)) {
            throw new IllegalArgumentException("Invalid port number; use range [0;65536]");
        }
        if ((controlPort < 0) || (controlPort > 65536)) {
            throw new IllegalArgumentException("Invalid port number; use range [0;65536]");
        }

        //UPDATE
        participant.dataDestination = new InetSocketAddress(host, dataPort);
        participant.controlDestination = new InetSocketAddress(host, controlPort);
        participant.dataPort = dataPort;
        participant.controlPort = controlPort;

        return participant;
    }

    public static RtpParticipant createReceiver(RtpParticipantInfo info, String host, int dataPort, int controlPort) {
        RtpParticipant participant = new RtpParticipant(info);

        if ((dataPort < 0) || (dataPort > 65536)) {
            throw new IllegalArgumentException("Invalid port number; use range [0;65536]");
        }
        if ((controlPort < 0) || (controlPort > 65536)) {
            throw new IllegalArgumentException("Invalid port number; use range [0;65536]");
        }

        try {
            participant.dataDestination = new InetSocketAddress(host, dataPort);
            participant.controlDestination = new InetSocketAddress(host, controlPort);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        //UPDATE
        participant.host = host;
        participant.dataPort = dataPort;
        participant.controlPort = controlPort;

//        if(participant.numParticipants == 0){
//            try {
//                participant.dataSocket = new DatagramSocket(dataPort);
//                participant.controlSocket = new DatagramSocket(controlPort);
//            } catch (SocketException e) {
//                e.printStackTrace();
//            }
//        }
//
//        numParticipants = numParticipants + 1;

        return participant;
    }

    public static RtpParticipant createFromUnexpectedDataPacket(SocketAddress origin, DataPacket packet) {
        RtpParticipant participant = new RtpParticipant(new RtpParticipantInfo());
        participant.lastDataOrigin = origin;
        participant.getInfo().setSsrc(packet.getSsrc());

        return participant;
    }

    public static RtpParticipant createFromSdesChunk(SocketAddress origin, SdesChunk chunk) {
        RtpParticipant participant = new RtpParticipant(new RtpParticipantInfo());
        participant.lastControlOrigin = origin;
        participant.getInfo().updateFromSdesChunk(chunk);
        participant.receivedSdes();

        return participant;
    }

    // public methods -------------------------------------------------------------------------------------------------

    public long resolveSsrcConflict(long ssrcToAvoid) {
        // Will hardly ever loop more than once...
        while (this.getSsrc() == ssrcToAvoid) {
            this.getInfo().setSsrc(RtpParticipantInfo.generateNewSsrc());
        }

        return this.getSsrc();
    }

    public long resolveSsrcConflict(Collection<Long> ssrcsToAvoid) {
        // Probability to execute more than once is higher than the other method that takes just a long as parameter,
        // but its still incredibly low: for 1000 participants, there's roughly 2*10^-7 chance of collision
        while (ssrcsToAvoid.contains(this.getSsrc())) {
            this.getInfo().setSsrc(RtpParticipantInfo.generateNewSsrc());
        }

        return this.getSsrc();
    }


    public void byeReceived() {
        this.byeReceptionInstant = TimeUtils.now();
    }

    public void receivedSdes() {
        this.receivedSdes = true;
    }

    public void packetReceived() {
        this.lastReceptionInstant = TimeUtils.now();
    }
//UPDATE
    public boolean isReceiver() {
        return (this.dataDestination != null) && (this.controlDestination != null);
    }
//    public boolean isReceiver(){
//        return (this.dataPort != 65536) && (this.controlPort != 65536);
//    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public long getSsrc() {
        return this.getInfo().getSsrc();
    }

    public RtpParticipantInfo getInfo() {
        return info;
    }

    public long getLastReceptionInstant() {
        return lastReceptionInstant;
    }

    public long getByeReceptionInstant() {
        return byeReceptionInstant;
    }

    public int getLastSequenceNumber() {
        return lastSequenceNumber;
    }

    //MY UPDATE
    public int getMaxSequenceNumber() { return maxSequenceNumber; }

    public void setMaxSequenceNumber(int maxSequenceNumber) { this.maxSequenceNumber = maxSequenceNumber; }


    public void setLastSequenceNumber(int lastSequenceNumber) {
        this.lastSequenceNumber = lastSequenceNumber;
    }

    public boolean receivedBye() {
        return this.byeReceptionInstant > 0;
    }

    public long getReceivedPackets() {
        return this.receivedPacketCounter.get();
    }
    //using below methods to change the received packets in one go.
    public long getAndSetReceivedPackets() {
        long pkts =  this.receivedPacketCounter.get();
        this.receivedPacketCounter.set(0);
        return pkts;
    }
    public void setReceivedPackets(long receivedPackets){ this.receivedPacketCounter.set(receivedPackets);}     // this function is used to reset the receivedPacketCounter in every RTCP transmission interval.

    // MY UPDATE
    public long incrementReceivedPackets(){
        return this.receivedPacketCounter.incrementAndGet();
    }

    // MY UPDATE
    public long incrementReceivedPacketsBytes(int delta)
    {
        if (delta < 0)
        {
            return this.receivedByteCounter.get();
        }
            return this.receivedByteCounter.getAndAdd(delta);
    }

    public long getReceivedBytes() {
        return this.receivedByteCounter.get();
    }

    public boolean hasReceivedSdes() {
        return receivedSdes;
    }

    //UPDATE
    public SocketAddress getDataDestination() {
        return dataDestination;
    }
    public int getDataPort(){
        return dataPort;
    }
//UPDATE
    public void setDataDestination(SocketAddress dataDestination) {
        if (dataDestination == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }
        this.dataDestination = dataDestination;
    }
    public void setDataPort(int dataPort) {
        if (dataPort == 65536) {
            throw new IllegalArgumentException("Argument cannot be 65536");
        }
        this.dataPort = dataPort;
    }
//UPDATE
    public SocketAddress getControlDestination() {
        return controlDestination;
    }
    public int getControlPort() {
        return controlPort;
    }

    //UPDATE
//    public void setControlDestination(SocketAddress controlDestination) {
//        if (dataDestination == null) {
//            throw new IllegalArgumentException("Argument cannot be null");
//        }
//        this.controlDestination = controlDestination;
//    }
    public void setControlDestination(int controlPort) {
        if (controlPort == 65536) {
            throw new IllegalArgumentException("Argument cannot be 65536");
        }
        this.controlPort = controlPort;
    }
//UPDATE
    public String getHost(){
        return host;
    }
//UPDATE
//    public DatagramSocket getDataSocket(){
//        return dataSocket;
//    }
//    public DatagramSocket getControlSocket(){
//        return controlSocket;
//    }

    public SocketAddress getLastDataOrigin() {
        return lastDataOrigin;
    }

    public void setLastDataOrigin(SocketAddress lastDataOrigin) {
        this.lastDataOrigin = lastDataOrigin;
    }

    public SocketAddress getLastControlOrigin() {
        return lastControlOrigin;
    }

    public void setLastControlOrigin(SocketAddress lastControlOrigin) {
        this.lastControlOrigin = lastControlOrigin;
    }

    // low level overrides --------------------------------------------------------------------------------------------

    //UPDATE
    @Override
//    public boolean equals(Object o) {
//        if (this == o) {
//            return true;
//        }
//        if (!(o instanceof RtpParticipant)) {
//            return false;
//        }
//
//        RtpParticipant that = (RtpParticipant) o;
//        return this.controlDestination.equals(that.controlDestination) &&
//               this.dataDestination.equals(that.dataDestination) &&
//               this.info.getCname().equals(that.info.getCname());
//    }
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RtpParticipant)) {
            return false;
        }

        RtpParticipant that = (RtpParticipant) o;
        return this.controlPort == (that.controlPort) &&
                this.dataPort == (that.dataPort) &&
                this.info.getCname().equals(that.info.getCname());
    }

    //UPDATE
    @Override
//    public int hashCode() {
//        int result = dataDestination.hashCode();
//        result = 31 * result + controlDestination.hashCode();
//        return result;
//    }
    public int hashCode() {
        Integer dp = valueOf(dataPort);
        int result = dp.hashCode();
        Integer cp = valueOf(controlPort);
        result = 31 * result + cp.hashCode();
        return result;
    }
    @Override
    public String toString() {
        return this.getInfo().toString();
    }
}

/*
 * Copyright (c) 2017.  Richard Scott McNew.
 *
 * This file is part of Liquid Fortress Packet Analyzer.
 *
 * Liquid Fortress Packet Analyzer is free software: you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Liquid Fortress Packet Analyzer is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Liquid Fortress Packet Analyzer.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.liquidfortress.packetanalyzer.tcp;

import com.liquidfortress.packetanalyzer.main.Main;
import com.liquidfortress.packetanalyzer.main.Mode;
import com.liquidfortress.packetanalyzer.pcap_file.PacketInfo;
import com.liquidfortress.packetanalyzer.pcap_file.PcapFileSummary;
import org.apache.logging.log4j.core.Logger;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.TcpPort;

/**
 * TcpPacketProcessor
 * <p/>
 * Processes TCP packets
 */
public class TcpPacketProcessor {
    private static Logger log = Main.log;

    public static void processTcpPacket(Packet packet, PcapFileSummary pcapFileSummary, PacketInfo packetInfo, Mode mode) {
        if (packet == null) {
            return; // skip empty packets
        }
        String sourceAddress = packetInfo.get(PacketInfo.SOURCE_ADDRESS);
        String destinationAddress = packetInfo.get(PacketInfo.DESTINATION_ADDRESS);
        try {
            log.trace("Converting to TCP packet");
            TcpPacket tcpPacket = TcpPacket.newPacket(packet.getRawData(), 0, packet.length());
            TcpPacket.TcpHeader tcpHeader = tcpPacket.getHeader();
            TcpPort sourcePort = tcpHeader.getSrcPort();
            TcpPort destinationPort = tcpHeader.getDstPort();
            String tcpSource = sourceAddress + ":" + sourcePort;
            String tcpDestination = destinationAddress + ":" + destinationPort;
            boolean syn = tcpHeader.getSyn();
            boolean ack = tcpHeader.getAck();
            boolean fin = tcpHeader.getFin();
            long sequenceNumber = tcpHeader.getSequenceNumberAsLong();
            long acknowledgementNumber = tcpHeader.getAcknowledgmentNumberAsLong();
            log.trace("TCP{ source: " + tcpSource + ", destination: " + tcpDestination +
                    ", SYN: " + syn + ", ACK: " + ack + ", FIN: " + fin +
                    ", seq number: " + sequenceNumber + ", ack number: " + acknowledgementNumber + " }");
            // Track TCP connection state
            IpAddressPair addressPair = new IpAddressPair(tcpSource, tcpDestination);
            TcpConnectionTracker tcpConnectionTracker = pcapFileSummary.activeTcpConnections.get(addressPair);
            if (tcpConnectionTracker == null) {
                tcpConnectionTracker = new TcpConnectionTracker(tcpSource, tcpDestination);
                if (syn) { // step 1: Client SYN
                    tcpConnectionTracker.setStep1ClientSequenceNumber(sequenceNumber);
                    tcpConnectionTracker.addFlowBytes((long) packet.length());
                    pcapFileSummary.activeTcpConnections.put(addressPair, tcpConnectionTracker);
                }
            } else if (!tcpConnectionTracker.isConnected() && !tcpConnectionTracker.isClosed()) {
                if (syn && ack) { // step 2: Server SYN-ACK
                    tcpConnectionTracker.setStep2Numbers(acknowledgementNumber, sequenceNumber);
                    tcpConnectionTracker.addFlowBytes((long) packet.length());
                } else if (ack) { // step 3: Client ACK
                    tcpConnectionTracker.setStep3Numbers(acknowledgementNumber, sequenceNumber);
                    tcpConnectionTracker.addFlowBytes((long) packet.length());
                    pcapFileSummary.tcpConnectionCount++;
                }
            } else if (tcpConnectionTracker.isConnected() && !tcpConnectionTracker.isClosed()) {
                if (fin && tcpConnectionTracker.getStep4CloseRequestSequenceNumber() == TcpConnectionTracker.NOT_DEFINED) {
                    // step 4: Initiator FIN_WAIT_1
                    tcpConnectionTracker.setStep4CloseRequestSequenceNumber(sequenceNumber);
                    tcpConnectionTracker.addFlowBytes((long) packet.length());
                } else if (ack && !fin &&
                        tcpConnectionTracker.getStep4CloseRequestSequenceNumber() != TcpConnectionTracker.NOT_DEFINED &&
                        tcpConnectionTracker.getStep5CloseRequestAckNumber() == TcpConnectionTracker.NOT_DEFINED) {
                    // step 5: Receiver ACK
                    tcpConnectionTracker.setStep5CloseRequestAckNumber(acknowledgementNumber);
                    tcpConnectionTracker.addFlowBytes((long) packet.length());
                } else if (fin && !ack &&
                        tcpConnectionTracker.getStep5CloseRequestAckNumber() != TcpConnectionTracker.NOT_DEFINED &&
                        tcpConnectionTracker.getStep6CloseRequestSequenceNumber() == TcpConnectionTracker.NOT_DEFINED) {
                    // step 6: Receiver FIN
                    tcpConnectionTracker.setStep6CloseRequestSequenceNumber(sequenceNumber);
                    tcpConnectionTracker.addFlowBytes((long) packet.length());
                } else if (fin && ack &&
                        tcpConnectionTracker.getStep4CloseRequestSequenceNumber() != TcpConnectionTracker.NOT_DEFINED &&
                        tcpConnectionTracker.getStep5CloseRequestAckNumber() == TcpConnectionTracker.NOT_DEFINED &&
                        tcpConnectionTracker.getStep6CloseRequestSequenceNumber() == TcpConnectionTracker.NOT_DEFINED) {
                    // combined step 5 and 6: Receiver FIN and ACK
                    tcpConnectionTracker.setStep5CloseRequestAckNumber(acknowledgementNumber);
                    tcpConnectionTracker.setStep6CloseRequestSequenceNumber(sequenceNumber);
                    tcpConnectionTracker.addFlowBytes((long) packet.length());
                } else if (ack && !fin &&
                        tcpConnectionTracker.getStep5CloseRequestAckNumber() != TcpConnectionTracker.NOT_DEFINED &&
                        tcpConnectionTracker.getStep6CloseRequestSequenceNumber() != TcpConnectionTracker.NOT_DEFINED &&
                        tcpConnectionTracker.getStep7CloseRequestAckNumber() == TcpConnectionTracker.NOT_DEFINED) {
                    // step 7: Initiator ACK
                    tcpConnectionTracker.setStep7CloseRequestAckNumber(acknowledgementNumber);
                    tcpConnectionTracker.addFlowBytes((long) packet.length());
                    // remove the closed TCP connection from tracking
                    pcapFileSummary.closedTcpConnections.add(tcpConnectionTracker);
                    pcapFileSummary.activeTcpConnections.remove(addressPair);
                } else { // add to flow tracking
                    tcpConnectionTracker.addFlowBytes((long) packet.length());
                }
            }
        } catch (IllegalRawDataException e) {
            log.error("Exception occurred while processing a packet. Exception was: " + e);
            System.exit(-2);
        }
    }
}

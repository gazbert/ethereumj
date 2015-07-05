package org.ethereum.net.swarm.bzz;

import org.ethereum.net.client.Capability;
import org.ethereum.net.p2p.Peer;
import org.ethereum.net.swarm.NetStore;
import org.ethereum.net.swarm.kademlia.stub.Address;
import org.ethereum.net.swarm.kademlia.stub.Node;
import org.ethereum.util.Functional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Admin on 18.06.2015.
 */
public class BzzProtocol implements Node, Functional.Consumer<BzzMessage> {
    private final static Logger LOG = LoggerFactory.getLogger("net.bzz");

    public static class PeerAddr {}

    private final static AtomicLong idGenerator = new AtomicLong(0);
    public final static int Version            = 0;
    public final static long ProtocolLength     = 8;
    public final static long ProtocolMaxMsgSize = 10 * 1024 * 1024;
    public final static int NetworkId          = 0;
    public final static int Strategy           = 0;

    NetStore netStore;
    Functional.Consumer<BzzMessage> messageSender;
    public Peer peer;
    PeerAddress node;
    Address addr;
//    Node node;

    boolean handshaken = false;
    boolean handshakeOut = false;

    transient ExecutorService storeRequestExecutor = Executors.newSingleThreadExecutor();

    public BzzProtocol(NetStore netStore) {
        this.netStore = netStore;
    }

    public void setMessageSender(Functional.Consumer<BzzMessage> messageSender) {
        this.messageSender = messageSender;
    }

    public void start(Peer peer) {
        this.peer = peer;
//        this.messageSender = messageSender;
        handshakeOut();
    }

    public PeerAddress getNode() {
        return node;
    }

    /*****   Cademlia Node interface   *******/

    public Address addr() {
        if (addr == null) {
            if (node != null) addr = new Address(node.getId());
        }
        return addr;
    }

    @Override
    public String url() {
        return null;
    }

    @Override
    public Date lastActive() {
        return null;
    }

    @Override
    public void Drop() {

    }

    /*****  END  *******/

/*
    public void storeRequest(final Key key) {
//        Future<?> res = storeRequestExecutor.submit(new Runnable() {
//            @Override
//            public void run() {
                long id = idGenerator.incrementAndGet();
//                Chunk chunk = netStore.localStore.dbStore.get(key);// ??? Why DB ?
                Chunk chunk = netStore.localStore.get(key);
                BzzStoreReqMessage msg = new BzzStoreReqMessage(id, key, chunk.getData());
                sendMessage(msg);
//            }
//        });
    }
*/

    private void handshakeOut() {
        if (!handshakeOut) {
            handshakeOut = true;
            PeerAddress selfAddress = new PeerAddress(netStore.self);
            BzzStatusMessage outStatus = new BzzStatusMessage(Version, "honey",
                    selfAddress.id, selfAddress, NetworkId,
                    Collections.singletonList(new Capability(Capability.BZZ, (byte) 1)));
            sendMessageImpl(outStatus);
        }
    }

    private void handshakeIn(BzzStatusMessage msg) {
        if (!handshaken) {
            netStore.statHandshakes.add(1);
            // TODO check status parameters
            node = msg.getAddr();
            netStore.hive.addPeer(this);

            handshaken = true;
            handshakeOut();

            for (BzzMessage pmsg : pendingHandshakeOutMessages) {
                sendMessageImpl(pmsg);
            }
            pendingHandshakeOutMessages = null;
            // ping the peer for self neighbours
            sendMessageImpl(new BzzRetrieveReqMessage(new Address(netStore.self)));

            for (BzzMessage pmsg : pendingHandshakeInMessages) {
                handleMsg(pmsg);
            }
            pendingHandshakeInMessages = null;

        } else {
//            throw new RuntimeException("Already handshaken.");
        }
    }

    List<BzzMessage> pendingHandshakeOutMessages = new ArrayList<>();
    List<BzzMessage> pendingHandshakeInMessages = new ArrayList<>();
    public synchronized void sendMessage(BzzMessage msg) {
        if (handshaken) {
            sendMessageImpl(msg);
        } else {
            pendingHandshakeOutMessages.add(msg);
        }
    }
    private void sendMessageImpl(BzzMessage msg) {
        netStore.statOutMsg.add(1);
        msg.setId(idGenerator.incrementAndGet());
        messageSender.accept(msg);
    }

    @Override
    public void accept(BzzMessage bzzMessage) {
        handleMsg(bzzMessage);
    }

    void handleMsg(BzzMessage msg) {
        synchronized (netStore) {
            netStore.statInMsg.add(1);
            msg.setPeer(this);
            LOG.trace(peer + " ===> " + netStore.self + ": " + msg);
            if (msg.getCommand() == BzzMessageCodes.STATUS) {
                handshakeIn((BzzStatusMessage) msg);
            } else {
                if (!handshaken) {
                    pendingHandshakeInMessages.add(msg);
                } else {
                    switch (msg.getCommand()) {
                        case STORE_REQUEST:
                            netStore.addStoreRequest((BzzStoreReqMessage) msg);
                            break;
                        case RETRIEVE_REQUEST:
                            netStore.addRetrieveRequest((BzzRetrieveReqMessage) msg);
                            break;
                        case PEERS:
                            netStore.hive.addPeerRecords((BzzPeersMessage) msg);
                            break;
                        default:
                            throw new RuntimeException("Invalid BZZ command: " + msg.getCommand() + ": " + msg);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return netStore.self + " => " + peer + " (Node: " + node + ")";
    }
}
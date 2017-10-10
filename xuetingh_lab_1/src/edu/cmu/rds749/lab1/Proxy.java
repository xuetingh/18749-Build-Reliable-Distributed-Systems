package edu.cmu.rds749.lab1;

import edu.cmu.rds749.common.AbstractProxy;
import edu.cmu.rds749.common.BankAccountStub;
import org.apache.commons.configuration2.Configuration;
import rds749.NoServersAvailable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
//import java.util.*;

/**
 * Created by jiaqi on 8/28/16.
 *
 * Implements the Proxy.
 */
public class Proxy extends AbstractProxy {

    long heartbeatInterval;
    private Server activeServer;
    private AtomicLong idCounter = new AtomicLong(0);
    private ConcurrentHashMap<Long,Server> serverMap;

    public Proxy(Configuration config)
    {
        super(config);
        this.heartbeatInterval = config.getLong("heartbeatIntervalMillis");
        this.serverMap = new ConcurrentHashMap<>();
        this.activeServer = null;

    }

    public Server getActiveServer() {
        Server server = this.activeServer;
        if (server == null) {
            server = changeServer();
        }
        return server;
    }

    public void setActiveServer(Server server) {
        this.activeServer = server;
    }

    public void removeServer(Server server) {
        if(server == null) {
            return;
        }
        this.serverMap.remove(server.getID());
    }

    public Server changeServer() {
        Iterator<Long> it = serverMap.keySet().iterator();
        if(it.hasNext()) return serverMap.get(it.next());
        else return null;

    }

    public int readBalance() throws NoServersAvailable
    {
        System.out.println("(In Proxy)");
        if (this.activeServer != null) {
            Server server = this.getActiveServer();

            try {
                int result = server.getStub().readBalance();
                return result;
            } catch (BankAccountStub.NoConnectionException var4) {
                System.err.println(var4.getMessage() + ":" + server.getID());
                removeServer(server);
                activeServer = changeServer();
                return this.readBalance();
            }
        } else {
            throw new NoServersAvailable();
        }
    }

    public int changeBalance(int update) throws NoServersAvailable
    {
        System.out.println("(In Proxy)");
        if (this.activeServer != null) {
            Server server = this.getActiveServer();

            try {
                int result = server.getStub().changeBalance(update);
                return result;
            } catch (BankAccountStub.NoConnectionException var4) {
                System.err.println(var4.getMessage() + ":" + server.getID());
                removeServer(server);
                activeServer = changeServer();
                return this.changeBalance(update);
            }
        } else {
            throw new NoServersAvailable();
        }
    }

    public long register(String hostname, int port)
    {
        BankAccountStub connectedServer = this.connectToServer(hostname, port);
        AtomicLong serverID = generateID();
        System.out.print(serverID);
        Server newServer = new Server(hostname, port, connectedServer, serverID.longValue(), System.currentTimeMillis());
        System.out.print(serverMap.toString());
        serverMap.putIfAbsent(serverID.longValue(), newServer);
        System.out.print(serverMap.toString());
        if (serverMap.size() == 1) activeServer = newServer;

        heartbeatChecker hbc = new heartbeatChecker( heartbeatInterval, newServer);
        return serverID.longValue();
    }

    public void heartbeat(long ID, long serverTimestamp) {
        Server currServer = serverMap.get(ID);
        long lastTimestamp = currServer.getTimestamp();
        if (serverTimestamp < lastTimestamp) {
            return;
        } else {
            currServer.setTimestamp(serverTimestamp);
        }

    }

    public AtomicLong generateID() {
        AtomicLong id = idCounter;
        idCounter.getAndIncrement();
        return id;
    }

    public class heartbeatChecker {
        private long heartbeatInterval;
        private Server server;
        private ScheduledExecutorService schedExecService;

        public heartbeatChecker(long heartbeatInterval, Server server)
        {
            this.server = server;
            this.heartbeatInterval = heartbeatInterval;
            schedExecService = Executors.newSingleThreadScheduledExecutor();
            this.schedExecService.scheduleWithFixedDelay(new checkTask(),
                    0, 2 * heartbeatInterval, TimeUnit.MILLISECONDS);
        }
        public class checkTask implements Runnable {
            public void run() {
                if (System.currentTimeMillis() - server.getTimestamp() >= 2 * heartbeatInterval) {
                    removeServer(server);
                    if (server.getID() == activeServer.getID()) {
                        setActiveServer(null);
                        activeServer = changeServer();
                    }
                }
            }
        }
    }
}

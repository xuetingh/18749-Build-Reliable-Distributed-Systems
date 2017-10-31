package edu.cmu.rds749.lab2;

import edu.cmu.rds749.common.AbstractProxy;
import edu.cmu.rds749.common.BankAccountStub;
import org.apache.commons.configuration2.Configuration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;



/**
 * Implements the Proxy.
 */
public class Proxy extends AbstractProxy {
    /**
     * class Server records information about a server.
     */
    public class Server {
        public boolean isAlive;
        public BankAccountStub stub;

        public Server(BankAccountStub stub, boolean isAlive) {
            this.isAlive = isAlive;
            this.stub = stub;
        }
    }

    // a hashmap for registered servers
    private HashMap<Long, Server> serverMap;
    // a list of failed servers, don't know if it is safe
    private List<Long> failedServers;
    // records which server completes which request, key is request id, value is a list of server id
    private HashMap<Integer, List<Long>> requests;

    private Object serverMapLock;
    private Object failedServersLock;
    private Object requestLock;


    public Proxy(Configuration config) {
        super(config);
        serverMap = new HashMap<>();
        requests = new HashMap<>();
        failedServers = new ArrayList<>();
        serverMapLock = new Object();
        failedServersLock = new Object();
        requestLock = new Object();
    }

    @Override
    protected synchronized void serverRegistered(long id, BankAccountStub stub) {
        int state;
        boolean isAlive = true;

        for (long serverId : serverMap.keySet()) {
            if (serverMap.get(serverId).isAlive) {
                try {
                    state = serverMap.get(serverId).stub.getState();
                } catch (BankAccountStub.NoConnectionException e) {
                    serverMap.get(serverId).isAlive = false;
                    synchronized (failedServersLock){
                    failedServers.add(serverId);
                    }
                    continue;
                }

                try {
                    stub.setState(state);
                } catch (BankAccountStub.NoConnectionException e) {
                    isAlive = false;
                    synchronized (failedServersLock){
                        failedServers.add(serverId);
                    }
                }
                break;
            }
        }
        synchronized (serverMapLock) {
        serverMap.put(id, new Server(stub, isAlive));
        }
        synchronized (failedServersLock){
            serversFailed(failedServers);
        }
    }

    @Override
    protected synchronized void beginReadBalance(int reqid) {
        System.out.println("(In Proxy)");
        this.sendToAllServers(reqid, 0, false);
    }


    @Override
    protected synchronized void beginChangeBalance(int reqid, int update) {
        System.out.println("(In Proxy)");
        this.sendToAllServers(reqid, update, true);
    }


    private synchronized void sendToAllServers (int reqid, int update, boolean change) {
        boolean reqFail = true;
        for (long id : serverMap.keySet()) {
            if (serverMap.get(id).isAlive) {
                try {
                    if (change) serverMap.get(id).stub.beginChangeBalance(reqid, update);
                    else serverMap.get(id).stub.beginReadBalance(reqid);
                    reqFail = false;
                } catch (BankAccountStub.NoConnectionException e) {
                    serverMap.get(id).isAlive = false;
                    synchronized (failedServersLock){
                        failedServers.add(id);
                    }
                }
            }
        }
        if (reqFail) clientProxy.RequestUnsuccessfulException(reqid);
    }

    @Override
    protected synchronized void endReadBalance(long serverid, int reqid, int balance) {
        System.out.println("(In Proxy)");
        if (requests.get(reqid) == null) {
            List<Long> serverList = new ArrayList<>();
            serverList.add(serverid);
            synchronized (requestLock) {
                requests.put(reqid, serverList);

            clientProxy.endReadBalance(reqid, balance);}
        } else {
            List<Long> list = requests.get(reqid);
            list.add(serverid);
            synchronized (requestLock) {
            requests.put(reqid, list);
            }
        }
//        System.out.println("end read balance, server:" + serverid + ", request:" + reqid);
//        System.out.println(requests.get(reqid).toString());
    }

    @Override
    protected synchronized void endChangeBalance(long serverid, int reqid, int balance) {
        System.out.println("(In Proxy)");
        if (requests.get(reqid) == null) {
            List<Long> serverList = new ArrayList<>();
            serverList.add(serverid);
            synchronized (requestLock) {
            requests.put(reqid, serverList);
            clientProxy.endChangeBalance(reqid, balance);}
        } else {
            List<Long> list = requests.get(reqid);
            list.add(serverid);
            synchronized (requestLock) {
            requests.put(reqid, list);}
        }

    }

    @Override
    protected void serversFailed(List<Long> failedServers) {
        super.serversFailed(failedServers);
    }
}
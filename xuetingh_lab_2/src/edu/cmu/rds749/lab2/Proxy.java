package edu.cmu.rds749.lab2;

import edu.cmu.rds749.common.AbstractProxy;
import edu.cmu.rds749.common.BankAccountStub;
import org.apache.commons.configuration2.Configuration;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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

        @Override
        public String toString() {
            return "" + this.isAlive;
        }
    }

    // a hashmap for registered servers
    private HashMap<Long, Server> serverMap;
    // a list of failed servers, don't know if it is safe
    private List<Long> failedServers;
    // records which server completes which request, key is request id, value is a list of server id
    private HashMap<Integer, List<Long>> requests;

    private ReadWriteLock serverMapLock;
    private ReadWriteLock failedServersLock;
    private ReadWriteLock requestLock;


    public Proxy(Configuration config) {
        super(config);
        serverMap = new HashMap<>();
        requests = new HashMap<>();
        failedServers = new ArrayList<>();
        serverMapLock = new ReentrantReadWriteLock();
        failedServersLock = new ReentrantReadWriteLock();
        requestLock = new ReentrantReadWriteLock();
    }

    @Override
    protected synchronized void serverRegistered(long id, BankAccountStub stub) {
        System.out.println("register");

        int state;
        boolean isAlive = true;
        while (!requests.isEmpty()) ;
        for (long serverId : serverMap.keySet()) {

            if (serverMap.get(serverId).isAlive) {
                try {
                    serverMapLock.readLock().lock();
                    state = serverMap.get(serverId).stub.getState();

                    System.out.println("in get state");
                    System.out.println(serverId);
                } catch (BankAccountStub.NoConnectionException e) {
                    System.out.println("catch in serverRegistered");
                    serverMapLock.readLock().unlock();

                    serverMapLock.writeLock().lock();
                    serverMap.get(serverId).isAlive = false;
                    serverMapLock.writeLock().unlock();

                    failedServersLock.writeLock().lock();
                    System.out.println("jia list " + serverId);
                    failedServers.add(serverId);
                    failedServersLock.writeLock().unlock();
                    continue;
                } finally {
                    serverMapLock.readLock().unlock();
                }

                try {

                    stub.setState(state);
                } catch (BankAccountStub.NoConnectionException e) {
                    isAlive = false;

                    failedServersLock.writeLock().lock();
                    failedServers.add(serverId);
                    failedServersLock.writeLock().unlock();
                }
                break;
            }
        }

        serverMapLock.writeLock().lock();
        serverMap.put(id, new Server(stub, isAlive));
        serverMapLock.writeLock().unlock();

        failedServersLock.readLock().lock();
        System.out.println("called by me");
        serversFailed(failedServers);
        failedServersLock.readLock().unlock();

        System.out.println(serverMap);
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


    private synchronized void sendToAllServers(int reqid, int update, boolean change) {
        boolean reqFail = true;
        for (long id : serverMap.keySet()) {
            if (serverMap.get(id).isAlive) {
                try {
                    if (change) {
                        serverMap.get(id).stub.beginChangeBalance(reqid, update);
                    } else {
                        serverMap.get(id).stub.beginReadBalance(reqid);
                    }
                    reqFail = false;
                } catch (BankAccountStub.NoConnectionException e) {

                    serverMapLock.writeLock().lock();
                    serverMap.get(id).isAlive = false;
                    serverMapLock.writeLock().unlock();

                    failedServersLock.writeLock().lock();
                    failedServers.add(id);
                    System.out.println("called by me");
                    serversFailed(failedServers);
                    failedServersLock.writeLock().unlock();
                }
            }
        }

        if (reqFail) clientProxy.RequestUnsuccessfulException(reqid);
    }

    @Override
    protected synchronized void endReadBalance(long serverid, int reqid, int balance) {
        System.out.println("(In Proxy)");

        requestLock.readLock().lock();
        if (requests.get(reqid) == null) {
            List<Long> serverList = new ArrayList<>();
            serverList.add(serverid);
            requestLock.readLock().unlock();
            requestLock.writeLock().lock();
            requests.put(reqid, serverList);
            requestLock.writeLock().unlock();
            clientProxy.endReadBalance(reqid, balance);
        } else {
            List<Long> list = requests.get(reqid);
            list.add(serverid);
            requestLock.readLock().unlock();
            requestLock.writeLock().lock();
            requests.put(reqid, list);
            requestLock.writeLock().unlock();
        }
        checkRequests(reqid);
//        System.out.println("end read balance, server:" + serverid + ", request:" + reqid);
//        System.out.println(requests.get(reqid).toString());
    }

    @Override
    protected synchronized void endChangeBalance(long serverid, int reqid, int balance) {
        System.out.println("(In Proxy)");
        requestLock.readLock().lock();
        if (requests.get(reqid) == null) {
            List<Long> serverList = new ArrayList<>();
            serverList.add(serverid);
            requestLock.readLock().unlock();

            requestLock.writeLock().lock();
            requests.put(reqid, serverList);
            requestLock.writeLock().unlock();

            clientProxy.endChangeBalance(reqid, balance);
        } else {
            List<Long> list = requests.get(reqid);
            list.add(serverid);

            requestLock.readLock().unlock();

            requestLock.writeLock().lock();
            requests.put(reqid, list);
            requestLock.writeLock().unlock();
            checkRequests(reqid);
        }
    }

    protected synchronized void checkRequests(int reqid) {
        boolean isDone = true;
        requestLock.readLock().lock();
        serverMapLock.readLock().lock();
        for(long id : serverMap.keySet()) {
            if (serverMap.get(id).isAlive == true && !requests.get(reqid).contains(id)) {
                isDone = false;
                break;
            }
        }
        requestLock.readLock().unlock();
        serverMapLock.readLock().unlock();

        requestLock.writeLock().lock();
        if (isDone) requests.remove(reqid);
        requestLock.writeLock().unlock();
    }

//    protected synchronized void checkRequests(int reqid) {
//        boolean isDone = false;
//        requestLock.readLock().lock();
//        serverMapLock.readLock().lock();
//        for (long id : serverMap.keySet()) {
//            if (serverMap.get(id).isAlive == true && requests.get(reqid).contains(id)) {
//                isDone = true;
//                break;
//            }
//        }
//        requestLock.readLock().unlock();
//        serverMapLock.readLock().unlock();
//
//        requestLock.writeLock().lock();
//        if (isDone) requests.remove(reqid);
//        requestLock.writeLock().unlock();
//    }

    @Override
    protected void serversFailed(List<Long> failedServers) {
        System.out.println("failedServers size");
        System.out.println(failedServers.size());
        for (long f : failedServers) {
            System.out.println("f:" + f);
        }
        for (long serverId : serverMap.keySet()) {
            System.out.println();
            System.out.println("serverid:" + serverId);

            if (failedServers.contains(serverId)) {
                System.out.println("contained");
                serverMap.get(serverId).isAlive = false;
            }
        }
        super.serversFailed(failedServers);
    }
}
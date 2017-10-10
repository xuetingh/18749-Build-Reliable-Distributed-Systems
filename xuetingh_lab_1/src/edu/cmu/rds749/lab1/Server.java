package edu.cmu.rds749.lab1;

import edu.cmu.rds749.common.BankAccountStub;

import java.util.concurrent.atomic.AtomicLong;


public class Server {
    private BankAccountStub server;
    private String hostname;
    private int port;
    private long serverID;
    private long timestamp;
//    private boolean isAlive;

    public Server(String hostname, int port, BankAccountStub server, long id, long timestamp) {
        this.hostname = hostname;
        this.port = port;
        this.serverID = id;
        this.server = server;
        this.timestamp = timestamp;
//        this.isAlive = true;
    }

    public long getID() {
        return this.serverID;
    }

    public BankAccountStub getStub () {
        return this.server;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public int changeBalance(int update) throws BankAccountStub.NoConnectionException
    {
        return server.changeBalance(update);
    }


    public int readBalance() throws BankAccountStub.NoConnectionException
    {
        return server.readBalance();
    }
}

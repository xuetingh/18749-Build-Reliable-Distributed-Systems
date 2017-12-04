package edu.cmu.rds749.lab3;

import edu.cmu.rds749.common.AbstractServer;
import org.apache.commons.configuration2.Configuration;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import rds749.Checkpoint;

/**
 * Implements the BankAccounts transactions interface
 * Created by utsav on 8/27/16.
 */

public class BankAccountI extends AbstractServer
{
    private int balance = 0;
    private int curReqid = 0; // most recent request ID
    private boolean primaryServer;
    private ProxyControl ctl;
    private final LinkedList<Request> requestsLog = new LinkedList<>();

    private enum Methods { READ_BALANCE, CHANGE_BALANCE; }

    public class Request {
        Methods requestType;
        int reqid;
        int update;

        public Request(Methods requestType, int reqid, int update) {
            this.requestType = requestType;
            this.reqid = reqid;
            this.update = update;
        }
    }

    public BankAccountI(Configuration config) {
        super(config);
    }

    @Override
    protected void doStart(ProxyControl ctl) throws Exception {
        this.ctl = ctl;
    }

    @Override
    protected synchronized void handleBeginReadBalance(int reqid)
    {
        if (primaryServer) {
            System.out.println("PRIMARY READ: " + reqid);
            curReqid = reqid;

            ctl.endReadBalance(reqid, balance);
            System.out.println("Current balance: " + balance);
        }
        else {
            System.out.println("BACKUP READ");
            System.out.print("Previous log ");
            logDebug();
            Request request = new Request(Methods.READ_BALANCE, reqid, 0);
            requestsLog.add(request);
            System.out.print("After log ");
            logDebug();
        }
    }

    @Override
    protected synchronized void handleBeginChangeBalance(int reqid, int update)
    {
        if (primaryServer) {
            System.out.println("PRIMARY CHANGE: " + reqid);
            System.out.println("Old balance: " + balance);
            curReqid = reqid;
            this.balance += update;
            ctl.endChangeBalance(reqid, balance);
            System.out.println("New balance: " + balance + " Current reqid: " + curReqid);
        } else {
            System.out.println("BACKUP CHANGE ");
            System.out.print("Previous log ");
            logDebug();
            System.out.println("Current reqid: " + curReqid);
            Request request = new Request(Methods.CHANGE_BALANCE, reqid, update);
            requestsLog.add(request);
            System.out.print("After log: ");
            logDebug();

        }
    }

    @Override
    protected synchronized Checkpoint handleGetState()
    {
        System.out.println("CHECKPOINT GET STATE: " + balance);
        return new Checkpoint(curReqid, balance);
    }

    void logDebug()
    {
        for (int i = 0; i < requestsLog.size(); i++) {
            System.out.print("(" + requestsLog.get(i).reqid + ": " +  requestsLog.get(i).requestType + ", " + requestsLog.get(i).update + ") ");
        }
        System.out.println();
    }

    @Override
    protected synchronized int handleSetState(Checkpoint checkpoint)
    {
        System.out.println("CHECKPOINT SET STATE");
        System.out.println("Before: balance:" + balance + " curReqid: " + curReqid);

        balance = checkpoint.state;
        curReqid = checkpoint.reqid;

        System.out.println("After: balance:" + balance + " curReqid: " + curReqid);

        System.out.print("Previous log: ");
        logDebug();

        System.out.println("LOG PRUNING");
        for(Iterator<Request> i = requestsLog.iterator(); i.hasNext();)  {
            Request request = i.next();
            if (request.reqid <= curReqid)
                i.remove();
        }

        System.out.print("Current log: ");
        logDebug();

        return requestsLog.size();
    }

    @Override
    protected synchronized void handleSetPrimary()
    {
        System.err.println("SET Primary server");
        System.out.print("Previous log: ");
        logDebug();

        System.out.println("Before setting primary: balance:" + balance + " curReqid: " + curReqid);
        while (!requestsLog.isEmpty()) {
            Request request = requestsLog.removeFirst();
            switch (request.requestType) {
                case READ_BALANCE:
                    ctl.endReadBalance(request.reqid, balance);
                    break;
                case CHANGE_BALANCE:
                    balance += request.update;
                    ctl.endChangeBalance(request.reqid, balance);
            }
        }
        System.out.println("After setting primary: balance:" + balance + " curReqid: " + curReqid);
        primaryServer = true;
    }

    @Override
    protected synchronized void handleSetBackup()
    {
        System.err.println("SET Back up server");
        primaryServer = false;
    }
}
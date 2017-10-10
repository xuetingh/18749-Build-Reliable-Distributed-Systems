package edu.cmu.rds749.lab1;

import edu.cmu.rds749.common.AbstractServer;
import org.apache.commons.configuration2.Configuration;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.Timer;
/**
 * Implements the BankAccounts transactions interface
 * Created by utsav on 8/27/16.
 */

public class BankAccountI extends AbstractServer
{
    private final Configuration config;
    private ScheduledExecutorService schedExecService;
    private int balance = 0;
    private long serverId = -1L;
    private ProxyControl ctl;
    Timer timer;

    public BankAccountI(Configuration config) {
        super(config);
        this.config = config;
        this.schedExecService = Executors.newSingleThreadScheduledExecutor();
    }

    protected void doStart(ProxyControl ctl) throws Exception {
        this.ctl = ctl;
        this.serverId = ctl.register(this.config.getString("serverHost"), this.config.getInt("serverPort"));
        this.schedExecService.scheduleWithFixedDelay(new HeartbeatChecker(this),
                0, this.config.getLong("heartbeatIntervalMillis"), TimeUnit.MILLISECONDS);
    }

    protected int handleReadBalance() {
        return this.balance;
    }

    protected int handleChangeBalance(int update) {
        this.balance += update;
        return this.balance;
    }


    public class HeartbeatChecker implements Runnable {
        private  BankAccountI b;

        public HeartbeatChecker(BankAccountI b) {
            this.b = b;
        }
        @Override

        public void run() {
            try {
                //server id
                b.ctl.heartbeat(b.serverId, System.currentTimeMillis());
            } catch (IOException e) {
                System.err.println("failed to send heartbeat");
            }
        }
    }

}

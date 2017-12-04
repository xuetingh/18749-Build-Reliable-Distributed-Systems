package edu.cmu.rds749.lab3;

import edu.cmu.rds749.common.AbstractProxy;
import edu.cmu.rds749.common.BankAccountStub;
import org.apache.commons.configuration2.Configuration;
import java.util.*;


/**
 * Created by jiaqi on 8/28/16.
 *
 * Implements the Proxy.
 */
public class Proxy extends AbstractProxy
{

    public Proxy(Configuration config)
    {
        super(config);
    }

    @Override
    protected void serverRegistered(long id, BankAccountStub stub)
    {

    }

    @Override
    protected void beginReadBalance(int reqid)
    {
        System.out.println("(In Proxy)");
    }

    @Override
    protected void beginChangeBalance(int reqid, int update)
    {
        System.out.println("(In Proxy)");
    }

    @Override
    protected void endReadBalance(long serverid, int reqid, int balance)
    {
        System.out.println("(In Proxy -- endReadBalance)");
    }

    @Override
    protected void endChangeBalance(long serverid, int reqid, int balance)
    {
        System.out.println("(In Proxy -- endChangeBalance)");
    }

    @Override
    protected void serversFailed(List<Long> failedServers)
    {
        System.err.println("Servers failed:" + failedServers);
    }
}

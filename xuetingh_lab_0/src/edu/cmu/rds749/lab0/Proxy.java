package edu.cmu.rds749.lab0;

import Ice.Current;
import edu.cmu.rds749.common.AbstractProxy;

/**
 * Created by jiaqi on 8/28/16.
 *
 * Implements the Proxy.
 */
public class Proxy extends AbstractProxy
{
    public Proxy(String [] hostname, String [] port)
    {
        super(hostname, port);
    }
    private int balance = 0;
    public int readBalance(Current __current)
    {
        System.out.println("(In Proxy)");
        // student answer - Please change next line
        if (this.numberOfAccounts > 0 && this.actualBankAccounts[0] != null) {
            balance = this.actualBankAccounts[0].readBalance();
            return  balance;
        } else {
            return balance;
        }
    }

    public int changeBalance(int update, Current __current)
    {
        System.out.println("(In Proxy)");
        // student answer - Please change next line:
        if (this.numberOfAccounts > 0 && this.actualBankAccounts[0] != null) {
            balance = this.actualBankAccounts[0].changeBalance(update);
            return balance;
        } else {
            balance += update;
            return balance;
        }
    }

}

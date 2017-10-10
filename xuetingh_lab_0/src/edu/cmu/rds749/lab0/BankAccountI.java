package edu.cmu.rds749.lab0;

import Ice.Current;
import edu.cmu.rds749.common.rds749._BankAccountDisp;

/**
 * Implements the BankAccounts transactions interface
 * Created by utsav on 8/27/16.
 */

public class BankAccountI extends _BankAccountDisp
{
    /**
     * STATE: Money in the account
     */
    private int balance = 0;

    public int readBalance(Current __current)
    {
        // DO NOT REMOVE THIS LINE
        System.err.println("(readBalance:"+__current.con._toString().replace("\n"," / ")+")");

        return this.balance;
    }

    public int changeBalance(int update, Current __current)
    {
        // DO NOT REMOVE THIS LINE
        System.err.println("(changeBalance:"+__current.con._toString().replace("\n"," / ")+")");

        this.balance += update;
        return this.balance;
    }

}

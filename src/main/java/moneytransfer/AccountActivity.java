package moneytransfer;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface AccountActivity {
    // Retirar (Withdraw) una cantidad de dinero de la cuenta de origen
    @ActivityMethod
    void withdraw(String accountId, String referenceId, int amount);

    // Depositar (Deposit) una cantidad de dinero en la cuenta de destino
    @ActivityMethod
    void deposit(String accountId, String referenceId, int amount);

    // Compensar (Compensate) un depósito fallido reembolsándolo a la cuenta original
    @ActivityMethod
    void refund(String accountId, String referenceId, int amount);
}

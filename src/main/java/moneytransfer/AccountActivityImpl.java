package moneytransfer;

import io.temporal.activity.*;

public class AccountActivityImpl implements AccountActivity {
    // Mock up the withdrawal of an amount of money from the source account
    // Simular (Mock) el retiro de una cantidad de dinero de la cuenta de origen
    @Override
    public void withdraw(String accountId, String referenceId, int amount) {
        System.out.printf("\nWithdrawing $%d from account %s.\n[ReferenceId: %s]\n", amount, accountId, referenceId);
        System.out.flush();
    }

    // Mock up the deposit of an amount of money from the destination account
    // Simular el depósito de una cantidad de dinero desde la cuenta de destino
    @Override
    public void deposit(String accountId, String referenceId, int amount) {
        boolean activityShouldSucceed = true;

        if (!activityShouldSucceed) {
            System.out.println("Deposit failed");
            System.out.flush();
            throw Activity.wrap(new RuntimeException("Simulated Activity error during deposit of funds"));
        }

        System.out.printf("\nDepositing $%d into account %s.\n[ReferenceId: %s]\n", amount, accountId, referenceId);
        System.out.flush();
    }

    // Mock up a compensation refund to the source account
    // Simular un reembolso de compensación a la cuenta de origen
    @Override
    public void refund(String accountId, String referenceId, int amount) {
        boolean activityShouldSucceed = true;

        if (!activityShouldSucceed) {
            System.out.println("Refund failed");
            System.out.flush();
            throw Activity.wrap(new RuntimeException("Simulated Activity error during refund to source account"));
        }

        System.out.printf("\nRefunding $%d to account %s.\n[ReferenceId: %s]\n", amount, accountId, referenceId);
        System.out.flush();
    }
}

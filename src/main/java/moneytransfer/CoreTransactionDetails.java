package moneytransfer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CoreTransactionDetails implements TransactionDetails {

    private String sourceAccountId;
    private String destinationAccountId;
    private String transactionReferenceId;
    private int amountToTransfer;

}

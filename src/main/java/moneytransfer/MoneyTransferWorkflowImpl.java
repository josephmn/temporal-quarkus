package moneytransfer;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class MoneyTransferWorkflowImpl implements MoneyTransferWorkflow {

    private static final String WITHDRAW = "Withdraw";

    // RetryOptions specify how to automatically handle retries when Activities fail
    // RetryOptions especifica cómo manejar automáticamente los reintentos cuando las actividades fallan
    private final RetryOptions retryoptions = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1)) // Espere 1 segundo antes del primer reintento
            .setMaximumInterval(Duration.ofSeconds(20)) // No exceda los 20 segundos entre reintentos
            .setBackoffCoefficient(2) // Espere 1 segundo, luego 2, luego 4, etc.
            .setMaximumAttempts(5000) // Falla después de 5000 intentos
            .build();

    // ActivityOptions especifican los límites sobre cuánto tiempo puede durar una actividad antes de ejecutarse.
    // siendo interrumpido por el servicio de orquestación
    private final ActivityOptions defaultActivityOptions = ActivityOptions.newBuilder()
            .setRetryOptions(retryoptions) // Aplicar las RetryOptions definidas anteriormente
            .setStartToCloseTimeout(Duration.ofSeconds(2)) // Tiempo máximo de ejecución para una sola actividad
            .setScheduleToCloseTimeout(Duration.ofSeconds(5000)) // Duración completa desde la programación hasta la finalización, incluido el tiempo de espera
            .build();

    private final Map<String, ActivityOptions> perActivityMethodOptions = new HashMap<String, ActivityOptions>() {{
        // Un tiempo de espera de latido es un indicador de prueba de vida de que una actividad aún está funcionando.
        // La duración de 5 segundos utilizada aquí espera hasta 5 segundos para escuchar un latido.
        // Si no se escucha ninguno, la Actividad falla.
        // El método `withdraw` está codificado para que tenga éxito, por lo que esto nunca sucede.
        // Utilice latidos para aplicaciones impulsadas por eventos de larga duración.
        put(WITHDRAW, ActivityOptions.newBuilder().setHeartbeatTimeout(Duration.ofSeconds(5)).build());
    }};

    // Los ActivityStubs permiten llamadas a métodos como si el objeto Activity fuera local, pero en realidad realizan una invocación RPC.
    private final AccountActivity accountActivityStub = Workflow.newActivityStub(AccountActivity.class, defaultActivityOptions, perActivityMethodOptions);

    // El método de transferencia es el punto de entrada al flujo de trabajo.
    // Las ejecuciones de métodos de actividad se pueden orquestar aquí o desde otros métodos de actividad.
    @Override
    public void transfer(TransactionDetails transaction) {
        // Recuperar información de la transacción de la instancia `transacción`
        String sourceAccountId = transaction.getSourceAccountId();
        String destinationAccountId = transaction.getDestinationAccountId();
        String transactionReferenceId = transaction.getTransactionReferenceId();
        int amountToTransfer = transaction.getAmountToTransfer();

        // Etapa 1: Retirar (Withdraw) fondos de la fuente
        try {
            // Iniciar actividad de «retiro» (withdrawal)
            accountActivityStub.withdraw(sourceAccountId, transactionReferenceId, amountToTransfer);
        } catch (Exception e) {
            // Si el retiro (withdrawal) falla, por cualquier excepción, se detecta aquí
            System.out.printf("[%s] Withdrawal of $%d from account %s failed", transactionReferenceId, amountToTransfer, sourceAccountId);
            System.out.flush();

            // La transacción termina aquí
            return;
        }

        // Etapa 2: Depositar (Deposit) fondos en destino
        try {
            // Realizar la actividad de `depósito` (deposit)
            accountActivityStub.deposit(destinationAccountId, transactionReferenceId, amountToTransfer);

            // El 'depósito' fue exitoso
            System.out.printf("[%s] Transaction succeeded.\n", transactionReferenceId);
            System.out.flush();

            //  La transacción termina aquí
            return;
        } catch (Exception e) {
            // Si el depósito (deposit) falla, por cualquier excepción, se detecta aquí
            System.out.printf("[%s] Deposit of $%d to account %s failed.\n", transactionReferenceId, amountToTransfer, destinationAccountId);
            System.out.flush();
        }

        // Continuar compensando con un reembolso
        try {
            // Realizar la actividad de `reembolso` (refund)
            System.out.printf("[%s] Refunding $%d to account %s.\n", transactionReferenceId, amountToTransfer, sourceAccountId);
            System.out.flush();

            accountActivityStub.refund(sourceAccountId, transactionReferenceId, amountToTransfer);

            // Recuperación exitosa. La transacción finaliza aquí
            System.out.printf("[%s] Refund to originating account was successful.\n", transactionReferenceId);
            System.out.printf("[%s] Transaction is complete. No transfer made.\n", transactionReferenceId);
            return;
        } catch (Exception e) {
            // Un mecanismo de recuperación también puede fallar. Gestione cualquier excepción aquí
            System.out.printf("[%s] Deposit of $%d to account %s failed. Did not compensate withdrawal.\n",
                    transactionReferenceId, amountToTransfer, destinationAccountId);
            System.out.printf("[%s] Workflow failed.", transactionReferenceId);
            System.out.flush();

            // Volver a lanzar la excepción provoca un error en la tarea de flujo de trabajo
            throw(e);
        }
    }
}

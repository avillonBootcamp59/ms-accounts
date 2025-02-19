package proyecto1.msaccounts.entity;

import lombok.Data;
import org.bson.codecs.pojo.annotations.BsonId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "accounts")
public class Account {
    @BsonId
    private String id;
    private String number;
    private String type; // Ahorro, Corriente, Plazo Fijo
    private String customerId;
    private Double balance;
    private boolean hasMaintenanceFee;
    private int transactionLimit;
    private List<String> authorizedSigners; // Firmantes autorizados (solo para cuentas empresariales)
    private Double commissionFee; // Comisión por transacción adicional
    private Double minimumOpeningBalance; // Monto mínimo de apertura
    private LocalDateTime lastTransactionDate;
    private Integer freeTransactions; // Número de transacciones sin comisión
    private Integer transactionCount; // Contador de transacciones

    public Account() {
        this.lastTransactionDate = LocalDateTime.now();
    }

}

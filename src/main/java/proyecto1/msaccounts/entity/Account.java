package proyecto1.msaccounts.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "accounts")
public class Account {
    @Id
    private String id;
    private String number;
    private String type; // Ahorro, Corriente, Plazo Fijo
    private String customerId;
    private Double balance;
    private boolean hasMaintenanceFee;
    private int transactionLimit;
    private List<String> authorizedSigners; // Firmantes autorizados (solo para cuentas empresariales)
}

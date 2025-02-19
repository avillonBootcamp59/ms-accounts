package proyecto1.msaccounts.dto;

import lombok.Data;
import org.bson.codecs.pojo.annotations.BsonId;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
public class CreditDTO {
    private String id;
    private String customerId; // Relación con Cliente
    private Double amount;
    private String creditType; // "personal", "empresarial", "	Tarjeta de Crédito "
    private Double interestRate;
    private Double creditLimit;
    private Double currentDebt;
    private Double availableLimit;
}

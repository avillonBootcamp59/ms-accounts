package com.bank.pe.msaccounts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTO {
    private String id;
    private String number;
    private String type;
    private String customerId;
    private Double balance;
    private boolean hasMaintenanceFee;
    private int transactionLimit;
    private List<String> authorizedSigners;
    private Double commissionFee;
    private Double minimumOpeningBalance;
    private LocalDateTime lastTransactionDate;
    private Integer freeTransactions;
    private Integer transactionCount;
    private LocalDateTime createdAt;
}

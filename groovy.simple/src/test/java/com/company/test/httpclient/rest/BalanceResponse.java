package com.company.test.httpclient.rest;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;


public class BalanceResponse implements Serializable {

    private static final long serialVersionUID = 7002814469994176194L;

    private BigDecimal amount;
    private Currency currency;
    private Date balanceDate;
    private Date dateOfExpire;

    public BalanceResponse() {
        this.amount = amount;
        this.currency = currency;
        this.balanceDate = balanceDate;
        this.dateOfExpire = dateOfExpire;
    }

    public BalanceResponse(BigDecimal amount, Currency currency, Date balanceDate, Date dateOfExpire) {
        this.amount = amount;
        this.currency = currency;
        this.balanceDate = balanceDate;
        this.dateOfExpire = dateOfExpire;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Date getBalanceDate() {
        return balanceDate;
    }

    public void setBalanceDate(Date balanceDate) {
        this.balanceDate = balanceDate;
    }

    public Date getDateOfExpire() {
        return dateOfExpire;
    }

    public void setDateOfExpire(Date dateOfExpire) {
        this.dateOfExpire = dateOfExpire;
    }

    @Override
    public String toString() {
        return "BalanceResponse{" +
                "amount=" + amount +
                ", currency=" + currency +
                ", balanceDate=" + balanceDate +
                ", dateOfExpire=" + dateOfExpire +
                '}';
    }

    public static class Currency implements Serializable {

        private static final long serialVersionUID = -6119628287725486707L;

        private String symbol;
        private String code;
        private String name;

        public Currency() {
            this.symbol = symbol;
            this.code = code;
            this.name = name;
        }

        public Currency(String symbol, String code, String name) {
            this.symbol = symbol;
            this.code = code;
            this.name = name;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Currency{" +
                    "symbol='" + symbol + '\'' +
                    ", code='" + code + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}
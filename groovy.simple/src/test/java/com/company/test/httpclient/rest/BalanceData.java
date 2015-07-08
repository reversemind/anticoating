package com.company.test.httpclient.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BalanceData implements Serializable {

	private static final long serialVersionUID = 1L;

	private static transient final Logger log = LoggerFactory.getLogger(BalanceData.class);

	private static transient final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	private double value;
	private String msisdn;
	private Double limit;
	private Date date;

	public BalanceData(String msisdn, double balance, Date date) {
		this.value = balance;
		this.msisdn = msisdn;
		this.date = date;
		log.trace("new BalanceData (" + msisdn + ", " + balance + ", " + date + ")");
	}

	public BalanceData(String msisdn, String balanceStr, String balanceDate) throws ParseException, NumberFormatException {
		this.msisdn = msisdn;
		this.value = Double.valueOf(balanceStr);
		this.date = simpleDateFormat.parse(balanceDate);
	}

	public BalanceData() {
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double balance) {
		this.value = balance;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public Double getLimit() {
		return limit;
	}

	public void setLimit(Double limit) {
		this.limit = limit;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("BalanceData { msisdn:");
		sb.append(msisdn);
		sb.append(", balance: ");
		sb.append(value);
		sb.append(", limit: ");
		sb.append(limit);
		sb.append(", date: ");
		sb.append(simpleDateFormat.format(date));
		sb.append(" }");
		return sb.toString();
	}

}

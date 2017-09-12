package com.db.awmd.challenge.domain;

import java.math.BigDecimal;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MoneyTransfer {

	@NotNull
	@NotEmpty
	private final String accountFrom;
	
	@NotNull
	@NotEmpty
	private final String accountTo;
	
	@NotNull
	@Min(value = 0, message = "amount must be positive.")
	private final BigDecimal amount;
	
	@JsonCreator
	public MoneyTransfer(@JsonProperty("accountFrom") String accountFrom,
	  @JsonProperty("accountTo") String accountTo,	
	  @JsonProperty("amount") BigDecimal amount) {
	  this.accountFrom = accountFrom;
	  this.accountTo = accountTo;
	  this.amount = amount;
	}
}

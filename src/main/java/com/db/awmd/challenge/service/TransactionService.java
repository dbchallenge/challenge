package com.db.awmd.challenge.service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.MoneyTransfer;
import com.db.awmd.challenge.exception.FailedTransactionException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.exception.NegativeOrEmptyAmountException;

import lombok.Getter;

@Service
public class TransactionService {

	@Getter
	private final AccountsService accountsService;
	
	@Autowired
	public TransactionService(AccountsService accountsService) {
		this.accountsService = accountsService;
	}

	public void transferMoney(final MoneyTransfer moneyTransfer) {
		Account accountFrom = this.accountsService.getAccount(moneyTransfer.getAccountFrom());
		Account accountTo = this.accountsService.getAccount(moneyTransfer.getAccountTo());
		if(moneyTransfer.getAmount() == null || (moneyTransfer.getAmount() != null && moneyTransfer.getAmount().
				compareTo(BigDecimal.ZERO) == -1)) {
			throw new NegativeOrEmptyAmountException("Negative or empty amount amount " + moneyTransfer.getAccountTo());
		}
		if (accountFrom.getBalance() == null || (accountFrom.getBalance() != null && accountFrom.getBalance().
				compareTo(moneyTransfer.getAmount()) == -1)) {
			throw new InsufficientBalanceException("Insuffient balance account " + accountFrom.getAccountId());
		}
		boolean transactionCompleted = false;
		try {
			if (accountFrom.getLock().tryLock(20, TimeUnit.MILLISECONDS)
					&& accountTo.getLock().tryLock(20, TimeUnit.MILLISECONDS)) {
				accountFrom.withdraw(moneyTransfer.getAmount());
				accountTo.deposit(moneyTransfer.getAmount());
				transactionCompleted = true;
			}
		} catch (InterruptedException e) {
			// nop
		} finally {
			accountFrom.getLock().unlock();
			accountTo.getLock().unlock();
			if(!transactionCompleted) {
				throw new FailedTransactionException(
						String.format("Transfer from account %s to account %s " + "could not be completed",
								moneyTransfer.getAccountFrom(), moneyTransfer.getAccountTo()));
			}
		}
	}
}

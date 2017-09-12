package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.MoneyTransfer;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.exception.NegativeOrEmptyAmountException;
import com.db.awmd.challenge.exception.UnknowAccountException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import com.db.awmd.challenge.service.TransactionService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TransactionServiceTest {
	
	@Autowired
	TransactionService transactionService;
	
	@Autowired
	AccountsService accountsService;
	
	@MockBean
	NotificationService notificationService;

  @Before
  public void cleanStore() {
  	this.accountsService.getAccountsRepository().clearAccounts();
  }
	
  @Test
  public void transferMoney() throws Exception {
  	Account accountFrom = new Account("Id-1", new BigDecimal(100));
  	Account accountTo = new Account("id-2", new BigDecimal(10));
  	this.accountsService.createAccount(accountFrom);
  	this.accountsService.createAccount(accountTo);
  	MoneyTransfer moneyTransfer = new MoneyTransfer(accountFrom.getAccountId(), accountTo.getAccountId(), 
  			new BigDecimal(50));
  	this.transactionService.transferMoney(moneyTransfer);
  	
  	assertThat(this.accountsService.getAccount(accountFrom.getAccountId()).getBalance())
  	.isEqualTo(new BigDecimal(50));
  	assertThat(this.accountsService.getAccount(accountTo.getAccountId()).getBalance())
  	.isEqualTo(new BigDecimal(60));
  	
  	this.notificationService.notifyAboutTransfer(accountFrom, String.format("Amount %s has been sucessfully transferred to %s " , 
  			moneyTransfer.getAmount(), moneyTransfer.getAccountTo()));
  	this.notificationService.notifyAboutTransfer(accountTo, String.format("Amount %s has been received from %s ",
  			moneyTransfer.getAmount(), moneyTransfer.getAccountFrom()));
  }
  
  @Test
  public void transferMoney_failsOnNegativeAmount() throws Exception {
  	Account accountFrom = new Account("Id-1", new BigDecimal(100));
  	Account accountTo = new Account("id-2", new BigDecimal(10));
  	this.accountsService.createAccount(accountFrom);
  	this.accountsService.createAccount(accountTo);
  	MoneyTransfer moneyTransfer = new MoneyTransfer(accountFrom.getAccountId(), accountTo.getAccountId(), 
  			new BigDecimal(-100));
  	try {
  	this.transactionService.transferMoney(moneyTransfer);
  	fail("Should have failed due to negative amount");
  	} catch (NegativeOrEmptyAmountException ex) {
  		assertThat(ex.getMessage()).isEqualTo("Negative or empty amount amount " + moneyTransfer.getAccountTo());
  		assertThat(this.accountsService.getAccount(accountFrom.getAccountId()).getBalance())
  		.isEqualTo(new BigDecimal(100));
  		assertThat(this.accountsService.getAccount(accountTo.getAccountId()).getBalance())
  		.isEqualTo(new BigDecimal(10));
  		
    	this.notificationService.notifyAboutTransfer(accountFrom, String.format("Amount %s has not been transferred to %s " , 
    			moneyTransfer.getAmount(), moneyTransfer.getAccountTo()));
    	this.notificationService.notifyAboutTransfer(accountTo, String.format("Amount %s has not been received from %s ",
    			moneyTransfer.getAmount(), moneyTransfer.getAccountFrom()));

  	}
  	
  }
  
  @Test
  public void transferMoney_failsOnInsufficientBalance() throws Exception {
  	Account accountFrom = new Account("Id-1", new BigDecimal(10));
  	Account accountTo = new Account("id-2", new BigDecimal(10));
  	this.accountsService.createAccount(accountFrom);
  	this.accountsService.createAccount(accountTo);
  	MoneyTransfer moneyTransfer = new MoneyTransfer(accountFrom.getAccountId(), accountTo.getAccountId(), 
  			new BigDecimal(50));
  	try {
  		this.transactionService.transferMoney(moneyTransfer);
  		fail("Should have failed due to insuffient account balance");
  	} catch(InsufficientBalanceException ex) {
  		assertThat(ex.getMessage()).isEqualTo("Insuffient balance account " + accountFrom.getAccountId());
    	this.notificationService.notifyAboutTransfer(accountFrom, String.format("Amount %s could not be "
    			+ "transferred to %s due to insufficient account balance" , 
    			moneyTransfer.getAmount(), moneyTransfer.getAccountTo()));
    	this.notificationService.notifyAboutTransfer(accountTo, String.format("Amount %s has not been received from %s ",
    			moneyTransfer.getAmount(), moneyTransfer.getAccountFrom()));

  	}
  }
  
  @Test
  public void transferMoney_failsOnUnknownAccount() throws Exception {
  	MoneyTransfer moneyTransfer = new MoneyTransfer(new Account("Id-1").getAccountId(), new Account("Id-2").getAccountId(), 
  			new BigDecimal(50));
  	try {
  		this.transactionService.transferMoney(moneyTransfer);
  		fail("Should have failed due to unknown account");
  	} catch(UnknowAccountException ex) {
  		assertThat(ex.getMessage()).isEqualTo("Account Id-1 does not exist");
  	}
  }
  
  @Test
  public void transferMoney_Concurrent() throws Exception {
  	Account accountFrom = new Account("Id-1", new BigDecimal(100));
  	Account accountTo = new Account("id-2", new BigDecimal(10));
  	Account accountFromSecond = new Account("id-7", new BigDecimal(120));
  	this.accountsService.createAccount(accountFrom);
  	this.accountsService.createAccount(accountTo);
  	this.accountsService.createAccount(accountFromSecond);
  	MoneyTransfer moneyTransferFirst = new MoneyTransfer(accountFrom.getAccountId(), accountTo.getAccountId(), 
  			new BigDecimal(50));
  	MoneyTransfer moneyTransferSecond = new MoneyTransfer(accountFromSecond.getAccountId(), accountTo.getAccountId(), 
  			new BigDecimal(50));
  	final CyclicBarrier gate = new CyclicBarrier(3);
  	Runnable transferOne = () -> { try {
			gate.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		} this.transactionService.transferMoney(moneyTransferFirst); };
  	Runnable transferTwo = () -> { try {
			gate.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		} this.transactionService.transferMoney(moneyTransferSecond); }; 
  	Thread threadOne = new Thread(transferOne);
  	Thread threadTwo = new Thread(transferTwo);  
  	threadOne.start();
  	threadTwo.start();
  	gate.await();
  	threadOne.join();
  	threadTwo.join();
  	assertThat(this.accountsService.getAccount(accountFrom.getAccountId()).getBalance())
  	.isEqualTo(new BigDecimal(50));
  	assertThat(this.accountsService.getAccount(accountTo.getAccountId()).getBalance())
  	.isEqualTo(new BigDecimal(110));
  	assertThat(this.accountsService.getAccount(accountFromSecond.getAccountId()).getBalance())
  	.isEqualTo(new BigDecimal(70));
  	this.notificationService.notifyAboutTransfer(accountFrom, String.format("Amount %s has been sucessfully transferred to %s " , 
  			moneyTransferFirst.getAmount(), moneyTransferFirst.getAccountTo()));
  	this.notificationService.notifyAboutTransfer(accountTo, String.format("Amount %s has been received from %s ",
  			moneyTransferFirst.getAmount(), moneyTransferFirst.getAccountFrom()));
  	this.notificationService.notifyAboutTransfer(accountFrom, String.format("Amount %s has been sucessfully transferred to %s " , 
  			moneyTransferSecond.getAmount(), moneyTransferSecond.getAccountTo()));
  	this.notificationService.notifyAboutTransfer(accountTo, String.format("Amount %s has been received from %s ",
  			moneyTransferSecond.getAmount(), moneyTransferSecond.getAccountFrom()));

  }

}

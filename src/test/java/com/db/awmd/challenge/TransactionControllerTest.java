package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TransactionControllerTest {

	private MockMvc mockMvc;

	@Autowired
	private AccountsService accountsService;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Before
	public void prepareMockMvc() {
		this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

		// Reset the existing accounts before each test.
		accountsService.getAccountsRepository().clearAccounts();
	}

	@Test
	public void transferMoney() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-456\",\"balance\":200}")).andExpect(status().isCreated());

		this.mockMvc.perform(post("/v1/transfer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountFrom\":\"Id-123\",\"accountTo\":\"Id-456\",\"amount\":500.00}")).andExpect(status().isOk());

		Account accountFrom = accountsService.getAccount("Id-123");
		Account accountTo = accountsService.getAccount("Id-456");
		assertThat(accountFrom.getBalance()).isEqualByComparingTo("500");
		assertThat(accountTo.getBalance()).isEqualByComparingTo("700");
	}

	@Test
	public void transferMoneyNegativeAmount() throws Exception {
		this.mockMvc.perform(post("/v1/transfer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountFrom\":\"Id-123\",\"accountTo\":\"Id-456\",\"amount\":-500.00}")).andExpect(status().isBadRequest());
	}

	@Test
	public void transferMoneyNoAmount() throws Exception {
		this.mockMvc.perform(post("/v1/transfer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountFrom\":\"Id-123\",\"accountTo\":\"Id-456\",\"amount\":}")).andExpect(status().isBadRequest());
	}

	@Test
	public void transferMoneyInsufficientBalance() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":300}")).andExpect(status().isCreated());
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-456\",\"balance\":200}")).andExpect(status().isCreated());

		this.mockMvc.perform(post("/v1/transfer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountFrom\":\"Id-123\",\"accountTo\":\"Id-456\",\"amount\":500.00}")).andExpect(status().isBadRequest());
	}

	@Test
	public void transferMoneyEmptySender() throws Exception {
		this.mockMvc.perform(post("/v1/transfer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountFrom\":\"\",\"accountTo\":\"Id-456\",\"amount\":-500.00}")).andExpect(status().isBadRequest());
	}

	@Test
	public void transferMoneyEmptyReceiver() throws Exception {
		this.mockMvc.perform(post("/v1/transfer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountFrom\":\"Id-123\",\"accountTo\":\"\",\"amount\":-500.00}")).andExpect(status().isBadRequest());
	}

	@Test
	public void transferMoneyEmptySenderReceiver() throws Exception {
		this.mockMvc.perform(post("/v1/transfer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountFrom\":\"Id-123\",\"accountTo\":\"\",\"amount\":-500.00}")).andExpect(status().isBadRequest());
	}

	@Test
	public void transferMoneyNoBody() throws Exception {
		this.mockMvc.perform(post("/v1/transfer").contentType(MediaType.APPLICATION_JSON))
		.andExpect(status().isBadRequest());
	}

}

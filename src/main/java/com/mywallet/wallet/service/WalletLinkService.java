package com.mywallet.wallet.service;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.mywallet.wallet.client.BankLinkClient;
import com.mywallet.wallet.client.UserServiceClient;
import com.mywallet.wallet.entity.WalletAccount;
import com.mywallet.wallet.entity.WalletLinkAttempt;
import com.mywallet.wallet.exception.AccountAlreadyLinkedException;
import com.mywallet.wallet.exception.InvalidLinkCodeException;
import com.mywallet.wallet.exception.InvalidRequestException;
import com.mywallet.wallet.exception.WalletAlreadyExistsException;
import com.mywallet.wallet.security.AccessToken;

/**
 * Pas de @Transactional ici. L'insertion du wallet tient dans une transaction de WalletCreator :
 * si elle viole une contrainte, cette transaction est condamnée, et le nom de contrainte est traduit ensuite.
 */
@Service
public class WalletLinkService {

	/**
	 * Noms réels confirmés : user_id dans pg_constraint, bank_account_number nommé ainsi dans V3.
	 */
	static final String USER_ID_UNIQUE = "wallet_account_user_id_key";
	static final String BANK_ACCOUNT_UNIQUE = "wallet_account_bank_account_number_key";

	private static final Logger log = LoggerFactory.getLogger(WalletLinkService.class);

	private final AccessToken accessToken;
	private final UserServiceClient userServiceClient;
	private final BankLinkClient bankLinkClient;
	private final WalletCreator walletCreator;
	private final LinkAttemptStore attempts;

	public WalletLinkService(
			AccessToken accessToken,
			UserServiceClient userServiceClient,
			BankLinkClient bankLinkClient,
			WalletCreator walletCreator,
			LinkAttemptStore attempts) {
		this.accessToken = accessToken;
		this.userServiceClient = userServiceClient;
		this.bankLinkClient = bankLinkClient;
		this.walletCreator = walletCreator;
		this.attempts = attempts;
	}

	public BankLinkClient.Initiation initiate(UUID userId, String accountNumber) {
		if (accountNumber == null || !accountNumber.matches("\\d{12}")) {
			throw new InvalidRequestException();
		}
		if (walletCreator.findUserWallet(userId).isPresent()) {
			throw new WalletAlreadyExistsException();
		}
		attempts.rejectIfLimited(userId);
		String phone = userServiceClient.getProfile(accessToken.bearer());
		BankLinkClient.Initiation initiation = bankLinkClient.initiate(accountNumber, phone);
		attempts.save(initiation.linkId(), userId, accountNumber);
		return initiation;
	}

	public LinkedWallet confirm(UUID userId, UUID linkId, String code) {
		if (linkId == null || code == null || !code.matches("\\d{6}")) {
			throw new InvalidLinkCodeException();
		}
		WalletLinkAttempt attempt = attempts.find(linkId)
				.filter(row -> userId.equals(row.getUserId()))
				.orElseThrow(InvalidLinkCodeException::new);
		String accountNumber = normalize(attempt.getAccountNumber());
		Optional<WalletAccount> existing = walletCreator.findUserWallet(userId);
		if (existing.isPresent()) {
			if (sameAccount(existing.get(), accountNumber)) {
				attempts.delete(linkId);
				return new LinkedWallet(existing.get(), false);
			}
			throw new WalletAlreadyExistsException();
		}
		if (walletCreator.bankAccountTaken(accountNumber)) {
			throw new AccountAlreadyLinkedException();
		}
		BankLinkClient.Confirmation confirmation = bankLinkClient.confirm(linkId, code);
		if (!confirmation.verified() || !accountNumber.equals(normalize(confirmation.accountNumber()))) {
			throw new InvalidLinkCodeException();
		}
		try {
			WalletAccount created = walletCreator.createLinked(userId, accountNumber, linkId);
			return new LinkedWallet(created, true);
		}
		catch (DataIntegrityViolationException exception) {
			return translate(exception, userId, accountNumber, linkId);
		}
	}

	private LinkedWallet translate(
			DataIntegrityViolationException exception,
			UUID userId,
			String accountNumber,
			UUID linkId) {
		String constraint = DataIntegrityConstraints.name(exception);
		if (BANK_ACCOUNT_UNIQUE.equals(constraint)) {
			throw new AccountAlreadyLinkedException();
		}
		if (USER_ID_UNIQUE.equals(constraint)) {
			Optional<WalletAccount> wallet = walletCreator.findUserWallet(userId);
			if (wallet.isPresent() && sameAccount(wallet.get(), accountNumber)) {
				attempts.delete(linkId);
				return new LinkedWallet(wallet.get(), false);
			}
			if (wallet.isPresent()) {
				throw new WalletAlreadyExistsException();
			}
		}
		log.warn("Contrainte d'intégrité non traduite: {}", constraint);
		throw new IllegalStateException("Erreur d'intégrité.");
	}

	private static boolean sameAccount(WalletAccount wallet, String accountNumber) {
		return accountNumber.equals(normalize(wallet.getBankAccountNumber()));
	}

	private static String normalize(String accountNumber) {
		return accountNumber == null ? "" : accountNumber.trim();
	}

}

package com.mywallet.wallet.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mywallet.wallet.entity.Direction;
import com.mywallet.wallet.entity.EntryType;
import com.mywallet.wallet.entity.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

	Optional<LedgerEntry> findByAccountIdAndTransactionRefAndEntryType(
			UUID accountId,
			String transactionRef,
			EntryType entryType);

	@Query("""
			select sum(e.amount) from LedgerEntry e
			where e.accountId = :accountId
			  and e.entryType = :entryType
			  and e.direction = :direction
			  and e.createdAt >= :from
			""")
	BigDecimal sumSince(
			@Param("accountId") UUID accountId,
			@Param("entryType") EntryType entryType,
			@Param("direction") Direction direction,
			@Param("from") Instant from);

}

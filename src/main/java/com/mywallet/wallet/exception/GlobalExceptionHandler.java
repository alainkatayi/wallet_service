package com.mywallet.wallet.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(WalletNotFoundException.class)
	public ProblemDetail handleWalletNotFound(HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Introuvable", "Wallet introuvable.", "WALLET_NOT_FOUND", request);
	}

	@ExceptionHandler(UnauthenticatedException.class)
	public ProblemDetail handleUnauthenticated(HttpServletRequest request) {
		return problem(HttpStatus.UNAUTHORIZED, "Non autorisé", "Authentification requise.", "UNAUTHENTICATED", request);
	}

	@ExceptionHandler(ForbiddenException.class)
	public ProblemDetail handleForbidden(HttpServletRequest request) {
		return problem(HttpStatus.FORBIDDEN, "Interdit", "Accès refusé.", "FORBIDDEN", request);
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error("Erreur interne inattendue", exception);
		return problem(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Erreur interne",
				"Une erreur interne est survenue.",
				"INTERNAL_ERROR",
				request);
	}

	private static ProblemDetail problem(
			HttpStatus status,
			String title,
			String detail,
			String code,
			HttpServletRequest request) {
		return ProblemDetails.of(status, title, detail, code, request);
	}

}

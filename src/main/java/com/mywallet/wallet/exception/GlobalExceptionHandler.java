package com.mywallet.wallet.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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

	@ExceptionHandler(InvalidLinkCodeException.class)
	public ProblemDetail handleInvalidLinkCode(HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "Requête invalide", "Code de liaison invalide", "INVALID_LINK_CODE", request);
	}

	@ExceptionHandler(WalletAlreadyExistsException.class)
	public ProblemDetail handleWalletAlreadyExists(HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "Conflit", "Un wallet existe déjà.", "WALLET_ALREADY_EXISTS", request);
	}

	@ExceptionHandler(AccountAlreadyLinkedException.class)
	public ProblemDetail handleAccountAlreadyLinked(HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "Conflit", "Ce compte est déjà lié.", "ACCOUNT_ALREADY_LINKED", request);
	}

	@ExceptionHandler(TooManyRequestsException.class)
	public ResponseEntity<ProblemDetail> handleTooManyRequests(
			TooManyRequestsException exception,
			HttpServletRequest request) {
		ProblemDetail body = problem(
				HttpStatus.TOO_MANY_REQUESTS,
				"Trop de demandes",
				"Trop de demandes, réessayez plus tard.",
				"TOO_MANY_REQUESTS",
				request);
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(body);
	}

	@ExceptionHandler(BankUnavailableException.class)
	public ProblemDetail handleBankUnavailable(HttpServletRequest request) {
		return problem(
				HttpStatus.SERVICE_UNAVAILABLE,
				"Service indisponible",
				"La banque est indisponible.",
				"BANK_UNAVAILABLE",
				request);
	}

	@ExceptionHandler(UserServiceUnavailableException.class)
	public ProblemDetail handleUserServiceUnavailable(HttpServletRequest request) {
		return problem(
				HttpStatus.SERVICE_UNAVAILABLE,
				"Service indisponible",
				"Le service utilisateur est indisponible.",
				"USER_SERVICE_UNAVAILABLE",
				request);
	}

	@ExceptionHandler(InvalidRequestException.class)
	public ProblemDetail handleInvalidRequest(HttpServletRequest request) {
		return invalidRequest(request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleInvalidBody(HttpServletRequest request) {
		if (confirm(request)) {
			return handleInvalidLinkCode(request);
		}
		return invalidRequest(request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleUnreadable(HttpServletRequest request) {
		if (confirm(request)) {
			return handleInvalidLinkCode(request);
		}
		return invalidRequest(request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ProblemDetail handleMethodNotSupported(HttpServletRequest request) {
		return problem(
				HttpStatus.METHOD_NOT_ALLOWED,
				"Méthode non autorisée",
				"Méthode non autorisée.",
				"METHOD_NOT_ALLOWED",
				request);
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

	private static ProblemDetail invalidRequest(HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "Requête invalide", "La requête est invalide.", "VALIDATION_ERROR", request);
	}

	private static boolean confirm(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri != null && uri.endsWith("/api/wallets/link/confirm");
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

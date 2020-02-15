package com.meemaw.auth.service.signup;

import com.meemaw.auth.datasource.user.UserDatasource;
import com.meemaw.auth.model.signup.TeamInviteCreateIdentified;
import com.meemaw.auth.model.signup.dto.SignupCompleteRequestDTO;
import com.meemaw.auth.model.signup.Signup;
import com.meemaw.auth.model.signup.SignupRequest;
import com.meemaw.auth.model.signup.dto.SignupVerifyRequestDTO;
import com.meemaw.auth.model.signup.dto.TeamInviteDTO;
import com.meemaw.shared.rest.exception.DatabaseException;
import com.meemaw.shared.rest.response.Boom;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.ReactiveMailer;
import io.quarkus.qute.Template;
import io.quarkus.qute.api.ResourcePath;
import io.vertx.axle.pgclient.PgPool;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PgSignupService implements SignupService {

  private static final Logger log = LoggerFactory.getLogger(PgSignupService.class);

  @ResourcePath("signup/welcome")
  Template welcomeTemplate;

  @ResourcePath("org/invite")
  Template inviteTemplate;

  @Inject
  ReactiveMailer mailer;

  @Inject
  PgPool pgPool;

  @Inject
  UserDatasource userDatasource;

  private static final String FROM_SUPPORT = "Insight Support <support@insight.com>";

  private CompletionStage<Void> sendWelcomeEmail(SignupRequest signupRequest) {
    String email = signupRequest.getEmail();

    return welcomeTemplate
        .data("email", email)
        .data("orgId", signupRequest.getOrg())
        .data("token", signupRequest.getToken())
        .renderAsync()
        .thenCompose(html -> mailer
            .send(Mail.withHtml(email, "Welcome to Insight", html).setFrom(FROM_SUPPORT)));
  }

  private CompletionStage<Void> sendInviteEmail(UUID token,
      TeamInviteCreateIdentified teamInvite) {
    String email = teamInvite.getEmail();

    return inviteTemplate
        .data("creator", teamInvite.getCreator())
        .data("email", email)
        .data("orgId", teamInvite.getOrg())
        .data("token", token)
        .renderAsync()
        .thenCompose(html -> mailer
            .send(Mail.withHtml(email, "You've been invited to Insight", html)
                .setFrom(FROM_SUPPORT)));
  }

  public CompletionStage<Boolean> verifySignupRequestExists(
      SignupVerifyRequestDTO verifySignup) {
    return userDatasource.verifySignupExists(verifySignup);
  }

  public CompletionStage<SignupRequest> signup(final String email) {
    log.info("signup request email={}", email);

    return pgPool.begin().thenCompose(transaction -> userDatasource
        .createOrganization(transaction, new SignupRequest(email))
        .thenCompose(org -> userDatasource.createUser(transaction, org))
        .thenCompose(req -> userDatasource.createSignupRequest(transaction, req))
        .thenCompose(req -> sendWelcomeEmail(req)
            .exceptionally(throwable -> {
              transaction.rollback();
              log.error("Failed to send signup email={}", email, throwable);
              throw Boom.serverError().message("Failed to send signup email").exception();
            })
            .thenCompose(x -> transaction.commit())
            .thenApply(x -> {
              log.info("signup complete email={} userId={} org={}", email, req.getUserId(),
                  req.getOrg());
              return req;
            })
            .exceptionally(throwable -> {
              log.error("Failed to commit signup transaction email={}", email, throwable);
              throw new DatabaseException();
            })));
  }

  public CompletionStage<Boolean> completeSignup(
      SignupCompleteRequestDTO completeSignup) {
    String email = completeSignup.getEmail();
    String org = completeSignup.getOrg();
    UUID token = completeSignup.getToken();

    return pgPool.begin().thenCompose(transaction -> {
      log.info("signupComplete starting transaction email={} token={}", email, token);

      return userDatasource.findSignup(transaction, completeSignup)
          .thenApply(maybeSignup -> {
            Signup signup = maybeSignup.orElseThrow(() -> {
              log.info("Signup request does not exist email={} org={} token={}", email, org, token);
              throw Boom.badRequest().message("Signup request does not exist.").exception();
            });

            Instant lastActive = signup.getCreatedAt().plusDays(1).toInstant();
            if (Instant.now().isAfter(lastActive)) {
              log.info("Signup request expired email={} org={} token={}", email, org, token);
              throw Boom.badRequest().message("Signup request expired").exception();
            }

            return signup;
          })
          .thenCompose(signup -> {
            log.info("Deleting existing signup requests email={} org={}", email, org);
            return userDatasource.deleteSignupRequests(transaction, signup)
                .thenApply(isDeleted -> {
                  if (!isDeleted) {
                    log.info("Failed to delete signup requests email={} org={}", email, org);
                    throw new DatabaseException();
                  }
                  return signup;
                });
          })
          .thenCompose(signup -> {
            UUID userId = signup.getUserId();
            log.info("Storing password email={} userId={} org={}", email, userId, org);
            String hashedPassword = BCrypt
                .hashpw(completeSignup.getPassword(), BCrypt.gensalt(13));

            return userDatasource.storePassword(transaction, userId, hashedPassword)
                .thenApply(x -> transaction.commit())
                .thenApply(x -> {
                  log.info("signup complete email={} userId={} org={}", email, userId, org);
                  return true;
                });
          });
    });
  }

  @Override
  public CompletionStage<TeamInviteDTO> invite(TeamInviteCreateIdentified teamInviteCreate) {
    String email = teamInviteCreate.getEmail();
    return pgPool.begin()
        .thenCompose(transaction -> userDatasource.storeInvite(transaction, teamInviteCreate)
            .thenCompose(teamInvite -> sendInviteEmail(teamInvite.getToken(), teamInviteCreate)
                .exceptionally(throwable -> {
                  transaction.rollback();
                  log.error("Failed to send invite email={}", email, throwable);
                  throw Boom.serverError().message("Failed to send invite email").exception();
                })
                .thenCompose(x -> transaction.commit())
                .thenApply(x -> teamInvite)));
  }
}

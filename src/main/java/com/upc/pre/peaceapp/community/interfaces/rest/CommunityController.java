package com.upc.pre.peaceapp.community.interfaces.rest;

import com.upc.pre.peaceapp.community.domain.model.commands.*;
import com.upc.pre.peaceapp.community.domain.model.queries.GetCommentsByReportQuery;
import com.upc.pre.peaceapp.community.domain.model.queries.GetVoteSummaryQuery;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagTargetType;
import com.upc.pre.peaceapp.community.domain.services.CommentCommandService;
import com.upc.pre.peaceapp.community.domain.services.CommentQueryService;
import com.upc.pre.peaceapp.community.domain.services.ModerationCommandService;
import com.upc.pre.peaceapp.community.domain.services.VoteCommandService;
import com.upc.pre.peaceapp.community.domain.services.VoteQueryService;
import com.upc.pre.peaceapp.community.interfaces.rest.resources.*;
import com.upc.pre.peaceapp.community.interfaces.rest.transform.CommunityResourceAssemblers;
import com.upc.pre.peaceapp.shared.interfaces.rest.AuthenticatedRequester;
import com.upc.pre.peaceapp.shared.interfaces.rest.PagedResource;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Participación de la comunidad: comentarios, votos de confirmación y denuncias. Todos los endpoints piden token;
 * quien actúa sale del token. Los errores los traduce {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Community", description = "Comments, votes and abuse flags")
public class CommunityController {
    private static final int MAX_PAGE_SIZE = 100;

    private final CommentCommandService commentCommandService;
    private final CommentQueryService commentQueryService;
    private final VoteCommandService voteCommandService;
    private final VoteQueryService voteQueryService;
    private final ModerationCommandService moderationCommandService;

    public CommunityController(CommentCommandService commentCommandService, CommentQueryService commentQueryService,
                               VoteCommandService voteCommandService, VoteQueryService voteQueryService,
                               ModerationCommandService moderationCommandService) {
        this.commentCommandService = commentCommandService;
        this.commentQueryService = commentQueryService;
        this.voteCommandService = voteCommandService;
        this.voteQueryService = voteQueryService;
        this.moderationCommandService = moderationCommandService;
    }

    @PostMapping("/reports/{reportId}/comments")
    public ResponseEntity<CommentResource> addComment(@PathVariable Long reportId,
                                                      @Valid @RequestBody CreateCommentResource resource,
                                                      Authentication authentication) {
        var comment = commentCommandService.handle(
                new AddCommentCommand(reportId, authentication.getName(), resource.text()));
        return ResponseEntity.created(URI.create("/api/v1/reports/" + reportId + "/comments"))
                .body(CommunityResourceAssemblers.toResource(comment));
    }

    /** Del más antiguo al más nuevo, como se lee una conversación. */
    @GetMapping("/reports/{reportId}/comments")
    public ResponseEntity<PagedResource<CommentResource>> getComments(@PathVariable Long reportId,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size) {
        var comments = commentQueryService.handle(new GetCommentsByReportQuery(reportId, pageable(page, size)));
        return ResponseEntity.ok(PagedResource.from(comments, CommunityResourceAssemblers::toResource));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, Authentication authentication) {
        commentCommandService.handle(new DeleteCommentCommand(commentId, authentication.getName(),
                AuthenticatedRequester.isAdmin(authentication)));
        return ResponseEntity.noContent().build();
    }

    /** Crea o cambia el voto del usuario sobre el reporte. */
    @PutMapping("/reports/{reportId}/vote")
    public ResponseEntity<VoteSummaryResource> vote(@PathVariable Long reportId,
                                                    @Valid @RequestBody CastVoteResource resource,
                                                    Authentication authentication) {
        return ResponseEntity.ok(CommunityResourceAssemblers.toResource(voteCommandService.handle(
                new CastVoteCommand(reportId, authentication.getName(), resource.type()))));
    }

    @DeleteMapping("/reports/{reportId}/vote")
    public ResponseEntity<VoteSummaryResource> retractVote(@PathVariable Long reportId, Authentication authentication) {
        return ResponseEntity.ok(CommunityResourceAssemblers.toResource(
                voteCommandService.handle(new RetractVoteCommand(reportId, authentication.getName()))));
    }

    @GetMapping("/reports/{reportId}/votes")
    public ResponseEntity<VoteSummaryResource> getVotes(@PathVariable Long reportId, Authentication authentication) {
        return ResponseEntity.ok(CommunityResourceAssemblers.toResource(
                voteQueryService.handle(new GetVoteSummaryQuery(reportId, authentication.getName()))));
    }

    @PostMapping("/reports/{reportId}/flag")
    public ResponseEntity<AbuseFlagResource> flagReport(@PathVariable Long reportId,
                                                        @Valid @RequestBody FlagContentResource resource,
                                                        Authentication authentication) {
        return flag(FlagTargetType.REPORT, reportId, resource, authentication);
    }

    @PostMapping("/comments/{commentId}/flag")
    public ResponseEntity<AbuseFlagResource> flagComment(@PathVariable Long commentId,
                                                         @Valid @RequestBody FlagContentResource resource,
                                                         Authentication authentication) {
        return flag(FlagTargetType.COMMENT, commentId, resource, authentication);
    }

    private ResponseEntity<AbuseFlagResource> flag(FlagTargetType type, Long targetId, FlagContentResource resource,
                                                   Authentication authentication) {
        var flag = moderationCommandService.handle(new FlagContentCommand(type, targetId, authentication.getName(),
                resource.reason(), resource.details()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CommunityResourceAssemblers.toResource(flag));
    }

    private static org.springframework.data.domain.Pageable pageable(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page no puede ser negativo");
        if (size < 1 || size > MAX_PAGE_SIZE)
            throw new IllegalArgumentException("size debe estar entre 1 y " + MAX_PAGE_SIZE);
        return PageRequest.of(page, size, Sort.by("createdAt").ascending().and(Sort.by("id").ascending()));
    }
}

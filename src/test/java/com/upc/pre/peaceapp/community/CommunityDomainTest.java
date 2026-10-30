package com.upc.pre.peaceapp.community;

import com.upc.pre.peaceapp.community.domain.exceptions.AbuseFlagAlreadyResolvedException;
import com.upc.pre.peaceapp.community.domain.model.aggregates.AbuseFlag;
import com.upc.pre.peaceapp.community.domain.model.aggregates.ReportComment;
import com.upc.pre.peaceapp.community.domain.model.aggregates.ReportVote;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.*;
import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportCategory;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommunityDomainTest {

    @Test
    void commentTrimsTextAndRejectsEmptyOrTooLong() {
        assertThat(new ReportComment(1L, 2L, "  hola  ").getText()).isEqualTo("hola");
        assertThatThrownBy(() -> new ReportComment(1L, 2L, "  ")).hasMessage("El comentario no puede estar vacío");
        assertThatThrownBy(() -> new ReportComment(1L, 2L, "x".repeat(501)))
                .hasMessage("El comentario no puede superar 500 caracteres");
        assertThatThrownBy(() -> new ReportComment(null, 2L, "hola")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removingACommentIsIdempotent() {
        var comment = new ReportComment(1L, 2L, "hola");
        var first = Instant.parse("2026-01-01T00:00:00Z");
        comment.remove(first);
        comment.remove(first.plusSeconds(60));
        assertThat(comment.isRemoved()).isTrue();
        assertThat(comment.getRemovedAt()).isEqualTo(first);
    }

    @Test
    void voteCanChangeStance() {
        var vote = new ReportVote(1L, 2L, VoteType.CONFIRM);
        vote.changeTo(VoteType.DISPUTE);
        assertThat(vote.getType()).isEqualTo(VoteType.DISPUTE);
        assertThatThrownBy(() -> vote.changeTo(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void voteSummaryComparesNetConfirmationsWithThreshold() {
        assertThat(new VoteSummary(3, 0, null, 3).reachedThreshold()).isTrue();
        assertThat(new VoteSummary(4, 1, null, 3).reachedThreshold()).isTrue();
        assertThat(new VoteSummary(4, 2, null, 3).reachedThreshold()).isFalse();
        assertThat(new VoteSummary(1, 3, VoteType.DISPUTE, 3).net()).isEqualTo(-2);
    }

    @Test
    void flagIsResolvedOnlyOnce() {
        var flag = new AbuseFlag(FlagTargetType.REPORT, 1L, 2L, FlagReason.SPAM, " detalle ");
        assertThat(flag.isOpen()).isTrue();
        assertThat(flag.getDetails()).isEqualTo("detalle");

        flag.resolve(FlagStatus.DISMISSED, 9L, "sin falta", Instant.now());
        assertThat(flag.getStatus()).isEqualTo(FlagStatus.DISMISSED);
        assertThat(flag.getResolvedBy()).isEqualTo(9L);
        assertThatThrownBy(() -> flag.resolve(FlagStatus.ACTION_TAKEN, 9L, null, Instant.now()))
                .isInstanceOf(AbuseFlagAlreadyResolvedException.class);
    }

    @Test
    void flagRejectsOpenAsResolution() {
        var flag = new AbuseFlag(FlagTargetType.COMMENT, 1L, 2L, FlagReason.OFFENSIVE, null);
        assertThatThrownBy(() -> flag.resolve(FlagStatus.OPEN, 9L, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbuseFlag(FlagTargetType.COMMENT, 1L, 2L, null, null))
                .hasMessage("El motivo es obligatorio");
    }

    @Test
    void communityVerificationOnlyAdvancesPendingOrUnderReviewReports() {
        var pending = newReport();
        assertThat(pending.verifyByCommunity()).isTrue();
        assertThat(pending.getStatus()).isEqualTo(ReportStatus.VERIFIED);
        assertThat(pending.verifyByCommunity()).isFalse();

        var discarded = newReport();
        discarded.changeStatus(ReportStatus.DISCARDED);
        assertThat(discarded.verifyByCommunity()).isFalse();
        assertThat(discarded.getStatus()).isEqualTo(ReportStatus.DISCARDED);
    }

    private static Report newReport() {
        return new Report(1L, "Robo", null, ReportCategory.THEFT, null, null, GeoPoint.of(-12.05, -77.04), "Av. Lima", null);
    }
}

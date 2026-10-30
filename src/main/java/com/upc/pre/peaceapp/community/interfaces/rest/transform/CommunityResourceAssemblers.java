package com.upc.pre.peaceapp.community.interfaces.rest.transform;

import com.upc.pre.peaceapp.community.domain.model.aggregates.AbuseFlag;
import com.upc.pre.peaceapp.community.domain.model.aggregates.ModerationAction;
import com.upc.pre.peaceapp.community.domain.model.aggregates.ReportComment;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.CommunityMetrics;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteSummary;
import com.upc.pre.peaceapp.community.interfaces.rest.resources.*;

/** Conversiones de los agregados y objetos de valor de comunidad a sus recursos REST. */
public final class CommunityResourceAssemblers {
    private CommunityResourceAssemblers() {
    }

    public static CommentResource toResource(ReportComment comment) {
        return new CommentResource(comment.getId(), comment.getReportId(), comment.getAuthorId(), comment.getText(),
                comment.getCreatedAt().toInstant());
    }

    public static VoteSummaryResource toResource(VoteSummary summary) {
        return new VoteSummaryResource(summary.confirmations(), summary.disputes(), summary.net(), summary.myVote(),
                summary.verificationThreshold());
    }

    public static AbuseFlagResource toResource(AbuseFlag flag) {
        return new AbuseFlagResource(flag.getId(), flag.getTargetType(), flag.getTargetId(), flag.getReporterId(),
                flag.getReason(), flag.getDetails(), flag.getStatus(), flag.getResolvedBy(), flag.getResolutionNote(),
                flag.getCreatedAt().toInstant(), flag.getResolvedAt());
    }

    public static ModerationActionResource toResource(ModerationAction action) {
        return new ModerationActionResource(action.getId(), action.getModeratorId(), action.getType(),
                action.getTargetType(), action.getTargetId(), action.getNote(), action.getCreatedAt().toInstant());
    }

    public static CommunityMetricsResource toResource(CommunityMetrics metrics) {
        var reports = metrics.reports();
        var reviewed = reports.verified() + reports.discarded();
        var resolvedFlags = metrics.flagsDismissed() + metrics.flagsActionTaken();
        return new CommunityMetricsResource(
                new CommunityMetricsResource.Reports(reports.total(), reports.countByStatus(), reports.verified(),
                        reports.discarded(), reviewed == 0 ? null : (double) reports.verified() / reviewed,
                        reports.averageAttentionHours()),
                new CommunityMetricsResource.Community(metrics.comments(), metrics.confirmations(), metrics.disputes()),
                new CommunityMetricsResource.Flags(metrics.flagsOpen(), metrics.flagsDismissed(),
                        metrics.flagsActionTaken(), metrics.flagsTotal(),
                        resolvedFlags == 0 ? null : (double) metrics.flagsActionTaken() / resolvedFlags));
    }
}

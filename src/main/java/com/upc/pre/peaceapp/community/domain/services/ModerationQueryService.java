package com.upc.pre.peaceapp.community.domain.services;

import com.upc.pre.peaceapp.community.domain.model.aggregates.AbuseFlag;
import com.upc.pre.peaceapp.community.domain.model.aggregates.ModerationAction;
import com.upc.pre.peaceapp.community.domain.model.queries.GetAbuseFlagsQuery;
import com.upc.pre.peaceapp.community.domain.model.queries.GetCommunityMetricsQuery;
import com.upc.pre.peaceapp.community.domain.model.queries.GetModerationLogQuery;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.CommunityMetrics;
import org.springframework.data.domain.Page;

/** Consultas del panel de administración: todas exigen ser administrador. */
public interface ModerationQueryService {
    Page<AbuseFlag> handle(GetAbuseFlagsQuery query);

    Page<ModerationAction> handle(GetModerationLogQuery query);

    CommunityMetrics handle(GetCommunityMetricsQuery query);
}

package com.upc.pre.peaceapp.community.domain.model.queries;

import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagStatus;
import org.springframework.data.domain.Pageable;

/** {@code status} nulo trae todas las denuncias. */
public record GetAbuseFlagsQuery(boolean admin, FlagStatus status, Pageable pageable) {
}

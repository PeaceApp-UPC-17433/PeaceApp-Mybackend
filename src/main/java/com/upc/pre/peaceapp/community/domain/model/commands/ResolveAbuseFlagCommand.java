package com.upc.pre.peaceapp.community.domain.model.commands;

import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagStatus;

/** {@code resolution} es {@code DISMISSED} (sin falta) o {@code ACTION_TAKEN} (se retira el contenido). */
public record ResolveAbuseFlagCommand(Long flagId, String requesterEmail, boolean admin, FlagStatus resolution,
                                      String note) {
}

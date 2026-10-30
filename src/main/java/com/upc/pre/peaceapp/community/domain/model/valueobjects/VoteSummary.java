package com.upc.pre.peaceapp.community.domain.model.valueobjects;

/**
 * Resultado de las votaciones de un reporte.
 *
 * @param myVote voto de quien consulta; nulo si no votó
 */
public record VoteSummary(long confirmations, long disputes, VoteType myVote, int verificationThreshold) {
    /** Confirmaciones menos dudas: es lo que se compara con el umbral de verificación. */
    public long net() {
        return confirmations - disputes;
    }

    public boolean reachedThreshold() {
        return net() >= verificationThreshold;
    }
}

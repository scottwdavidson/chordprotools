package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.SetlistEvaluationReport;

public interface EvaluateSetlistUseCase {

    /**
     * @param gigParam gig slug, or null/blank to default to the latest gig
     *     (same resolution rule as {@code export-setlist}).
     */
    SetlistEvaluationReport evaluateSetlist(String gigParam);
}

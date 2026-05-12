package com.pourchoices.chordpro.adapter.out.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XML DTO for one RC-500 {@code <ASSIGNn>} element.
 *
 * <p>The RC-500 supports 8 assignable controllers per memory slot (ASSIGN1–ASSIGN8),
 * each with the same 6 fields.  All defaults reflect the factory-reset state
 * (controller disabled, no source or target mapped).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rc500AssignDto {

    /** 0 = controller assignment disabled. */
    @Builder.Default int sw         = 0;
    @Builder.Default int source     = 0;
    @Builder.Default int sourceMode = 0;
    @Builder.Default int target     = 0;
    @Builder.Default int targetMin  = 0;
    @Builder.Default int targetMax  = 1;
}

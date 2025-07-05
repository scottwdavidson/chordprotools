package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import java.util.List;

@Value
@Builder
public class ChordProFileListing {

    @Singular
    List<String> chordProFileNames;
}

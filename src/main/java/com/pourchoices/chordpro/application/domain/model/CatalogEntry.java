package com.pourchoices.chordpro.application.domain.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
public class CatalogEntry {

    @NonNull
    String chordProFilename;
    @NonNull
    String title;
    @NonNull
    String artist;
    @NonNull
    String key;
    @NonNull
    String duration;
    String tempo;
    String timeSignature;
    String capo;
    String nord;
    String version;
    String countin;
    String backing;
    String ve;
    String performanceKey;

}

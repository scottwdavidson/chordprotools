package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvBindByName;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CatalogEntryDto {

    @CsvBindByName(column = "chordProFilename")
    String chordProFilename;
    @CsvBindByName(column = "title")
    String title;
    @CsvBindByName(column = "artist")
    String artist;
    @CsvBindByName(column = "key")
    String key;
    @CsvBindByName(column = "duration")
    String duration;
    @CsvBindByName(column = "tempo")
    String tempo;
    @CsvBindByName(column = "time signature")
    String timeSignature;
    @CsvBindByName(column = "capo")
    String capo;
    @CsvBindByName(column = "nord")
    String nord;
    @CsvBindByName(column = "version")
    String version;
    @CsvBindByName(column = "countin")
    String countin;
    @CsvBindByName(column = "backing")
    String backing;
    @CsvBindByName(column = "ve")
    String ve;
    @CsvBindByName(column = "performance key")
    String performanceKey;

}

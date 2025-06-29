package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvBindByName;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CatalogEntryDto {

    @CsvBindByName(column = "title")
    String title;
    @CsvBindByName(column = "artist")
    String artist;
    @CsvBindByName(column = "key")
    String key;
    @CsvBindByName(column = "tempo")
    String tempo;
    @CsvBindByName(column = "duration")
    String duration;
    @CsvBindByName(column = "nord")
    String nord;
    @CsvBindByName(column = "backing")
    String backing;
    @CsvBindByName(column = "bbVolume")
    String bbVolume;
    @CsvBindByName(column = "capo")
    String capo;
    @CsvBindByName(column = "ve")
    String ve;

}

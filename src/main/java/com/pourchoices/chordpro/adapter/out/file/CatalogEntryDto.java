package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvBindByName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogEntryDto {

    public static final List<String> CATALOG_COLUMN_ORDER = Arrays.asList(

    "title","artist","key","duration","tempo",
    "countin","backing","nord","ve",
    "performance key",
    "time signature","capo","version","chordpro filename");


    @CsvBindByName(column = "chordpro filename")
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

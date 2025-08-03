package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.adapter.out.file.ChordProFileReader;
import com.pourchoices.chordpro.adapter.out.file.ChordProFileWriter;
import com.pourchoices.chordpro.application.domain.model.*;
import com.pourchoices.chordpro.application.domain.port.in.UpdateSongUseCase;
import com.pourchoices.chordpro.application.domain.port.in.UpdateSongsUseCase;
import com.pourchoices.chordpro.application.domain.port.out.CatalogPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
public class UpdateSongsService implements UpdateSongsUseCase {

    private final ReadSongListService readSongListService;
    private final UpdateSongService updateSongService;

    @Override
    public void updateSongs(String chordproSongPathString) {

        ChordProFileListing chordProFileListing = readSongListService.readSongList(chordproSongPathString);

        for (String chordProSongPathString : chordProFileListing.getChordProFileNames()) {

            this.updateSongService.updateSong(chordProSongPathString);
        }
    }
}

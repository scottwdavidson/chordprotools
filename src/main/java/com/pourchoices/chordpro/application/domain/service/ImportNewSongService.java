package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.port.in.ImportNewSongUseCase;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
public class ImportNewSongService implements ImportNewSongUseCase {


    @Override
    public void importNewSong(String chordproSongPathString) {

        // import the song catalog

        // import the song


    }
}

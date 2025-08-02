package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.port.in.UpdateCatalogUseCase;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
public class UpdateCatalogService implements UpdateCatalogUseCase {


    @Override
    public void updateCatalog(String chordproSongPathString) {

    }
}

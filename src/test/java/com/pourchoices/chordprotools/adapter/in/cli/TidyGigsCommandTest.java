package com.pourchoices.chordprotools.adapter.in.cli;

import com.pourchoices.chordpro.adapter.in.file.TidyGigsCommand;
import com.pourchoices.chordpro.application.port.in.TidyGigsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TidyGigsCommandTest {

    @Mock
    private TidyGigsUseCase tidyGigsUseCase;

    @InjectMocks
    private TidyGigsCommand command;

    @Test
    void callsUseCaseAndReturnsZero() {
        Integer result = command.call();

        verify(tidyGigsUseCase).tidyGigs();
        assertThat(result).isZero();
    }
}
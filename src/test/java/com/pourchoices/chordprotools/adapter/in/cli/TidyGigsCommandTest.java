package com.pourchoices.chordprotools.adapter.in.cli;

import com.pourchoices.chordpro.adapter.in.file.TidyGigsCommand;
import com.pourchoices.chordpro.application.domain.model.GigsRowRepair;
import com.pourchoices.chordpro.application.domain.model.TidyGigsResult;
import com.pourchoices.chordpro.application.port.in.TidyGigsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TidyGigsCommandTest {

    @Mock
    private TidyGigsUseCase tidyGigsUseCase;

    @InjectMocks
    private TidyGigsCommand command;

    @Test
    void tidiedSuccessfully_returnsZero() {
        when(tidyGigsUseCase.tidyGigs())
                .thenReturn(TidyGigsResult.builder().tidied(true).build());

        Integer result = command.call();

        verify(tidyGigsUseCase).tidyGigs();
        assertThat(result).isZero();
    }

    @Test
    void fileMissingOrEmpty_returnsZero() {
        when(tidyGigsUseCase.tidyGigs())
                .thenReturn(TidyGigsResult.builder().fileMissingOrEmpty(true).build());

        Integer result = command.call();

        assertThat(result).isZero();
    }

    @Test
    void rejectedRows_returnsOne() {
        when(tidyGigsUseCase.tidyGigs())
                .thenReturn(TidyGigsResult.builder()
                        .rejectedRow(new GigsRowRepair.RejectedRow(42, "too,many,fields,here,oops"))
                        .build());

        Integer result = command.call();

        assertThat(result).isEqualTo(1);
    }
}

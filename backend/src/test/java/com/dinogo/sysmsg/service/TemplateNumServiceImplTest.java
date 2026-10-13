package com.dinogo.sysmsg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.dinogo.sysmsg.entity.MsgFunctionSequenceEntity;
import com.dinogo.sysmsg.repository.MsgFunctionSequenceRepository;
import com.dinogo.sysmsg.service.impl.TemplateNumServiceImpl;

class TemplateNumServiceImplTest {
    private final MsgFunctionSequenceRepository repository = mock(MsgFunctionSequenceRepository.class);
    private final TemplateNumServiceImpl service = new TemplateNumServiceImpl(repository);

    @ParameterizedTest
    @MethodSource("prefixes")
    void generatesAllSixPrefixes(String input, String normalized) {
        MsgFunctionSequenceEntity sequence = mock(MsgFunctionSequenceEntity.class);
        when(sequence.getCurrentValue()).thenReturn(0);
        when(repository.findById(normalized)).thenReturn(Optional.of(sequence));

        assertEquals(normalized + "-001", service.generateMsgFunction(input));
        verify(sequence).setCurrentValue(1);
        verify(repository).save(sequence);
    }

    @Test
    void generates999ButRejectsTheNextNumber() {
        MsgFunctionSequenceEntity sequence = mock(MsgFunctionSequenceEntity.class);
        when(repository.findById("OA")).thenReturn(Optional.of(sequence));
        when(sequence.getCurrentValue()).thenReturn(998, 999);

        assertEquals("OA-999", service.generateMsgFunction("OA"));
        assertThrows(IllegalStateException.class, () -> service.generateMsgFunction("OA"));
        verify(sequence, never()).setCurrentValue(1000);
    }

    private static Stream<Arguments> prefixes() {
        return Stream.of("OA", "OC", "OS", "AC", "AS", "SC")
                .map(prefix -> Arguments.of(prefix.toLowerCase(), prefix));
    }
}

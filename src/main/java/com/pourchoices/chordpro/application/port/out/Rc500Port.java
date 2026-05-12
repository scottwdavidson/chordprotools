package com.pourchoices.chordpro.application.port.out;

import com.pourchoices.chordpro.application.domain.model.Rc500MemoryBank;

import java.nio.file.Path;

/**
 * Outbound port for RC-500 memory-bank file I/O.
 *
 * <p>The RC-500 looper pedal exports its state as {@code MEMORY1.RC0} /
 * {@code MEMORY2.RC0} XML files.  Implementations of this port are responsible
 * for parsing those files into the {@link Rc500MemoryBank} domain model and for
 * writing an updated bank back to disk.
 *
 * <p>The write operation uses a <em>read-modify-write</em> strategy: it reads the
 * existing file first so that all RC-500 settings that the domain model does not
 * explicitly model (loop FX, rhythm, assignable controllers, etc.) are preserved
 * verbatim.  Only the fields that the caller supplies via {@code updatedBank} are
 * changed.
 */
public interface Rc500Port {

    /**
     * Reads an RC-500 memory bank file and returns its contents as a domain object.
     *
     * @param rc0FilePath path to the {@code .RC0} file (must exist and be readable)
     * @return populated {@link Rc500MemoryBank} — never {@code null}
     * @throws com.pourchoices.chordpro.adapter.out.file.Rc500ParseException
     *         if the file cannot be parsed as a valid RC-500 memory bank
     */
    Rc500MemoryBank readMemoryBank(Path rc0FilePath);

    /**
     * Writes an updated memory bank back to the given RC0 file path.
     *
     * <p>The adapter performs a <em>read-modify-write</em>: it parses the existing
     * file to obtain the original per-slot XML data, overlays only the name and
     * track fields present in {@code updatedBank}, then writes the complete bank
     * back out so that no RC-500 settings are inadvertently reset.
     *
     * @param rc0FilePath path of the {@code .RC0} file to overwrite
     * @param updatedBank domain object whose slot names (and track data) should be
     *                    persisted; slots absent from this bank are left unchanged
     */
    void writeMemoryBank(Path rc0FilePath, Rc500MemoryBank updatedBank);
}

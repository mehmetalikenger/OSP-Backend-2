package org.offitec.osp.infrastructure.bootstrap.frdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * One-time seeder for the Frascold catalogue. Runs only when {@code osp.frdata.import-path} points
 * to a readable frdata-export.json, so normal startup is unaffected. Re-runnable: the import skips
 * compressor models that already exist.
 *
 * <p>Enable with e.g. {@code OSP_FRDATA_IMPORT_PATH=C:/Users/mak/Desktop/fulls/frdata-export.json}
 * (or {@code --osp.frdata.import-path=...}).</p>
 */
@Component
@Order(100) // after AdminBootstrap and schema creation
public class FrdataImportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FrdataImportRunner.class);

    private final FrdataImportService importService;
    private final String importPath;

    public FrdataImportRunner(FrdataImportService importService,
                              @org.springframework.beans.factory.annotation.Value("${osp.frdata.import-path:}") String importPath) {
        this.importService = importService;
        this.importPath = importPath;
    }

    @Override
    public void run(String... args) {
        if (importPath == null || importPath.isBlank()) {
            return; // not requested
        }
        Path path = Path.of(importPath);
        if (!Files.isReadable(path)) {
            log.warn("osp.frdata.import-path is set but file is not readable: {}", path);
            return;
        }
        try {
            log.info("Starting Frascold catalogue import from {}", path);
            FrdataImportService.Report r = importService.importFrom(path);
            log.info("""
                    Frascold import complete:
                      compressors imported : {}
                      compressors skipped  : {} (already present)
                      ratings imported     : {}
                      ratings non-calc     : {} (refrigerant has no CoolProp mapping yet)
                      ratings skipped      : {} no-refrigerant, {} duplicate
                      unsupported refrigerants : {}""",
                    r.compressorsImported(), r.compressorsSkipped(),
                    r.ratingsImported(), r.ratingsNonCalculable(),
                    r.ratingsSkippedNoRefrigerant(), r.ratingsSkippedDuplicate(),
                    r.unsupportedRefrigerants());
        } catch (Exception e) {
            log.error("Frascold catalogue import failed", e);
        }
    }
}

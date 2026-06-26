package org.offitec.osp.infrastructure.bootstrap.frdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.offitec.osp.domain.entity.Compressor;
import org.offitec.osp.domain.entity.CompressorRating;
import org.offitec.osp.domain.entity.Refrigerant;
import org.offitec.osp.domain.enums.CompressorKind;
import org.offitec.osp.infrastructure.repository.CompressorRepository;
import org.offitec.osp.infrastructure.repository.RefrigerantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Loads frdata-export.json (produced by tools/extract_frdata.ps1) into the catalogue: upserts
 * refrigerants, then seeds {@link Compressor} models and their per-refrigerant {@link CompressorRating}
 * rows. Idempotent on the FSS3 source key — re-running skips compressors already imported.
 *
 * <p>The loop holds no managed state across compressors: it preloads the keys it needs once, then
 * flushes and clears the persistence context after each compressor. This keeps inserts streaming
 * (no autoflush re-writing a growing context, no O(n^2) blow-up from the mutable jsonb arrays).</p>
 */
@Service
public class FrdataImportService {

    private static final Logger log = LoggerFactory.getLogger(FrdataImportService.class);

    private final CompressorRepository compressorRepo;
    private final RefrigerantRepository refrigerantRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager em;

    public FrdataImportService(CompressorRepository compressorRepo, RefrigerantRepository refrigerantRepo) {
        this.compressorRepo = compressorRepo;
        this.refrigerantRepo = refrigerantRepo;
    }

    public record Report(int compressorsImported, int compressorsSkipped,
                         int ratingsImported, int ratingsNonCalculable,
                         int ratingsSkippedNoRefrigerant, int ratingsSkippedDuplicate,
                         List<String> unsupportedRefrigerants) {}

    /** Refrigerant id + whether CoolProp can model it, captured as primitives so it survives em.clear(). */
    private record RefRef(Long id, boolean calculable) {}

    @Transactional
    public Report importFrom(Path jsonPath) throws IOException {
        FrdataExport export = objectMapper.readValue(Files.newInputStream(jsonPath), FrdataExport.class);
        log.info("Parsed frdata export: {} compressors / {} ratings", export.compressorCount(), export.ratingCount());

        Map<String, RefRef> refByCode = upsertRefrigerants(export);
        Set<Integer> existingKeys = new HashSet<>(compressorRepo.findAllSrcKeys());
        em.clear(); // start the main loop with a clean context

        int compImported = 0, compSkipped = 0, ratImported = 0, ratNonCalc = 0;
        int ratSkippedNoRef = 0, ratSkippedDup = 0;
        TreeSet<String> unsupported = new TreeSet<>();

        for (FrdataExport.Comp c : export.compressors()) {
            if (c.name() == null || c.name().isBlank()) continue;
            // Idempotency on the FSS3 key, not the model name: the same name can belong to several
            // genuinely different compressors (e.g. three-phase vs single-phase variants).
            if (c.key() != null && existingKeys.contains(c.key())) {
                compSkipped++;
                continue;
            }

            Compressor comp = toCompressor(c);
            em.persist(comp);
            compImported++;

            if (c.ratings() != null) {
                Set<Long> seenRefrigerants = new HashSet<>();
                for (FrdataExport.Rating r : c.ratings()) {
                    RefRef ref = r.refrigerant() == null ? null : refByCode.get(r.refrigerant().trim());
                    if (ref == null) { ratSkippedNoRef++; continue; }          // slot with no assigned refrigerant
                    if (!seenRefrigerants.add(ref.id())) { ratSkippedDup++; continue; }
                    if (!ref.calculable()) { unsupported.add(r.refrigerant().trim()); ratNonCalc++; }

                    CompressorRating cr = toRating(comp, em.getReference(Refrigerant.class, ref.id()), r, ref.calculable());
                    em.persist(cr);
                    ratImported++;
                }
            }
            // flush this compressor's graph and detach it so the context stays small
            em.flush();
            em.clear();
            if (c.key() != null) existingKeys.add(c.key());
        }

        Report report = new Report(compImported, compSkipped, ratImported, ratNonCalc,
                ratSkippedNoRef, ratSkippedDup, new java.util.ArrayList<>(unsupported));
        log.info("frdata import done: {} compressors imported, {} skipped, {} ratings imported "
                        + "({} non-calculable, {} skipped no-refrigerant, {} skipped duplicate). Unsupported refrigerants: {}",
                compImported, compSkipped, ratImported, ratNonCalc, ratSkippedNoRef, ratSkippedDup, unsupported);
        return report;
    }

    /** Ensure every refrigerant in the export exists with its CoolProp mapping; return code -> (id, calculable). */
    private Map<String, RefRef> upsertRefrigerants(FrdataExport export) {
        Map<String, Refrigerant> existing = new HashMap<>();
        for (Refrigerant r : refrigerantRepo.findAll()) {
            if (r.getCode() != null) existing.put(r.getCode().trim(), r);
        }
        Map<String, String> nameToLib = new HashMap<>();
        for (FrdataExport.Comp c : export.compressors()) {
            if (c.ratings() == null) continue;
            for (FrdataExport.Rating r : c.ratings()) {
                if (r.refrigerant() != null) nameToLib.putIfAbsent(r.refrigerant().trim(), r.refrigerantLib());
            }
        }
        Map<String, RefRef> result = new HashMap<>();
        for (Map.Entry<String, String> e : nameToLib.entrySet()) {
            String code = e.getKey();
            Refrigerant ref = existing.get(code);
            if (ref == null) {
                ref = new Refrigerant();
                ref.setName(code);
                ref.setCode(code);
            }
            ref.setLibrary(e.getValue());
            ref.setCoolpropName(RefrigerantCatalog.coolpropName(code));
            ref = refrigerantRepo.save(ref);
            result.put(code, new RefRef(ref.getId(), ref.getCoolpropName() != null));
        }
        em.flush();
        return result;
    }

    private Compressor toCompressor(FrdataExport.Comp c) {
        Compressor comp = new Compressor();
        comp.setBrand("Frascold");
        comp.setSrcKey(c.key());
        comp.setModel(c.name());
        comp.setType(inferKind(c));
        comp.setFrascoldType(c.type());
        comp.setMddKey(c.mddKey());
        comp.setModelDescription(c.modelDesc());
        comp.setDisplacement(c.displacement());
        comp.setPistonCount(c.pistonCount());
        comp.setTDisMax(c.tDisMax());
        comp.setNominalHp(c.nominalHp());
        comp.setOilA(c.oilA());
        comp.setOilB(c.oilB());
        comp.setOilC(c.oilC());
        comp.setImported(true);
        comp.setDeleted(false);
        return comp;
    }

    private CompressorRating toRating(Compressor comp, Refrigerant ref, FrdataExport.Rating r, boolean calculable) {
        CompressorRating cr = new CompressorRating();
        cr.setCompressor(comp);
        cr.setRefrigerant(ref);
        cr.setCapCoeffs(r.capCoeffs());
        cr.setPowerCoeffs(r.powerCoeffs());
        cr.setMassCoeffs(r.massCoeffs());
        cr.setUseMassCap(r.useMassCap());
        cr.setEco(r.eco());
        cr.setCalculable(calculable);

        FrdataExport.RefCond rc = r.referenceCondition();
        if (rc != null) {
            cr.setOhRef(rc.ohRef());
            cr.setTaspRef(rc.taspRef());
            cr.setScRef(rc.scRef());
            cr.setTliqRef(rc.tliqRef());
        }

        FrdataExport.CapCtrl cc = r.capControl();
        if (cc != null) {
            cr.setMinFrequency(cc.minFrequency());
            cr.setMaxFrequency(cc.maxFrequency());
            cr.setMinSpeed(cc.minSpeed());
            cr.setMaxSpeed(cc.maxSpeed());
            if (cc.frequencyCoef() != null) {
                cr.setFreqCapCoeffs(cc.frequencyCoef().cap());
                cr.setFreqPowerCoeffs(cc.frequencyCoef().pow());
                cr.setFreqCurrentCoeffs(cc.frequencyCoef().curr());
            }
        }

        if (r.envelope() != null && r.envelope().points() != null) {
            List<FrdataExport.Pt> pts = r.envelope().points();
            double[][] poly = new double[pts.size()][2];
            for (int i = 0; i < pts.size(); i++) {
                poly[i][0] = pts.get(i).x();
                poly[i][1] = pts.get(i).y();
            }
            cr.setEnvelope(poly);
        }
        return cr;
    }

    /** Inverter families become ISCR; everything else defaults to reciprocating. */
    private CompressorKind inferKind(FrdataExport.Comp c) {
        String desc = c.modelDesc() == null ? "" : c.modelDesc().toLowerCase();
        String name = c.name() == null ? "" : c.name().toLowerCase();
        boolean inverter = desc.contains("inverter") || name.contains("-vs") || name.contains("vs)");
        return inverter ? CompressorKind.ISCR : CompressorKind.RC;
    }
}

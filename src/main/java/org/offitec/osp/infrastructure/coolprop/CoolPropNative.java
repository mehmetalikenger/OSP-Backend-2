package org.offitec.osp.infrastructure.coolprop;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * In-process binding to the CoolProp C shared library via the Java 25 Foreign Function & Memory API
 * (Project Panama). Binds the single high-level entry point
 * {@code double PropsSI(const char* Output, const char* Name1, double Prop1, const char* Name2, double Prop2, const char* Fluid)}
 * which is sufficient for every thermodynamic property the cycle engine needs.
 *
 * <p>The platform-correct native library (CoolProp.dll on Windows, libCoolProp.so on Linux) is
 * shipped under {@code resources/native} and extracted to a temp file at startup, then loaded by
 * absolute path. An external override is available via {@code coolprop.library.path}.</p>
 *
 * <p>CoolProp's high-level interface is not guaranteed thread-safe, so native calls are serialized.</p>
 */
@Component
public class CoolPropNative {

    private static final Logger log = LoggerFactory.getLogger(CoolPropNative.class);

    private final String configuredPath;
    private final Object lock = new Object();
    private final Arena arena = Arena.ofShared();

    private MethodHandle propsSI;
    private boolean available;

    public CoolPropNative(@Value("${coolprop.library.path:}") String configuredPath) {
        this.configuredPath = configuredPath;
    }

    @PostConstruct
    void init() {
        try {
            Path lib = resolveLibrary();
            SymbolLookup lookup = SymbolLookup.libraryLookup(lib, arena);
            Linker linker = Linker.nativeLinker();
            this.propsSI = linker.downcallHandle(
                    lookup.find("PropsSI").orElseThrow(() -> new IllegalStateException("PropsSI not exported by " + lib)),
                    FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE,
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE,
                            ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE, ValueLayout.ADDRESS));
            this.available = true;
            log.info("CoolProp native library loaded from {}", lib);
        } catch (Exception e) {
            this.available = false;
            log.error("CoolProp native library could not be loaded — refrigerant property calls will fail", e);
        }
    }

    /** Whether the native library loaded successfully. */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Calls CoolProp PropsSI. All values are SI: T[K], P[Pa], H[J/kg], S[J/kg/K], D[kg/m3],
     * Q[0..1], molar_mass[kg/mol]. Returns NaN on a CoolProp error (the library reports failures
     * by returning NaN for the high-level call).
     */
    public double propsSI(String output, String name1, double prop1, String name2, double prop2, String fluid) {
        if (!available) {
            throw new IllegalStateException("CoolProp native library is not available");
        }
        synchronized (lock) {
            try (Arena call = Arena.ofConfined()) {
                MemorySegment out = call.allocateFrom(output);
                MemorySegment n1 = call.allocateFrom(name1);
                MemorySegment n2 = call.allocateFrom(name2);
                MemorySegment fl = call.allocateFrom(fluid);
                return (double) propsSI.invoke(out, n1, prop1, n2, prop2, fl);
            } catch (Throwable t) {
                throw new RuntimeException("CoolProp PropsSI(" + output + ";" + name1 + "," + prop1
                        + ";" + name2 + "," + prop2 + ";" + fluid + ") failed", t);
            }
        }
    }

    private Path resolveLibrary() throws Exception {
        if (configuredPath != null && !configuredPath.isBlank()) {
            Path p = Path.of(configuredPath);
            if (Files.isReadable(p)) return p;
            log.warn("coolprop.library.path set but not readable: {} — falling back to bundled library", p);
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        String resource;
        String tmpName;
        if (os.contains("win")) {
            resource = "/native/CoolProp.dll";
            tmpName = "CoolProp.dll";
        } else if (os.contains("nux") || os.contains("nix")) {
            resource = "/native/libCoolProp.so";
            tmpName = "libCoolProp.so";
        } else {
            throw new IllegalStateException("No bundled CoolProp library for OS: " + os);
        }
        try (InputStream in = CoolPropNative.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("Bundled CoolProp library not found on classpath: " + resource);
            Path dir = Files.createTempDirectory("coolprop");
            Path target = dir.resolve(tmpName);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            target.toFile().deleteOnExit();
            return target;
        }
    }
}

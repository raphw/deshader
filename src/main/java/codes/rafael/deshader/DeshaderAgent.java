package codes.rafael.deshader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

public class DeshaderAgent {

    public static void premain(String arg, Instrumentation instrumentation) {
        Map<String, String> reshades = new HashMap<>();
        for (String pair : arg.split(",")) {
            String[] elements = pair.split("/");
            if (elements.length != 2) {
                throw new IllegalArgumentException("Argument usage: <actual package>/<new package>");
            }
            reshades.put(elements[0].replace('.', '/'), elements[1].replace('.', '/'));
        }
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) {
                // No need to even consider platform and boot classes.
                if (loader == null || className == null || loader == ClassLoader.getSystemClassLoader().getParent()) {
                    return null;
                }
                // We transform the class by replacing all package names.
                ClassReader reader = new ClassReader(classfileBuffer);
                ClassWriter writer = new ClassWriter(reader, 0);
                StateTrackingRemapper remapper = new StateTrackingRemapper(reshades);
                reader.accept(new ClassRemapper(writer, remapper), 0);
                // Only if we remapped a package, we need to signal a transformation.
                return remapper.changed ? writer.toByteArray() : null;
            }
        });
    }

    static class StateTrackingRemapper extends Remapper {

        private final Map<String, String> reshades;

        boolean changed;

        StateTrackingRemapper(Map<String, String> reshades) {
            this.reshades = reshades;
        }

        @Override
        public String map(String internalName) {
            for (Map.Entry<String, String> entry : reshades.entrySet()) {
                if (internalName.startsWith(entry.getKey())) {
                    changed = true;
                    return entry.getValue() + internalName.substring(entry.getKey().length());
                }
            }
            return internalName;
        }

        @Override
        public String mapPackageName(String name) {
            for (Map.Entry<String, String> entry : reshades.entrySet()) {
                if (name.startsWith(entry.getKey())) {
                    changed = true;
                    return entry.getValue() + name.substring(entry.getKey().length());
                }
            }
            return name;

        }
    }
}

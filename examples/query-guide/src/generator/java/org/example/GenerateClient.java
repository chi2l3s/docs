package org.example;

import io.github.thirtyeighttwentysix.volan.codegen.VolanGenerator;
import io.github.thirtyeighttwentysix.volan.ir.SchemaLoader;
import io.github.thirtyeighttwentysix.volan.schema.SourceFile;
import java.nio.file.Files;
import java.nio.file.Path;

public class GenerateClient {
    public static void main(String[] args) throws Exception {
        var source = new SourceFile("schema.volan", Files.readString(Path.of(args[0])));
        VolanGenerator.writeTo(SchemaLoader.loadOrThrow(source), Path.of(args[1]));
        try (var fragments = Files.list(Path.of("schema-examples"))) {
            for (var fragment : fragments.sorted().toList()) {
                var sample = new SourceFile(fragment.toString(), Files.readString(fragment));
                VolanGenerator.writeTo(SchemaLoader.loadOrThrow(sample), Path.of(args[1]));
            }
        }
    }
}

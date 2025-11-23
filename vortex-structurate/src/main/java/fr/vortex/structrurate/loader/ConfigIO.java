package fr.vortex.structrurate.loader;

import fr.vortex.structrurate.node.ConfigNode;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigIO {

    private static final Logger log = Logger.getLogger(ConfigIO.class.getName());

    private final Yaml yaml;

    public ConfigIO() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setIndent(2);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        yaml = new Yaml(dumperOptions);
    }

    public record ReadResult(Map<String, Object> map, List<String> headerComments) {
    }

    @SuppressWarnings("unchecked")
    public ReadResult readWithHeader(Path file) {
        List<String> header = new ArrayList<>();
        if (Files.notExists(file)) return new ReadResult(new LinkedHashMap<>(), header);

        try (BufferedReader r = Files.newBufferedReader(file)) {
            String line;
            boolean inHeader = true;
            StringBuilder yamlBody = new StringBuilder();

            while ((line = r.readLine()) != null) {
                String trimmed = line.trim();
                if (inHeader) {
                    if (trimmed.startsWith("#")) {
                        header.add(line);
                    } else if (!trimmed.isEmpty()) {
                        inHeader = false;
                        yamlBody.append(line).append("\n");
                    }
                } else {
                    yamlBody.append(line).append("\n");
                }
            }

            Object data = !yamlBody.isEmpty() ? yaml.load(yamlBody.toString()) : null;
            Map<String, Object> map = data instanceof Map ? new LinkedHashMap<>((Map<String,Object>)data) : new LinkedHashMap<>();
            return new ReadResult(map, header);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to read config file " + file, e);
            return new ReadResult(new LinkedHashMap<>(), header);
        }
    }

    public void writeWithHeader(Path file, Map<String, Object> data, List<String> headerComments) {
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                if (headerComments != null) {
                    for (String h : headerComments) {
                        w.write(h);
                        w.newLine();
                    }
                    if (!headerComments.isEmpty()) {
                        w.newLine();
                    }
                }

                String dump = yaml.dump(data == null ? Collections.emptyMap() : data);
                w.write(dump);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to write config file " + file, e);
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    public ConfigNode readNode(Path file) {
        ReadResult r = readWithHeader(file);
        return new ConfigNode(r.map);
    }

    public List<String> readHeader(Path file) {
        ReadResult r = readWithHeader(file);
        return r.headerComments;
    }
}
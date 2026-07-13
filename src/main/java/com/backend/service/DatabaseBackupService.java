package com.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DatabaseBackupService {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${seal.backup.directory:backups}")
    private String backupDirectory;
    @Value("${seal.postgres.bin:}")
    private String configuredPostgresBin;

    public List<Map<String, Object>> listBackups() {
        Path directory = backupPath();
        try {
            Files.createDirectories(directory);
            try (var files = Files.list(directory)) {
                return files.filter(path -> path.getFileName().toString().endsWith(".dump"))
                        .sorted(Comparator.comparing(this::lastModified).reversed())
                        .map(this::metadata)
                        .toList();
            }
        } catch (IOException exception) {
            throw new RuntimeException("Không thể đọc thư mục sao lưu", exception);
        }
    }

    public Map<String, Object> createBackup() {
        Path directory = backupPath();
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve("seal-" + FILE_TIME.format(LocalDateTime.now()) + ".dump");
            DatabaseInfo database = databaseInfo();
            run(List.of(tool("pg_dump"), "--format=custom", "--no-owner", "--no-privileges",
                    "--host=" + database.host(), "--port=" + database.port(), "--username=" + username,
                    "--file=" + target, database.name()), password);
            return metadata(target);
        } catch (IOException exception) {
            throw new RuntimeException("Không thể tạo bản sao lưu", exception);
        }
    }

    public Map<String, Object> restore(String fileName) {
        if (!fileName.matches("[a-zA-Z0-9._-]+\\.dump")) {
            throw new RuntimeException("Tên bản sao lưu không hợp lệ");
        }
        Path source = backupPath().resolve(fileName).normalize();
        if (!source.startsWith(backupPath()) || !Files.exists(source)) {
            throw new RuntimeException("Không tìm thấy bản sao lưu");
        }
        DatabaseInfo database = databaseInfo();
        run(List.of(tool("pg_restore"), "--clean", "--if-exists", "--no-owner", "--no-privileges",
                "--host=" + database.host(), "--port=" + database.port(), "--username=" + username,
                "--dbname=" + database.name(), source.toString()), password);
        return metadata(source);
    }

    private void run(List<String> command, String databasePassword) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
            builder.environment().put("PGPASSWORD", databasePassword);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes());
            if (!process.waitFor(2, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new RuntimeException("Tiến trình PostgreSQL đã quá thời gian cho phép");
            }
            if (process.exitValue() != 0) {
                throw new RuntimeException("PostgreSQL backup/restore thất bại: " + output.trim());
            }
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Không thể chạy công cụ PostgreSQL", exception);
        }
    }

    private String tool(String name) {
        if (configuredPostgresBin != null && !configuredPostgresBin.isBlank()) {
            return Path.of(configuredPostgresBin, name + ".exe").toString();
        }
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) {
            Path postgres = Path.of(programFiles, "PostgreSQL");
            if (Files.isDirectory(postgres)) {
                try (var versions = Files.list(postgres)) {
                    Optional<Path> found = versions.sorted(Comparator.reverseOrder())
                            .map(version -> version.resolve("bin").resolve(name + ".exe"))
                            .filter(Files::exists).findFirst();
                    if (found.isPresent()) return found.get().toString();
                } catch (IOException ignored) {
                    // Fallback to PATH below.
                }
            }
        }
        return name;
    }

    private DatabaseInfo databaseInfo() {
        URI uri = URI.create(datasourceUrl.substring("jdbc:".length()));
        String name = uri.getPath().replaceFirst("^/", "");
        return new DatabaseInfo(uri.getHost(), uri.getPort() < 0 ? 5432 : uri.getPort(), name);
    }

    private Path backupPath() {
        return Path.of(backupDirectory).toAbsolutePath().normalize();
    }

    private Map<String, Object> metadata(Path path) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fileName", path.getFileName().toString());
            result.put("size", Files.size(path));
            result.put("createdAt", Files.getLastModifiedTime(path).toInstant());
            return result;
        } catch (IOException exception) {
            throw new RuntimeException("Không thể đọc thông tin bản sao lưu", exception);
        }
    }

    private long lastModified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException ignored) { return 0L; }
    }

    private record DatabaseInfo(String host, int port, String name) {}
}

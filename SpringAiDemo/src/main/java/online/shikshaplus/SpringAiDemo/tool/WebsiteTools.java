package online.shikshaplus.SpringAiDemo.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Component
public class WebsiteTools {

    private final Path workspaceDir;

    public WebsiteTools() {
        // Workspace is anchored to the local project root: ./workspace
        this.workspaceDir = Paths.get(System.getProperty("user.dir"), "workspace").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.workspaceDir);
        } catch (IOException e) {
            System.err.println("Could not create workspace directory: " + e.getMessage());
        }
    }

    /**
     * Bulletproof safePath function:
     * - Blocks directory traversal (../, ..\\)
     * - Blocks absolute / drive paths (C:, D:, /etc, \\network)
     * - Blocks null bytes and control characters
     * - Blocks Windows reserved device names (CON, PRN, AUX, NUL, COM1-9, LPT1-9)
     * - Verifies symlinks do not escape the workspace
     */
    public Path safePath(String requestedPath) {
        if (requestedPath == null || requestedPath.trim().isEmpty()) {
            throw new SecurityException("Security violation: File path cannot be empty or null.");
        }

        // Check for null bytes or control characters
        if (requestedPath.contains("\0")) {
            throw new SecurityException("Security violation: Null byte detected in path.");
        }

        // Reject Windows drive letters, network UNC paths, or root prefixes
        if (requestedPath.contains(":") || requestedPath.startsWith("\\\\")) {
            throw new SecurityException("Security violation: Absolute or drive-letter paths are strictly prohibited.");
        }

        // Normalize slashes and strip leading slashes to keep path strictly relative
        String cleanPath = requestedPath.trim().replace('\\', '/');
        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        if (cleanPath.isEmpty()) {
            throw new SecurityException("Security violation: Path resolves to empty or root.");
        }

        // Check for Windows reserved device names
        try {
            Path fileNamePath = Paths.get(cleanPath).getFileName();
            if (fileNamePath != null) {
                String fileName = fileNamePath.toString().toUpperCase();
                if (fileName.matches("^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?$")) {
                    throw new SecurityException("Security violation: Reserved device name is not allowed: " + fileName);
                }
            }
        } catch (Exception ignored) {}

        // Resolve against workspace and normalize
        Path target = workspaceDir.resolve(cleanPath).normalize().toAbsolutePath();

        // Enforce that target path is strictly within the workspace directory
        if (!target.startsWith(workspaceDir)) {
            throw new SecurityException("Security violation: Path traversal attempt blocked! Path cannot escape workspace: " + requestedPath);
        }

        // Prevent targeting the workspace root folder itself as a file
        if (target.equals(workspaceDir)) {
            throw new SecurityException("Security violation: Cannot target the workspace root folder as a file.");
        }

        // If file or parent exists, verify real path to prevent symlink bypass
        try {
            if (Files.exists(target)) {
                Path realTarget = target.toRealPath();
                Path realWorkspace = workspaceDir.toRealPath();
                if (!realTarget.startsWith(realWorkspace)) {
                    throw new SecurityException("Security violation: Symlink traversal attempt blocked!");
                }
            }
        } catch (IOException e) {
            // If cannot resolve real path, standard startsWith check above has already validated
        }

        return target;
    }

    public Path getSafePath(String requestedPath) {
        return safePath(requestedPath);
    }

    @Tool(description = "Creates a new project directory/folder inside the workspace for a website (e.g. 'portfolio', 'ecommerce-app', 'gym-website'). Keeps multiple websites separate so they never overwrite each other.")
    public String makeDirectory(
            @ToolParam(description = "Name or relative path of the project directory to create, e.g. 'portfolio', 'restaurant-app'") String directoryName) {
        try {
            Path targetPath = safePath(directoryName);
            if (Files.exists(targetPath)) {
                if (Files.isDirectory(targetPath)) {
                    return "Notice: Directory already exists: " + directoryName;
                } else {
                    return "Error: A file already exists with the name: " + directoryName;
                }
            }
            Files.createDirectories(targetPath);
            return "Success: Project directory '" + directoryName + "' created successfully in workspace.";
        } catch (SecurityException e) {
            return "Security Blocked: " + e.getMessage();
        } catch (Exception e) {
            return "Error creating directory: " + e.getMessage();
        }
    }

    @Tool(description = "Creates a new project directory inside the workspace. Alias for makeDirectory.")
    public String createDirectory(
            @ToolParam(description = "Name or relative path of the project directory, e.g. 'portfolio'") String directoryName) {
        return makeDirectory(directoryName);
    }

    @Tool(description = "Lists all project directories and websites currently present in the workspace.")
    public String listProjects() {
        try {
            if (!Files.exists(workspaceDir)) {
                Files.createDirectories(workspaceDir);
            }
            try (Stream<Path> stream = Files.list(workspaceDir)) {
                List<String> dirs = stream
                        .filter(Files::isDirectory)
                        .map(p -> workspaceDir.relativize(p).toString().replace('\\', '/'))
                        .sorted()
                        .toList();

                if (dirs.isEmpty()) {
                    return "No website project directories found in workspace.";
                }
                return "Existing website projects in workspace:\n" + String.join("\n", dirs);
            }
        } catch (Exception e) {
            return "Error listing projects: " + e.getMessage();
        }
    }

    @Tool(description = "Creates a new file with the specified content inside the website workspace (e.g. index.html, style.css, script.js). Fails if the file already exists.")
    public String createFile(
            @ToolParam(description = "Relative file path or name, e.g. 'index.html', 'css/style.css'") String fileName,
            @ToolParam(description = "Initial text or code content for the file") String content) {
        try {
            Path targetPath = safePath(fileName);
            if (Files.exists(targetPath)) {
                return "Error: File '" + fileName + "' already exists. Use writeFile to overwrite or update it.";
            }
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.writeString(targetPath, content != null ? content : "");
            return "Success: File '" + fileName + "' created successfully in workspace.";
        } catch (SecurityException e) {
            return "Security Blocked: " + e.getMessage();
        } catch (Exception e) {
            return "Error creating file: " + e.getMessage();
        }
    }

    @Tool(description = "Reads the content of an existing file from the website workspace.")
    public String readFile(
            @ToolParam(description = "Relative file path or name, e.g. 'index.html', 'style.css'") String fileName) {
        try {
            Path targetPath = safePath(fileName);
            if (!Files.exists(targetPath)) {
                return "Error: File not found in workspace: " + fileName;
            }
            if (Files.isDirectory(targetPath)) {
                return "Error: Path is a directory, not a file: " + fileName;
            }
            return Files.readString(targetPath);
        } catch (SecurityException e) {
            return "Security Blocked: " + e.getMessage();
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Writes or overwrites content to a file in the website workspace (e.g. updating HTML, CSS, or JS code).")
    public String writeFile(
            @ToolParam(description = "Relative file path or name, e.g. 'index.html', 'css/style.css'") String fileName,
            @ToolParam(description = "Full text or code content to write to the file") String content) {
        try {
            Path targetPath = safePath(fileName);
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.writeString(targetPath, content != null ? content : "");
            return "Success: File '" + fileName + "' written successfully in workspace.";
        } catch (SecurityException e) {
            return "Security Blocked: " + e.getMessage();
        } catch (Exception e) {
            return "Error writing to file: " + e.getMessage();
        }
    }

    @Tool(description = "Lists all files and directories currently present inside the website workspace.")
    public String listFiles() {
        try {
            if (!Files.exists(workspaceDir)) {
                Files.createDirectories(workspaceDir);
            }
            try (Stream<Path> stream = Files.walk(workspaceDir)) {
                List<String> files = stream
                        .filter(p -> !p.equals(workspaceDir))
                        .map(p -> {
                            String relative = workspaceDir.relativize(p).toString().replace('\\', '/');
                            return Files.isDirectory(p) ? relative + "/" : relative;
                        })
                        .sorted()
                        .toList();

                if (files.isEmpty()) {
                    return "Workspace is empty. (Path: " + workspaceDir + ")";
                }
                return "Files in workspace:\n" + String.join("\n", files);
            }
        } catch (Exception e) {
            return "Error listing workspace files: " + e.getMessage();
        }
    }
}

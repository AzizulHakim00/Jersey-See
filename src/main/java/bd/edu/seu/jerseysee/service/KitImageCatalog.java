package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.exception.InvalidFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Whitelisted kit photos supplied with JerseySee and available in the product editor. */
@Service
public class KitImageCatalog {

    private static final String RESOURCE_ROOT = "classpath:/static/images/kits/";
    private static final Map<String, KitImageOption> OPTIONS = optionsById();

    private final ResourceLoader resourceLoader;

    public KitImageCatalog(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public List<KitImageOption> options() {
        return List.copyOf(OPTIONS.values());
    }

    public MultipartFile selectedFile(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        KitImageOption option = OPTIONS.get(id);
        if (option == null) {
            throw new InvalidFileException("Select a valid kit image.");
        }
        Resource resource = resourceLoader.getResource(RESOURCE_ROOT + option.fileName());
        try (InputStream input = resource.getInputStream()) {
            return new InMemoryMultipartFile(option.fileName(), input.readAllBytes());
        } catch (IOException exception) {
            throw new InvalidFileException("The selected kit image could not be loaded.", exception);
        }
    }

    private static Map<String, KitImageOption> optionsById() {
        Map<String, KitImageOption> options = new LinkedHashMap<>();
        add(options, "ac-milan-retro", "AC Milan retro", "ac-milan-retro.jpg");
        add(options, "arsenal-away", "Arsenal away", "arsenal-away.jpg");
        add(options, "arsenal-home", "Arsenal home", "arsenal-home.jpg");
        add(options, "arsenal-third", "Arsenal third", "arsenal-third.jpg");
        add(options, "barcelona-away", "Barcelona away", "barcelona-away.jpg");
        add(options, "barcelona-home", "Barcelona home", "barcelona-home.jpg");
        add(options, "barcelona-third", "Barcelona third", "barcelona-third.jpg");
        add(options, "chelsea-away", "Chelsea away", "chelsea-away.jpg");
        add(options, "chelsea-home", "Chelsea home", "chelsea-home.jpg");
        add(options, "chelsea-third", "Chelsea third", "chelsea-third.jpg");
        add(options, "juventus-94-95-retro", "Juventus 94/95 retro", "juventus-94-95-retro.jpg");
        add(options, "liverpool-away", "Liverpool away", "liverpool-away.jpg");
        add(options, "liverpool-home", "Liverpool home", "liverpool-home.jpg");
        add(options, "manchester-city-away", "Manchester City away", "manchester-city-away.jpg");
        add(options, "manchester-city-home", "Manchester City home", "manchester-city-home.jpg");
        add(options, "manchester-city-third", "Manchester City third", "manchester-city-third.jpg");
        add(options, "manchester-united-away", "Manchester United away", "manchester-united-away.jpg");
        add(options, "manchester-united-third", "Manchester United third", "manchester-united-third.jpg");
        add(options, "real-madrid-away", "Real Madrid away", "real-madrid-away.jpg");
        add(options, "real-madrid-home", "Real Madrid home", "real-madrid-home.jpg");
        add(options, "real-madrid-retro", "Real Madrid retro", "real-madrid-retro.jpg");
        add(options, "real-madrid-third", "Real Madrid third", "real-madrid-third.jpg");
        return Collections.unmodifiableMap(options);
    }

    private static void add(Map<String, KitImageOption> options, String id, String label, String fileName) {
        options.put(id, new KitImageOption(id, label, fileName, "/images/kits/" + fileName));
    }

    public record KitImageOption(String id, String label, String fileName, String previewUrl) { }

    private static final class InMemoryMultipartFile implements MultipartFile {
        private final String fileName;
        private final byte[] content;

        private InMemoryMultipartFile(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content.clone();
        }

        @Override public String getName() { return "selectedKitImage"; }
        @Override public String getOriginalFilename() { return fileName; }
        @Override public String getContentType() { return "image/jpeg"; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content.clone(); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }

        @Override
        public void transferTo(File destination) throws IOException {
            Files.write(destination.toPath(), content);
        }
    }
}

package com.project.modules.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.project.common.exception.BadRequestException;
import com.project.modules.court.entity.Court;
import com.project.modules.court.repository.CourtImageRepository;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.court.service.CourtService;
import com.project.modules.storage.service.FileStorageService;
import com.project.modules.user.repository.UserRepository;

@SpringBootTest
class LocalFileStorageServiceIntegrationTest {
    private static final Path UPLOAD_DIRECTORY = Paths.get("build/test-uploads/courts").toAbsolutePath().normalize();

    @Autowired
    private FileStorageService storage;
    @Autowired
    private CourtService courtService;
    @Autowired
    private CourtRepository courts;
    @Autowired
    private CourtImageRepository images;
    @Autowired
    private UserRepository users;

    @BeforeEach
    @AfterEach
    void cleanUp() throws IOException {
        SecurityContextHolder.clearContext();
        images.deleteAll();
        courts.deleteAll();
        if (Files.exists(UPLOAD_DIRECTORY)) {
            try (var paths = Files.walk(UPLOAD_DIRECTORY)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
        }
    }

    @Test
    void attachesMultipleImagesToOneCourt() {
        Court court = createCourt();

        var uploaded = storage.attachToCourt(court.getId(), List.of(image("first.png"), image("second.png")));
        var first = uploaded.get(0);
        var second = uploaded.get(1);

        var response = courtService.findById(court.getId());
        assertThat(response.images()).extracting("id").containsExactly(first.id(), second.id());
        assertThat(images.count()).isEqualTo(2);
        assertThat(Files.exists(UPLOAD_DIRECTORY.resolve(first.fileName()))).isTrue();
        assertThat(Files.exists(UPLOAD_DIRECTORY.resolve(second.fileName()))).isTrue();
    }

    @Test
    void deletesCourtImageByUuid() {
        Court court = createCourt();
        var uploaded = storage.attachToCourt(court.getId(), List.of(image("court.png"))).getFirst();

        storage.deleteCourtImage(uploaded.id());

        assertThat(images.existsById(uploaded.id())).isFalse();
        assertThat(Files.exists(UPLOAD_DIRECTORY.resolve(uploaded.fileName()))).isFalse();
        assertThat(courtService.findById(court.getId()).images()).isEmpty();
    }

    @Test
    void validatesAllImagesBeforeStoringAnyFile() throws IOException {
        Court court = createCourt();
        var invalid = new MockMultipartFile("files", "invalid.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> storage.attachToCourt(court.getId(), List.of(image("valid.png"), invalid)))
                .isInstanceOf(BadRequestException.class);

        assertThat(images.count()).isZero();
        assertThat(Files.exists(UPLOAD_DIRECTORY)).isFalse();
    }

    private Court createCourt() {
        var manager = users.findByUsername("manager").orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                manager.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
        return courts.save(Court.builder().name("Court 1").address("Address")
                .pricePerHour(BigDecimal.valueOf(100_000)).managers(Set.of(manager)).build());
    }

    private MockMultipartFile image(String name) {
        return new MockMultipartFile("file", name, "image/png", new byte[]{1, 2, 3});
    }
}

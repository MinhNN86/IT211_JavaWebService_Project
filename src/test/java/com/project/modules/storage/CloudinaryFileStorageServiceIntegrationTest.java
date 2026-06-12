package com.project.modules.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.project.common.exception.BadRequestException;
import com.project.modules.court.entity.Court;
import com.project.modules.court.repository.CourtImageRepository;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.court.service.CourtService;
import com.project.modules.storage.service.CloudinaryStorageClient;
import com.project.modules.storage.service.CloudinaryStorageClient.UploadedAsset;
import com.project.modules.storage.service.FileStorageService;
import com.project.modules.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CloudinaryFileStorageServiceIntegrationTest {
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
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private CloudinaryStorageClient cloudinaryStorageClient;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        images.deleteAll();
        courts.deleteAll();
        reset(cloudinaryStorageClient);
        when(cloudinaryStorageClient.uploadImage(any(), anyString())).thenAnswer(invocation -> {
            String publicId = invocation.getArgument(1);
            return new UploadedAsset(publicId, "https://res.cloudinary.com/test-cloud/image/upload/" + publicId);
        });
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        images.deleteAll();
        courts.deleteAll();
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
        assertThat(first.fileName()).isEqualTo(first.id().toString());
        assertThat(first.url()).startsWith("https://res.cloudinary.com/test-cloud/image/upload/test-courts/");
        assertThat(second.fileName()).isEqualTo(second.id().toString());
        assertThat(second.url()).startsWith("https://res.cloudinary.com/test-cloud/image/upload/test-courts/");
    }

    @Test
    void deletesCourtImageByUuid() {
        Court court = createCourt();
        var uploaded = storage.attachToCourt(court.getId(), List.of(image("court.png"))).getFirst();

        storage.deleteCourtImage(uploaded.id());

        assertThat(images.existsById(uploaded.id())).isFalse();
        verify(cloudinaryStorageClient).deleteImage("test-courts/" + uploaded.fileName());
        assertThat(courtService.findById(court.getId()).images()).isEmpty();
    }

    @Test
    void validatesAllImagesBeforeStoringAnyFile() {
        Court court = createCourt();
        var invalid = new MockMultipartFile("files", "invalid.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> storage.attachToCourt(court.getId(), List.of(image("valid.png"), invalid)))
                .isInstanceOf(BadRequestException.class);

        assertThat(images.count()).isZero();
        verify(cloudinaryStorageClient, never()).uploadImage(any(), anyString());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void uploadResponseContainsAbsoluteImageUrl() throws Exception {
        Court court = createCourt();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .multipart("/api/v1/manager/courts/{courtId}/images", court.getId())
                .file(new MockMultipartFile("files", "court.png", "image/png", new byte[]{1, 2, 3})))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].url")
                        .value(org.hamcrest.Matchers.startsWith("https://res.cloudinary.com/test-cloud/")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/courts/{courtId}", court.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.data.images[0].url")
                        .value(org.hamcrest.Matchers.startsWith("https://res.cloudinary.com/test-cloud/")));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void deleteResponseContainsSuccessMessage() throws Exception {
        Court court = createCourt();
        var uploaded = storage.attachToCourt(court.getId(), List.of(image("court.png"))).getFirst();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/manager/courts/images/{imageId}", uploaded.id()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.success")
                        .value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("Delete successfully"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data")
                        .doesNotExist());
    }

    private Court createCourt() {
        var manager = users.findByUsername("manager").orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                manager.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
        return courts.save(Court.builder().name("Court 1").address("Address").managers(Set.of(manager)).build());
    }

    private MockMultipartFile image(String name) {
        return new MockMultipartFile("file", name, "image/png", new byte[]{1, 2, 3});
    }
}

package com.project.modules.timeslot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashSet;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.project.modules.court.entity.Court;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TimeSlotAuthorizationIntegrationTest {
    private static final String CREATE_REQUEST = """
            {
              "startTime": "06:01:00",
              "endTime": "07:01:00",
              "price": 50000
            }
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CourtRepository courts;
    @Autowired
    private UserRepository users;

    private Court managedCourt;
    private Court otherCourt;

    @BeforeEach
    void setUp() {
        var manager = users.findByUsername("manager").orElseThrow();
        managedCourt = courts.save(Court.builder().name("Managed court").address("Address")
                .managers(new HashSet<>(java.util.Set.of(manager))).build());
        otherCourt = courts.save(Court.builder().name("Other court").address("Address").build());
    }

    @Test
    void anonymousUserCanViewTimeSlotsForCourt() throws Exception {
        mockMvc.perform(get("/api/v1/courts/{courtId}/time-slots", managedCourt.getId())).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void managerCanOnlyManageAssignedCourtTimeSlots() throws Exception {
        var response = mockMvc
                .perform(post("/api/v1/manager/courts/{courtId}/time-slots", managedCourt.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_REQUEST))
                .andExpect(status().isCreated()).andReturn();
        var slotId = com.jayway.jsonpath.JsonPath.read(response.getResponse().getContentAsString(), "$.data.id")
                .toString();

        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots", otherCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(CREATE_REQUEST)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/manager/courts/{courtId}/time-slots/{id}", managedCourt.getId(), slotId)
                .contentType(MediaType.APPLICATION_JSON).content(updateRequest())).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/manager/courts/{courtId}/time-slots/{id}", managedCourt.getId(), slotId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanManageAnyCourtAndSameTimesCanExistForDifferentCourts() throws Exception {
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(CREATE_REQUEST)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots", otherCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(CREATE_REQUEST)).andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotManageTimeSlots() throws Exception {
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(CREATE_REQUEST)).andExpect(status().isForbidden());
    }

    private String updateRequest() {
        return """
                {
                  "startTime": "06:30:00",
                  "endTime": "07:30:00",
                  "price": 60000,
                  "active": true
                }
                """;
    }
}

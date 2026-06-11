package com.project.modules.timeslot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
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
import com.project.modules.timeslot.repository.TimeSlotRepository;
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
    @Autowired
    private TimeSlotRepository timeSlots;

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

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void managerCanCreateTimeSlotsInBulk() throws Exception {
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots/bulk", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(bulkCreateRequest("06:00:00", "08:00:00", 30)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].startTime").value("06:00:00"))
                .andExpect(jsonPath("$.data[3].endTime").value("08:00:00"))
                .andExpect(jsonPath("$.data[0].price").value(50000));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void bulkCreateRejectsInvalidDurationAndNonDivisibleRange() throws Exception {
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots/bulk", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(bulkCreateRequest("06:00:00", "08:00:00", 45)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots/bulk", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(bulkCreateRequest("06:00:00", "07:45:00", 60)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots/bulk", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(bulkCreateRequest("06:00:00", "07:00:01", 60)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void bulkCreateIsAtomicWhenTimeSlotAlreadyExists() throws Exception {
        timeSlots.save(com.project.modules.timeslot.entity.TimeSlot.builder().court(managedCourt)
                .startTime(java.time.LocalTime.of(7, 0)).endTime(java.time.LocalTime.of(8, 0))
                .price(BigDecimal.valueOf(50_000)).build());

        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots/bulk", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(bulkCreateRequest("06:00:00", "09:00:00", 60)))
                .andExpect(status().isConflict());

        Assertions.assertEquals(1, timeSlots.findByCourtIdOrderByStartTime(managedCourt.getId()).size());
    }

    private String bulkCreateRequest(String startTime, String endTime, int durationMinutes) {
        return """
                {
                  "startTime": "%s",
                  "endTime": "%s",
                  "durationMinutes": %d,
                  "price": 50000
                }
                """.formatted(startTime, endTime, durationMinutes);
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

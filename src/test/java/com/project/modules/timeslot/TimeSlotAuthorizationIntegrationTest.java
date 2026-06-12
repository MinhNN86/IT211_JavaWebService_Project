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
import com.project.modules.timeslot.repository.TimeSlotRepository;
import com.project.modules.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TimeSlotAuthorizationIntegrationTest {
    private static final String CREATE_REQUEST = """
            {
              "startTime": "06:00",
              "endTime": "07:00"
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
    void courtDetailIncludesTimeSlotsOrderedByStartTime() throws Exception {
        var laterSlot = createTimeSlot(managedCourt, 7, 0, 8, 0);
        var earlierSlot = createTimeSlot(managedCourt, 6, 0, 6, 30);

        mockMvc.perform(get("/api/v1/courts/{courtId}", managedCourt.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(managedCourt.getId()))
                .andExpect(jsonPath("$.data.timeSlots.length()").value(2))
                .andExpect(jsonPath("$.data.timeSlots[0].id").value(earlierSlot.getId()))
                .andExpect(jsonPath("$.data.timeSlots[0].startTime").value("06:00"))
                .andExpect(jsonPath("$.data.timeSlots[1].id").value(laterSlot.getId()))
                .andExpect(jsonPath("$.data.timeSlots[1].startTime").value("07:00"));
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
    @WithMockUser(username = "manager", roles = "MANAGER")
    void createRejectsOverlappingActiveTimeSlot() throws Exception {
        timeSlots.save(com.project.modules.timeslot.entity.TimeSlot.builder().court(managedCourt)
                .startTime(java.time.LocalTime.of(6, 0)).endTime(java.time.LocalTime.of(7, 0))
                .price(50_000).build());

        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(createRequest("06:30", "07:30")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Time slot overlaps an active time slot for this court"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void createOnlyAcceptsThirtyOrSixtyMinuteSlots() throws Exception {
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(createRequest("06:00", "07:30")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(createRequest("08:00:00", "08:30:00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void createRequiresThirtyMinuteTimeAlignment() throws Exception {
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(createRequest("06:15", "06:45")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start and end times must align to 30-minute intervals"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void createReactivatesInactiveTimeSlotWithDefaultPrice() throws Exception {
        var inactiveSlot = timeSlots.save(com.project.modules.timeslot.entity.TimeSlot.builder().court(managedCourt)
                .startTime(java.time.LocalTime.of(6, 0)).endTime(java.time.LocalTime.of(7, 0))
                .price(50_000).active(false).build());

        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(createRequest("06:00", "07:00")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value(inactiveSlot.getId()))
                .andExpect(jsonPath("$.data.active").value(true)).andExpect(jsonPath("$.data.price").value(0));

        Assertions.assertEquals(1, timeSlots.findByCourtIdOrderByStartTime(managedCourt.getId()).size());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void malformedTimeJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(createRequest("invalid", "07:00")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Malformed JSON request"));
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
                .contentType(MediaType.APPLICATION_JSON).content(bulkCreateRequest("06:00", "08:00", 30)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].startTime").value("06:00"))
                .andExpect(jsonPath("$.data[3].endTime").value("08:00"))
                .andExpect(jsonPath("$.data[0].price").value(0));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void bulkCreateRejectsInvalidDurationAndNonDivisibleRange() throws Exception {
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots/bulk", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(bulkCreateRequest("06:00", "08:00", 45)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots/bulk", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(bulkCreateRequest("06:00", "07:45", 60)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots/bulk", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(bulkCreateRequest("06:00:00", "07:00:00", 60)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void bulkCreateIsAtomicWhenTimeSlotAlreadyExists() throws Exception {
        timeSlots.save(com.project.modules.timeslot.entity.TimeSlot.builder().court(managedCourt)
                .startTime(java.time.LocalTime.of(7, 0)).endTime(java.time.LocalTime.of(8, 0))
                .price(50_000).build());

        mockMvc.perform(post("/api/v1/manager/courts/{courtId}/time-slots/bulk", managedCourt.getId())
                .contentType(MediaType.APPLICATION_JSON).content(bulkCreateRequest("06:00", "09:00", 60)))
                .andExpect(status().isConflict());

        Assertions.assertEquals(1, timeSlots.findByCourtIdOrderByStartTime(managedCourt.getId()).size());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void managerCanUpdateMultiplePriceGroupsForAssignedCourt() throws Exception {
        var first = createTimeSlot(managedCourt, 6, 0, 6, 30);
        var second = createTimeSlot(managedCourt, 6, 30, 7, 0);
        var third = createTimeSlot(managedCourt, 7, 0, 7, 30);

        mockMvc.perform(patch("/api/v1/manager/time-slots/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePricesRequest(first.getId(), second.getId(), third.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].price").value(50000))
                .andExpect(jsonPath("$.data[1].price").value(50000))
                .andExpect(jsonPath("$.data[2].price").value(80000));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void managerCannotUpdatePriceForUnassignedCourt() throws Exception {
        var timeSlot = createTimeSlot(otherCourt, 6, 0, 7, 0);

        mockMvc.perform(patch("/api/v1/manager/time-slots/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(singlePriceRequest(timeSlot.getId(), 50_000)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUpdatePriceForAnyCourt() throws Exception {
        var timeSlot = createTimeSlot(otherCourt, 6, 0, 7, 0);

        mockMvc.perform(patch("/api/v1/manager/time-slots/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(singlePriceRequest(timeSlot.getId(), 50_000)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].price").value(50000));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updatePricesRejectsNonPositivePrice() throws Exception {
        var timeSlot = createTimeSlot(managedCourt, 6, 0, 7, 0);

        mockMvc.perform(patch("/api/v1/manager/time-slots/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(singlePriceRequest(timeSlot.getId(), 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updatePricesRejectsDuplicateTimeSlotIds() throws Exception {
        var timeSlot = createTimeSlot(managedCourt, 6, 0, 7, 0);
        var request = """
                {
                  "entries": [
                    { "timeSlotIds": [%d], "price": 50000 },
                    { "timeSlotIds": [%d], "price": 80000 }
                  ]
                }
                """.formatted(timeSlot.getId(), timeSlot.getId());

        mockMvc.perform(patch("/api/v1/manager/time-slots/prices")
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Duplicate time slot ID: " + timeSlot.getId()));
    }

    private String bulkCreateRequest(String startTime, String endTime, int durationMinutes) {
        return """
                {
                  "startTime": "%s",
                  "endTime": "%s",
                  "durationMinutes": %d
                }
                """.formatted(startTime, endTime, durationMinutes);
    }

    private String createRequest(String startTime, String endTime) {
        return """
                {
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(startTime, endTime);
    }

    private com.project.modules.timeslot.entity.TimeSlot createTimeSlot(
            Court court, int startHour, int startMinute, int endHour, int endMinute) {
        return timeSlots.save(com.project.modules.timeslot.entity.TimeSlot.builder().court(court)
                .startTime(java.time.LocalTime.of(startHour, startMinute))
                .endTime(java.time.LocalTime.of(endHour, endMinute)).price(0).build());
    }

    private String updatePricesRequest(Long firstId, Long secondId, Long thirdId) {
        return """
                {
                  "entries": [
                    { "timeSlotIds": [%d, %d], "price": 50000 },
                    { "timeSlotIds": [%d], "price": 80000 }
                  ]
                }
                """.formatted(firstId, secondId, thirdId);
    }

    private String singlePriceRequest(Long timeSlotId, int price) {
        return """
                {
                  "entries": [
                    { "timeSlotIds": [%d], "price": %d }
                  ]
                }
                """.formatted(timeSlotId, price);
    }

    private String updateRequest() {
        return """
                {
                  "startTime": "06:30",
                  "endTime": "07:30",
                  "price": 60000,
                  "active": true
                }
                """;
    }
}

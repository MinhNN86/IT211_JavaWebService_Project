package com.project.modules.timeslot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

    @Test
    void anonymousUserCanViewTimeSlots() throws Exception {
        mockMvc.perform(get("/api/v1/time-slots")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotManageTimeSlots() throws Exception {
        mockMvc.perform(post("/api/v1/admin/time-slots").contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_REQUEST)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/admin/time-slots/1").contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_REQUEST)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/admin/time-slots/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanManageTimeSlots() throws Exception {
        var response = mockMvc.perform(post("/api/v1/admin/time-slots").contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_REQUEST)).andExpect(status().isCreated()).andReturn();
        var location = com.jayway.jsonpath.JsonPath.read(response.getResponse().getContentAsString(), "$.data.id")
                .toString();

        mockMvc.perform(put("/api/v1/admin/time-slots/{id}", location).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "startTime": "06:30:00",
                          "endTime": "07:30:00",
                          "price": 60000,
                          "active": true
                        }
                        """)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/admin/time-slots/{id}", location)).andExpect(status().isNoContent());
    }
}

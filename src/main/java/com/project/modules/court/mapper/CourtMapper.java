package com.project.modules.court.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.common.util.PublicUrlResolver;
import com.project.modules.court.dto.response.CourtDetailResponse;
import com.project.modules.court.dto.response.CourtImageResponse;
import com.project.modules.court.dto.response.CourtResponse;
import com.project.modules.court.entity.Court;
import com.project.modules.timeslot.dto.response.TimeSlotResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CourtMapper {
    private final PublicUrlResolver publicUrlResolver;

    public CourtResponse toResponse(Court c) {
        return new CourtResponse(c.getId(), c.getName(), c.getDescription(), c.getAddress(), c.getStatus(),
                c.getImages().stream()
                        .map(i -> new CourtImageResponse(i.getId(), publicUrlResolver.resolve(i.getUrl())))
                        .toList());
    }

    public CourtDetailResponse toDetailResponse(Court c, List<TimeSlotResponse> timeSlots) {
        return new CourtDetailResponse(c.getId(), c.getName(), c.getDescription(), c.getAddress(), c.getStatus(),
                c.getImages().stream()
                        .map(i -> new CourtImageResponse(i.getId(), publicUrlResolver.resolve(i.getUrl())))
                        .toList(),
                timeSlots);
    }
}

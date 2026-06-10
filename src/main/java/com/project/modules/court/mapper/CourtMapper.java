package com.project.modules.court.mapper;

import org.springframework.stereotype.Component;

import com.project.modules.court.dto.response.CourtImageResponse;
import com.project.modules.court.dto.response.CourtResponse;
import com.project.modules.court.entity.Court;

@Component
public class CourtMapper {
    public CourtResponse toResponse(Court c) {
        return new CourtResponse(c.getId(), c.getName(), c.getDescription(), c.getAddress(), c.getPricePerHour(),
                c.getStatus(), c.getImages().stream().map(i -> new CourtImageResponse(i.getId(), i.getUrl())).toList());
    }
}

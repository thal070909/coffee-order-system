package com.sparta.coffee.domain.point.controller;

import com.sparta.coffee.common.response.ApiResponse;
import com.sparta.coffee.domain.point.dto.request.PointChargeRequest;
import com.sparta.coffee.domain.point.dto.response.PointChargeResponse;
import com.sparta.coffee.domain.point.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<PointChargeResponse>> charge(
            @PathVariable Long userId,
            @Valid @RequestBody PointChargeRequest request) {

        PointChargeResponse response = pointService.charge(userId, request.amount());

        return ResponseEntity.ok(ApiResponse.ok("포인트 충전에 성공했습니다.", response));
    }
}

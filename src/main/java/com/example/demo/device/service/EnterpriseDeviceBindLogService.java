package com.example.demo.device.service;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.entity.Device;
import com.example.demo.device.entity.EdgeDeviceBindLog;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.device.repository.EdgeDeviceBindLogRepository;
import com.example.demo.enterprise.dto.EnterpriseDeviceBindLogItemData;
import com.example.demo.enterprise.dto.EnterpriseDeviceBindLogPageResponseData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnterpriseDeviceBindLogService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final EdgeDeviceBindLogRepository edgeDeviceBindLogRepository;
    private final DeviceRepository deviceRepository;
    private final BusinessAccessService businessAccessService;

    public EnterpriseDeviceBindLogService(EdgeDeviceBindLogRepository edgeDeviceBindLogRepository,
                                          DeviceRepository deviceRepository,
                                          BusinessAccessService businessAccessService) {
        this.edgeDeviceBindLogRepository = edgeDeviceBindLogRepository;
        this.deviceRepository = deviceRepository;
        this.businessAccessService = businessAccessService;
    }

    @Transactional(readOnly = true)
    public EnterpriseDeviceBindLogPageResponseData list(AuthenticatedUser operator, Long enterpriseId, Integer page, Integer size) {
        Long readableEnterpriseId = businessAccessService.resolveReadableEnterpriseId(operator, enterpriseId);
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Page<EdgeDeviceBindLog> result = edgeDeviceBindLogRepository.findByEnterpriseIdOrderByCreatedAtDescIdDesc(
                readableEnterpriseId,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        Map<Long, Device> devicesById = loadDevices(result.getContent());
        return new EnterpriseDeviceBindLogPageResponseData(
                result.getTotalElements(),
                pageNo,
                pageSize,
                result.getContent().stream().map(log -> toItem(log, devicesById.get(log.getDeviceId()))).toList());
    }

    private Map<Long, Device> loadDevices(List<EdgeDeviceBindLog> logs) {
        Map<Long, Device> result = new HashMap<>();
        List<Long> deviceIds = logs.stream().map(EdgeDeviceBindLog::getDeviceId).distinct().toList();
        for (Device device : deviceRepository.findAllById(deviceIds)) {
            if (device.getId() != null) {
                result.put(device.getId(), device);
            }
        }
        return result;
    }

    private EnterpriseDeviceBindLogItemData toItem(EdgeDeviceBindLog log, Device device) {
        return new EnterpriseDeviceBindLogItemData(
                log.getId(),
                log.getDeviceId(),
                log.getDeviceCode(),
                device == null ? null : device.getDeviceName(),
                log.getEnterpriseId(),
                log.getEnterpriseNameSnapshot(),
                log.getActivationCodeMasked(),
                log.getAction(),
                log.getOperatorType(),
                log.getOperatorId(),
                log.getRemark(),
                toOffsetDateTime(log.getCreatedAt()));
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "page必须大于等于1");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "size必须大于等于1");
        }
        return Math.min(size, MAX_SIZE);
    }
}

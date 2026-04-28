package com.example.demo.vehicle.service;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.entity.Device;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.fleet.entity.Fleet;
import com.example.demo.fleet.repository.FleetRepository;
import com.example.demo.system.service.SystemAuditService;
import com.example.demo.vehicle.dto.CreateVehicleRequest;
import com.example.demo.vehicle.dto.UpdateVehicleRequest;
import com.example.demo.vehicle.dto.VehicleDetailResponseData;
import com.example.demo.vehicle.dto.VehicleListItemData;
import com.example.demo.vehicle.dto.VehiclePageResponseData;
import com.example.demo.vehicle.entity.Vehicle;
import com.example.demo.vehicle.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VehicleManagementService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final VehicleRepository vehicleRepository;
    private final DeviceRepository deviceRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final FleetRepository fleetRepository;
    private final BusinessAccessService businessAccessService;
    private final SystemAuditService systemAuditService;

    public VehicleManagementService(VehicleRepository vehicleRepository,
                                    DeviceRepository deviceRepository,
                                    EnterpriseRepository enterpriseRepository,
                                    FleetRepository fleetRepository,
                                    BusinessAccessService businessAccessService,
                                    SystemAuditService systemAuditService) {
        this.vehicleRepository = vehicleRepository;
        this.deviceRepository = deviceRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.fleetRepository = fleetRepository;
        this.businessAccessService = businessAccessService;
        this.systemAuditService = systemAuditService;
    }

    @Transactional(readOnly = true)
    public VehiclePageResponseData listVehicles(AuthenticatedUser operator,
                                                Integer page,
                                                Integer size,
                                                Long enterpriseId,
                                                Long fleetId,
                                                String keyword,
                                                Byte status) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Long readableEnterpriseId = businessAccessService.resolveReadableEnterpriseId(operator, enterpriseId);
        validateFleetScope(fleetId, readableEnterpriseId);
        Specification<Vehicle> specification = buildSpecification(readableEnterpriseId, fleetId, keyword, normalizeStatus(status, true));
        Page<Vehicle> result = vehicleRepository.findAll(specification, PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.ASC, "id")));
        Map<Long, Device> boundDevices = loadBoundDeviceMap(result.getContent());
        return new VehiclePageResponseData(result.getTotalElements(), pageNo, pageSize,
                result.getContent().stream().map(vehicle -> toListItem(vehicle, boundDevices)).toList());
    }

    @Transactional(readOnly = true)
    public VehicleDetailResponseData getVehicle(AuthenticatedUser operator, Long vehicleId) {
        Vehicle vehicle = getVehicleEntity(vehicleId);
        businessAccessService.resolveReadableEnterpriseId(operator, vehicle.getEnterpriseId());
        return toDetail(vehicle, loadBoundDeviceMap(List.of(vehicle)));
    }

    @Transactional
    public VehicleDetailResponseData createVehicle(AuthenticatedUser operator, CreateVehicleRequest request) {
        Long enterpriseId = request.enterpriseId();
        businessAccessService.assertCanManageEnterprise(operator, enterpriseId);
        validateEnterpriseExists(enterpriseId);
        validateFleetBelongsToEnterprise(request.fleetId(), enterpriseId);
        String plateNumber = normalizeRequired(request.plateNumber(), "plateNumber不能为空");
        if (vehicleRepository.existsByEnterpriseIdAndPlateNumber(enterpriseId, plateNumber)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "车牌号已存在");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Vehicle vehicle = new Vehicle();
        vehicle.setEnterpriseId(enterpriseId);
        vehicle.setFleetId(request.fleetId());
        vehicle.setPlateNumber(plateNumber);
        vehicle.setVin(normalizeOptional(request.vin()));
        vehicle.setStatus((byte) 1);
        vehicle.setRemark(normalizeOptional(request.remark()));
        vehicle.setCreatedAt(now);
        vehicle.setUpdatedAt(now);
        Vehicle saved = vehicleRepository.save(vehicle);
        systemAuditService.record(operator, "VEHICLE", "CREATE_VEHICLE", "VEHICLE", String.valueOf(saved.getId()),
                "SUCCESS", "创建车辆", auditDetail(operator, saved, null, snapshot(saved)));
        return toDetail(saved, loadBoundDeviceMap(List.of(saved)));
    }

    @Transactional
    public VehicleDetailResponseData updateVehicle(AuthenticatedUser operator, Long vehicleId, UpdateVehicleRequest request) {
        Vehicle vehicle = getVehicleEntity(vehicleId);
        businessAccessService.assertCanManageEnterprise(operator, vehicle.getEnterpriseId());
        validateFleetBelongsToEnterprise(request.fleetId(), vehicle.getEnterpriseId());
        String plateNumber = normalizeRequired(request.plateNumber(), "plateNumber不能为空");
        if (vehicleRepository.existsByEnterpriseIdAndPlateNumberAndIdNot(vehicle.getEnterpriseId(), plateNumber, vehicleId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "车牌号已存在");
        }
        Map<String, Object> before = snapshot(vehicle);
        vehicle.setFleetId(request.fleetId());
        vehicle.setPlateNumber(plateNumber);
        vehicle.setVin(normalizeOptional(request.vin()));
        vehicle.setRemark(normalizeOptional(request.remark()));
        Vehicle saved = vehicleRepository.save(vehicle);
        systemAuditService.record(operator, "VEHICLE", "UPDATE_VEHICLE", "VEHICLE", String.valueOf(saved.getId()),
                "SUCCESS", "更新车辆", auditDetail(operator, saved, before, snapshot(saved)));
        return toDetail(saved, loadBoundDeviceMap(List.of(saved)));
    }

    @Transactional
    public VehicleDetailResponseData updateStatus(AuthenticatedUser operator, Long vehicleId, Byte status) {
        Vehicle vehicle = getVehicleEntity(vehicleId);
        businessAccessService.assertCanManageEnterprise(operator, vehicle.getEnterpriseId());
        Map<String, Object> before = snapshot(vehicle);
        vehicle.setStatus(normalizeStatus(status, false));
        Vehicle saved = vehicleRepository.save(vehicle);
        systemAuditService.record(operator, "VEHICLE", "UPDATE_VEHICLE_STATUS", "VEHICLE", String.valueOf(saved.getId()),
                "SUCCESS", "更新车辆状态", auditDetail(operator, saved, before, snapshot(saved)));
        return toDetail(saved, loadBoundDeviceMap(List.of(saved)));
    }

    private Specification<Vehicle> buildSpecification(Long enterpriseId, Long fleetId, String keyword, Byte status) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (enterpriseId != null) {
                predicates.add(cb.equal(root.get("enterpriseId"), enterpriseId));
            }
            if (fleetId != null) {
                predicates.add(cb.equal(root.get("fleetId"), fleetId));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("plateNumber"), pattern),
                        cb.like(root.get("vin"), pattern)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Vehicle getVehicleEntity(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }

    private void validateEnterpriseExists(Long enterpriseId) {
        if (!enterpriseRepository.existsById(enterpriseId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId不存在");
        }
    }

    private void validateFleetScope(Long fleetId, Long enterpriseId) {
        if (fleetId == null) {
            return;
        }
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_PARAM, "fleetId不存在"));
        if (enterpriseId != null && !enterpriseId.equals(fleet.getEnterpriseId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
    }

    private void validateFleetBelongsToEnterprise(Long fleetId, Long enterpriseId) {
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_PARAM, "fleetId不存在"));
        if (!fleet.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "fleetId不属于当前enterpriseId");
        }
    }

    private VehicleListItemData toListItem(Vehicle vehicle, Map<Long, Device> boundDevices) {
        Device boundDevice = boundDevices.get(vehicle.getId());
        return new VehicleListItemData(vehicle.getId(), vehicle.getEnterpriseId(), vehicle.getFleetId(), vehicle.getPlateNumber(),
                vehicle.getVin(), vehicle.getStatus(),
                boundDevice == null ? null : boundDevice.getId(),
                boundDevice == null ? null : boundDevice.getDeviceCode(),
                boundDevice == null ? null : boundDevice.getDeviceName(),
                vehicle.getRemark(), toOffsetDateTime(vehicle.getCreatedAt()), toOffsetDateTime(vehicle.getUpdatedAt()));
    }

    private VehicleDetailResponseData toDetail(Vehicle vehicle, Map<Long, Device> boundDevices) {
        Device boundDevice = boundDevices.get(vehicle.getId());
        return new VehicleDetailResponseData(vehicle.getId(), vehicle.getEnterpriseId(), vehicle.getFleetId(), vehicle.getPlateNumber(),
                vehicle.getVin(), vehicle.getStatus(),
                boundDevice == null ? null : boundDevice.getId(),
                boundDevice == null ? null : boundDevice.getDeviceCode(),
                boundDevice == null ? null : boundDevice.getDeviceName(),
                vehicle.getRemark(), toOffsetDateTime(vehicle.getCreatedAt()), toOffsetDateTime(vehicle.getUpdatedAt()));
    }

    private Map<Long, Device> loadBoundDeviceMap(Collection<Vehicle> vehicles) {
        Map<Long, Device> result = new HashMap<>();
        if (vehicles.isEmpty()) {
            return result;
        }
        List<Long> vehicleIds = vehicles.stream().map(Vehicle::getId).toList();
        for (Device device : deviceRepository.findByVehicleIdInOrderByStatusDescIdDesc(vehicleIds)) {
            if (device.getVehicleId() != null) {
                result.putIfAbsent(device.getVehicleId(), device);
            }
        }
        return result;
    }

    private Map<String, Object> snapshot(Vehicle vehicle) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", vehicle.getId());
        snapshot.put("enterpriseId", vehicle.getEnterpriseId());
        snapshot.put("fleetId", vehicle.getFleetId());
        snapshot.put("plateNumber", vehicle.getPlateNumber());
        snapshot.put("vin", vehicle.getVin());
        snapshot.put("status", vehicle.getStatus());
        return snapshot;
    }

    private Map<String, Object> auditDetail(AuthenticatedUser operator, Vehicle vehicle, Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operatorUserId", operator.getUserId());
        detail.put("operatorRoles", operator.getRoles());
        detail.put("targetType", "VEHICLE");
        detail.put("targetId", vehicle.getId());
        detail.put("targetEnterpriseId", vehicle.getEnterpriseId());
        detail.put("before", before);
        detail.put("after", after);
        return detail;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Byte normalizeStatus(Byte status, boolean allowNull) {
        if (status == null) {
            if (allowNull) {
                return null;
            }
            throw new BusinessException(ApiCode.INVALID_PARAM, "status不能为空");
        }
        if (status != (byte) 0 && status != (byte) 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "status仅支持0或1");
        }
        return status;
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

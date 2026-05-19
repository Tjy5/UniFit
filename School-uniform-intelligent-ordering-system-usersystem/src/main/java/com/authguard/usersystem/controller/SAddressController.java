package com.authguard.usersystem.controller;

import com.authguard.usersystem.dto.AddressRequestDto;
import com.authguard.usersystem.dto.ApiResponse;
import com.authguard.usersystem.entity.SAddress;
import com.authguard.usersystem.exception.ForbiddenException;
import com.authguard.usersystem.exception.NotFoundException;
import com.authguard.usersystem.exception.BizException;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISAddressService;
import com.authguard.usersystem.service.ISUserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/s-addresses")
@RequiredArgsConstructor
public class SAddressController {

    private final ISAddressService addressService;
    private final CurrentUserResolver currentUserResolver;
    private final ISUserActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SAddress>>> listMyAddresses(HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        List<SAddress> addresses = addressService.listAddressesByUserId(userId);
        activityLogService.recordActivityAsync(userId, "VIEW_MY_ADDRESSES", "ADDRESS_LIST", null, null, request);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse<SAddress>> getAddressDetails(@PathVariable Long addressId, HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        try {
            SAddress address = addressService.getAddressById(addressId, userId);
            activityLogService.recordActivityAsync(userId, "VIEW_ADDRESS_DETAIL", "ADDRESS", addressId.toString(), null, request);
            return ResponseEntity.ok(ApiResponse.success(address));
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException(ex.getMessage());
        } catch (SecurityException ex) {
            throw new ForbiddenException(ex.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SAddress>> createAddress(@Valid @RequestBody AddressRequestDto address, HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        try {
            SAddress createdAddress = addressService.createAddress(address.toEntity(), userId);
            activityLogService.recordActivityAsync(userId, "CREATE_ADDRESS_SUCCESS", "ADDRESS", createdAddress.getId().toString(),
                    address.getIsDefault() == null ? null : "isDefault:" + address.getIsDefault(), request);
            return ResponseEntity.status(201).body(ApiResponse.success(createdAddress));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BizException(ex.getMessage());
        }
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<SAddress>> updateAddress(@PathVariable Long addressId,
                                                               @Valid @RequestBody AddressRequestDto addressUpdates,
                                                               HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        try {
            SAddress updatedAddress = addressService.updateAddress(addressId, addressUpdates.toEntity(), userId);
            activityLogService.recordActivityAsync(userId, "UPDATE_ADDRESS_SUCCESS", "ADDRESS", addressId.toString(),
                    addressUpdates.getIsDefault() == null ? null : "isDefault:" + addressUpdates.getIsDefault(), request);
            return ResponseEntity.ok(ApiResponse.success(updatedAddress));
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException(ex.getMessage());
        } catch (SecurityException ex) {
            throw new ForbiddenException(ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new BizException(ex.getMessage());
        }
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long addressId, HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        try {
            boolean deleted = addressService.deleteAddress(addressId, userId);
            if (!deleted) {
                throw new NotFoundException("Address not found or you do not have permission to delete it.");
            }
            activityLogService.recordActivityAsync(userId, "DELETE_ADDRESS_SUCCESS", "ADDRESS", addressId.toString(), null, request);
            return ResponseEntity.ok(ApiResponse.success("Address deleted successfully.", null));
        } catch (IllegalStateException ex) {
            throw new BizException(ex.getMessage());
        }
    }

    @PutMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<Void>> setDefaultAddress(@PathVariable Long addressId, HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        try {
            boolean success = addressService.setDefaultAddress(addressId, userId);
            if (!success) {
                throw new BizException("Failed to set default address.");
            }
            activityLogService.recordActivityAsync(userId, "SET_DEFAULT_ADDRESS_SUCCESS", "ADDRESS", addressId.toString(), null, request);
            return ResponseEntity.ok(ApiResponse.success("Address set as default successfully.", null));
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException(ex.getMessage());
        } catch (SecurityException ex) {
            throw new ForbiddenException(ex.getMessage());
        }
    }
}

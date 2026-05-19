// src/main/java/com/authguard/usersystem/service/impl/SAddressServiceImpl.java
package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.entity.SAddress;
import com.authguard.usersystem.mapper.ISAddressMapper;
import com.authguard.usersystem.service.ISAddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional
import org.springframework.util.Assert; // For assertions

import java.util.List;

@Service
public class SAddressServiceImpl implements ISAddressService {

    private static final Logger log = LoggerFactory.getLogger(SAddressServiceImpl.class);

    @Autowired
    private ISAddressMapper addressMapper;

    @Override
    public List<SAddress> listAddressesByUserId(Long userId) {
        Assert.notNull(userId, "User ID cannot be null");
        return addressMapper.selectByUserId(userId);
    }

    @Override
    public SAddress getAddressById(Long addressId, Long userId) {
        Assert.notNull(addressId, "Address ID cannot be null");
        Assert.notNull(userId, "User ID cannot be null");

        SAddress address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new IllegalArgumentException("Address not found with ID: " + addressId);
        }
        // Ownership check
        if (!address.getUserId().equals(userId)) {
            throw new SecurityException("Access denied. Address does not belong to the current user.");
        }
        return address;
    }

    @Override
    @Transactional // Ensure atomicity if setting default involves multiple steps later
    public SAddress createAddress(SAddress address, Long userId) {
        Assert.notNull(address, "Address data cannot be null");
        Assert.notNull(userId, "User ID cannot be null");
        // Ensure the address is associated with the correct user
        address.setUserId(userId);
        // Reset ID just in case it was passed incorrectly
        address.setId(null);

        // Business Logic: If this is the *first* address being added for the user,
        // should it automatically become the default?
        // Optional: Check if user has other addresses, if not, set isDefault = true
        // List<SAddress> existingAddresses = addressMapper.selectByUserId(userId);
        // if (existingAddresses == null || existingAddresses.isEmpty()) {
        //    address.setIsDefault(true);
        // } else if (address.getIsDefault() == null) { // Ensure isDefault is not null
        //    address.setIsDefault(false);
        // } else if (address.getIsDefault()) {
        // If creating a new default address, unset the old one first
        //    addressMapper.unsetDefaultForUser(userId);
        // }

        // Simpler approach: If creating a new default, handle it via setDefaultAddress later
        // or ensure only one is true via frontend/validation. Force false if not specified.
        if (address.getIsDefault() == null) {
            address.setIsDefault(false);
        } else if (address.getIsDefault()) {
            // If creating a new default, first unset any existing default for this user
            log.debug("Creating default address; unsetting previous default for user {}", userId);
            addressMapper.unsetDefaultForUser(userId);
        }


        int inserted = addressMapper.insertAddress(address);
        if (inserted > 0 && address.getId() != null) {
            log.info("Address {} created for user {}", address.getId(), userId);
            // Return the object with the generated ID
            return address;
        } else {
            log.warn("Failed to insert address for user {}", userId);
            throw new RuntimeException("Failed to create address.");
        }
    }

    @Override
    @Transactional // If setting default is part of update
    public SAddress updateAddress(Long addressId, SAddress addressUpdates, Long userId) {
        Assert.notNull(addressId, "Address ID cannot be null");
        Assert.notNull(addressUpdates, "Address update data cannot be null");
        Assert.notNull(userId, "User ID cannot be null");

        // 1. Fetch existing address and verify ownership
        SAddress existingAddress = getAddressById(addressId, userId); // This already checks ownership

        // 2. Apply updates (prevent changing ID and userId)
        existingAddress.setRecipientName(addressUpdates.getRecipientName());
        existingAddress.setPhoneNumber(addressUpdates.getPhoneNumber());
        existingAddress.setProvince(addressUpdates.getProvince());
        existingAddress.setCity(addressUpdates.getCity());
        existingAddress.setDistrict(addressUpdates.getDistrict());
        existingAddress.setStreetAddress(addressUpdates.getStreetAddress());

        // Handle 'isDefault' carefully
        Boolean requestedDefault = addressUpdates.getIsDefault();
        if (requestedDefault != null && requestedDefault && !existingAddress.getIsDefault()) {
            // Requesting to make this address the new default
            log.debug("Setting address {} as default for user {}; unsetting previous default", addressId, userId);
            addressMapper.unsetDefaultForUser(userId); // Unset old default first
            existingAddress.setIsDefault(true);
        } else if (requestedDefault != null && !requestedDefault) {
            // Explicitly setting to non-default (maybe prevent unsetting the *only* default?)
            existingAddress.setIsDefault(false);
        }
        // If requestedDefault is null, don't change the default status

        // 3. Persist changes
        int updated = addressMapper.updateAddress(existingAddress);
        if (updated > 0) {
            log.info("Address {} updated for user {}", addressId, userId);
            return existingAddress; // Return the updated object
        } else {
            log.warn("Failed to update address {} for user {}", addressId, userId);
            // Might happen if the record was deleted between fetch and update, though unlikely with check.
            throw new RuntimeException("Failed to update address.");
        }
    }

    @Override
    @Transactional
    public boolean deleteAddress(Long addressId, Long userId) {
        Assert.notNull(addressId, "Address ID cannot be null");
        Assert.notNull(userId, "User ID cannot be null");

        // Optional: Check if it's the default address before deleting?
        // SAddress addressToDelete = getAddressById(addressId, userId); // Verify ownership first
        // if (addressToDelete.getIsDefault()) {
        //     throw new IllegalStateException("Cannot delete the default address. Please set another address as default first.");
        // }

        log.debug("Deleting address {} for user {}", addressId, userId);
        int deleted = addressMapper.deleteByIdAndUserId(addressId, userId);
        if (deleted > 0) {
            log.info("Address {} deleted for user {}", addressId, userId);
        } else {
            log.debug("Address {} not found for user {}; delete skipped", addressId, userId);
        }
        return deleted > 0;
    }

    @Override
    @Transactional // Crucial for atomicity
    public boolean setDefaultAddress(Long addressId, Long userId) {
        Assert.notNull(addressId, "Address ID cannot be null");
        Assert.notNull(userId, "User ID cannot be null");

        // 1. Verify the address exists and belongs to the user
        getAddressById(addressId, userId); // Checks ownership

        // 2. Unset the current default address(es) for this user
        log.debug("Unsetting current default address for user {}", userId);
        addressMapper.unsetDefaultForUser(userId);

        // 3. Set the target address as the new default
        log.debug("Setting address {} as default for user {}", addressId, userId);
        int updated = addressMapper.updateDefaultStatusByIdAndUserId(addressId, userId, true);

        if (updated > 0) {
            log.info("Address {} set as default for user {}", addressId, userId);
            return true;
        } else {
            // This theoretically shouldn't happen if getAddressById succeeded,
            // unless DB issue or record deleted concurrently.
            log.warn("Failed to set address {} as default for user {}", addressId, userId);
            // Transaction should roll back if unset succeeded but set failed.
            throw new RuntimeException("Failed to set default address status after unsetting previous default.");
        }
    }
}

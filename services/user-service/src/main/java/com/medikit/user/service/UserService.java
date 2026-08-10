package com.medikit.user.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import com.medikit.user.entity.UserRole;
import com.medikit.user.dto.AddressRequest;
import com.medikit.user.dto.AddressResponse;
import com.medikit.user.dto.UpdateProfileRequest;
import com.medikit.user.dto.UserResponse;
import com.medikit.user.entity.Address;
import com.medikit.user.entity.User;
import com.medikit.user.repository.AddressRepository;
import com.medikit.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public UserService(UserRepository userRepository, AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    public UserResponse getProfile(UUID userId) {
        return toResponse(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null && !request.email().isBlank() && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new ConflictException("Email already in use");
            }
            user.setEmail(request.email().toLowerCase());
            user.setEmailVerified(false);
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse upgradeRole(UUID userId, String requestedRole) {
        UserRole target;
        try {
            target = UserRole.valueOf(requestedRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role");
        }
        if (target != UserRole.DISTRIBUTOR && target != UserRole.PHARMACIST) {
            throw new BadRequestException("Role upgrade only allowed to DISTRIBUTOR or PHARMACIST");
        }
        User user = findUser(userId);
        if (user.getRole() == UserRole.ADMIN || user.getRole() == target) {
            return toResponse(user);
        }
        if (user.getRole() == UserRole.DELIVERY_PARTNER) {
            throw new BadRequestException("Cannot change role of a delivery partner");
        }
        user.setRole(target);
        return toResponse(userRepository.save(user));
    }

    public List<AddressResponse> getAddresses(UUID userId) {
        return addressRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Transactional
    public AddressResponse addAddress(UUID userId, AddressRequest request) {
        User user = findUser(userId);
        boolean first = addressRepository.countByUserId(userId) == 0;

        Address address = Address.builder()
                .user(user)
                .addressLine1(request.addressLine1())
                .addressLine2(request.addressLine2())
                .city(request.city())
                .state(request.state())
                .pincode(request.pincode())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .type(request.type() != null ? Address.AddressType.valueOf(request.type()) : Address.AddressType.HOME)
                .isDefault(first)
                .build();

        return toAddressResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        if (!address.getUser().getId().equals(userId)) {
            throw new NotFoundException("Address not found");
        }
        addressRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .forEach(a -> {
                    a.setDefault(false);
                    addressRepository.save(a);
                });
        address.setDefault(true);
        return toAddressResponse(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        if (!address.getUser().getId().equals(userId)) {
            throw new NotFoundException("Address not found");
        }
        address.setActive(false);
        addressRepository.save(address);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.getRole().name(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getCreatedAt());
    }

    private AddressResponse toAddressResponse(Address a) {
        return new AddressResponse(
                a.getId(),
                a.getAddressLine1(),
                a.getAddressLine2(),
                a.getCity(),
                a.getState(),
                a.getPincode(),
                a.getLatitude(),
                a.getLongitude(),
                a.getType().name(),
                a.isDefault(),
                a.getCreatedAt());
    }
}

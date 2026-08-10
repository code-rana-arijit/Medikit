package com.medikit.distributor.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import com.medikit.distributor.dto.CatalogItemRequest;
import com.medikit.distributor.dto.CatalogItemResponse;
import com.medikit.distributor.dto.DistributorRegisterRequest;
import com.medikit.distributor.dto.DistributorResponse;
import com.medikit.distributor.entity.CatalogItem;
import com.medikit.distributor.entity.DistributorProfile;
import com.medikit.distributor.repository.CatalogItemRepository;
import com.medikit.distributor.repository.DistributorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributorServiceTest {

    @Mock
    private DistributorProfileRepository profileRepository;

    @Mock
    private CatalogItemRepository catalogRepository;

    @InjectMocks
    private DistributorService distributorService;

    private UUID userId;
    private UUID distributorId;
    private DistributorProfile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        distributorId = UUID.randomUUID();
        profile = DistributorProfile.builder()
                .id(distributorId)
                .userId(userId)
                .shopName("Medi Wholesale")
                .licenseNumber("DL-12345")
                .address("12 Market Road")
                .city("Pune")
                .active(true)
                .build();
    }

    @Test
    void register_createsProfile() {
        when(profileRepository.existsByUserId(userId)).thenReturn(false);
        when(profileRepository.save(any(DistributorProfile.class))).thenAnswer(i -> i.getArgument(0));

        DistributorResponse response = distributorService.register(userId,
                new DistributorRegisterRequest("Medi Wholesale", "DL-12345", "12 Market Road", "Pune"));

        assertThat(response.shopName()).isEqualTo("Medi Wholesale");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.active()).isTrue();
        verify(profileRepository).save(any(DistributorProfile.class));
    }

    @Test
    void register_throwsWhenProfileExists() {
        when(profileRepository.existsByUserId(userId)).thenReturn(true);

        assertThatThrownBy(() -> distributorService.register(userId,
                new DistributorRegisterRequest("Medi Wholesale", "DL-12345", "12 Market Road", "Pune")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getMine_returnsProfile() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        DistributorResponse response = distributorService.getMine(userId);

        assertThat(response.id()).isEqualTo(distributorId);
        assertThat(response.city()).isEqualTo("Pune");
    }

    @Test
    void getMine_throwsWhenMissing() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> distributorService.getMine(userId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateProfile_updatesFields() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(DistributorProfile.class))).thenAnswer(i -> i.getArgument(0));

        DistributorResponse response = distributorService.updateProfile(userId,
                new DistributorRegisterRequest("New Shop", "DL-99999", "88 New Street", "Mumbai"));

        assertThat(response.shopName()).isEqualTo("New Shop");
        assertThat(response.city()).isEqualTo("Mumbai");
    }

    @Test
    void listActive_returnsOnlyActive() {
        DistributorProfile other = DistributorProfile.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).shopName("Other")
                .active(false).build();
        when(profileRepository.findByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(profile));

        List<DistributorResponse> result = distributorService.listActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(distributorId);
        assertThat(other.isActive()).isFalse();
    }

    @Test
    void addCatalogItem_createsItem() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(catalogRepository.findByDistributorIdAndProductId(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());
        when(catalogRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));

        CatalogItemResponse response = distributorService.addCatalogItem(userId,
                new CatalogItemRequest(UUID.randomUUID(), "Paracetamol 500mg", new BigDecimal("12.50"), 20, 100));

        assertThat(response.productName()).isEqualTo("Paracetamol 500mg");
        assertThat(response.stockQty()).isEqualTo(100);
        assertThat(response.distributorId()).isEqualTo(distributorId);
    }

    @Test
    void addCatalogItem_throwsOnDuplicateProduct() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(catalogRepository.findByDistributorIdAndProductId(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.of(CatalogItem.builder().build()));

        assertThatThrownBy(() -> distributorService.addCatalogItem(userId,
                new CatalogItemRequest(UUID.randomUUID(), "Paracetamol", new BigDecimal("12.50"), 20, 100)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateCatalogItem_updatesItem() {
        UUID itemId = UUID.randomUUID();
        CatalogItem item = CatalogItem.builder()
                .id(itemId).distributorId(distributorId).productId(UUID.randomUUID())
                .productName("Old").unitPrice(new BigDecimal("10.00")).packSize(10).stockQty(5).build();
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(catalogRepository.findByDistributorIdAndId(distributorId, itemId)).thenReturn(Optional.of(item));
        when(catalogRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));

        CatalogItemResponse response = distributorService.updateCatalogItem(userId, itemId,
                new CatalogItemRequest(item.getProductId(), "New Name", new BigDecimal("15.00"), 30, 50));

        assertThat(response.productName()).isEqualTo("New Name");
        assertThat(response.unitPrice()).isEqualByComparingTo("15.00");
        assertThat(response.stockQty()).isEqualTo(50);
    }

    @Test
    void deleteCatalogItem_deletesItem() {
        UUID itemId = UUID.randomUUID();
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(catalogRepository.findByDistributorIdAndId(distributorId, itemId))
                .thenReturn(Optional.of(CatalogItem.builder().id(itemId).build()));

        distributorService.deleteCatalogItem(userId, itemId);

        verify(catalogRepository).delete(any(CatalogItem.class));
    }

    @Test
    void adjustStock_rejectsNegativeResult() {
        UUID productId = UUID.randomUUID();
        CatalogItem item = CatalogItem.builder()
                .distributorId(distributorId).productId(productId).stockQty(2).build();
        when(catalogRepository.findByDistributorIdAndProductId(distributorId, productId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> distributorService.adjustStock(distributorId, productId, -5))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void adjustStock_updatesQuantity() {
        UUID productId = UUID.randomUUID();
        CatalogItem item = CatalogItem.builder()
                .distributorId(distributorId).productId(productId).stockQty(2).build();
        when(catalogRepository.findByDistributorIdAndProductId(distributorId, productId)).thenReturn(Optional.of(item));
        when(catalogRepository.save(any(CatalogItem.class))).thenAnswer(i -> i.getArgument(0));

        distributorService.adjustStock(distributorId, productId, 3);

        assertThat(item.getStockQty()).isEqualTo(5);
    }
}

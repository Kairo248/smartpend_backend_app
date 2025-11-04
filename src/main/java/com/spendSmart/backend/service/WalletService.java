package com.spendSmart.backend.service;

import com.spendSmart.backend.dto.wallet.*;
import com.spendSmart.backend.entity.User;
import com.spendSmart.backend.entity.Wallet;
import com.spendSmart.backend.repository.UserRepository;
import com.spendSmart.backend.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    public List<WalletResponse> getUserWallets(Long userId) {
        List<Wallet> wallets = walletRepository.findByUserIdAndIsActiveTrue(userId);
        return wallets.stream()
                .map(this::mapToWalletResponse)
                .collect(Collectors.toList());
    }

    public WalletResponse getWalletById(Long walletId, Long userId) {
        Wallet wallet = walletRepository.findByIdAndUserIdAndIsActiveTrue(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        return mapToWalletResponse(wallet);
    }

    @Transactional
    public WalletResponse createWallet(WalletCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if wallet name already exists for this user
        if (walletRepository.existsByUserIdAndNameAndIsActiveTrue(userId, request.getName())) {
            throw new RuntimeException("Wallet with this name already exists");
        }

        // If this is the first wallet for the user, make it default
        List<Wallet> existingWallets = walletRepository.findByUserIdAndIsActiveTrue(userId);
        boolean shouldBeDefault = existingWallets.isEmpty() || request.getIsDefault();

        // If setting as default, unset other default wallets
        if (shouldBeDefault) {
            walletRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)
                    .ifPresent(defaultWallet -> {
                        defaultWallet.setIsDefault(false);
                        walletRepository.save(defaultWallet);
                    });
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .name(request.getName())
                .currency(request.getCurrency())
                .balance(request.getBalance())
                .description(request.getDescription())
                .isDefault(shouldBeDefault)
                .isActive(true)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        return mapToWalletResponse(savedWallet);
    }

    @Transactional
    public WalletResponse updateWallet(Long walletId, WalletUpdateRequest request, Long userId) {
        Wallet wallet = walletRepository.findByIdAndUserIdAndIsActiveTrue(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // Check if name already exists (excluding current wallet)
        if (!wallet.getName().equals(request.getName()) && 
            walletRepository.existsByUserIdAndNameAndIsActiveTrue(userId, request.getName())) {
            throw new RuntimeException("Wallet with this name already exists");
        }

        // If setting as default, unset other default wallets
        if (Boolean.TRUE.equals(request.getIsDefault()) && !wallet.getIsDefault()) {
            walletRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)
                    .ifPresent(defaultWallet -> {
                        defaultWallet.setIsDefault(false);
                        walletRepository.save(defaultWallet);
                    });
        }

        wallet.setName(request.getName());
        wallet.setCurrency(request.getCurrency());
        wallet.setBalance(request.getBalance());
        wallet.setDescription(request.getDescription());
        
        if (request.getIsDefault() != null) {
            wallet.setIsDefault(request.getIsDefault());
        }
        
        if (request.getIsActive() != null) {
            wallet.setIsActive(request.getIsActive());
        }

        Wallet updatedWallet = walletRepository.save(wallet);
        return mapToWalletResponse(updatedWallet);
    }

    @Transactional
    public void deleteWallet(Long walletId, Long userId) {
        Wallet wallet = walletRepository.findByIdAndUserIdAndIsActiveTrue(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // Soft delete
        wallet.setIsActive(false);
        walletRepository.save(wallet);

        // If this was the default wallet, set another wallet as default
        if (wallet.getIsDefault()) {
            List<Wallet> otherWallets = walletRepository.findByUserIdAndIsActiveTrue(userId);
            if (!otherWallets.isEmpty()) {
                Wallet newDefault = otherWallets.get(0);
                newDefault.setIsDefault(true);
                walletRepository.save(newDefault);
            }
        }
    }

    private WalletResponse mapToWalletResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getName(),
                wallet.getCurrency(),
                wallet.getBalance(),
                wallet.getDescription(),
                wallet.getIsDefault(),
                wallet.getIsActive(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}
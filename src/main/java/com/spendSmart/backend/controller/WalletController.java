package com.spendSmart.backend.controller;

import com.spendSmart.backend.dto.wallet.*;
import com.spendSmart.backend.security.UserPrincipal;
import com.spendSmart.backend.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping
    public ResponseEntity<List<WalletResponse>> getUserWallets(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<WalletResponse> wallets = walletService.getUserWallets(userPrincipal.getId());
        return ResponseEntity.ok(wallets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> getWallet(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        WalletResponse wallet = walletService.getWalletById(id, userPrincipal.getId());
        return ResponseEntity.ok(wallet);
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @Valid @RequestBody WalletCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        WalletResponse wallet = walletService.createWallet(request, userPrincipal.getId());
        return ResponseEntity.ok(wallet);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WalletResponse> updateWallet(
            @PathVariable Long id,
            @Valid @RequestBody WalletUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        WalletResponse wallet = walletService.updateWallet(id, request, userPrincipal.getId());
        return ResponseEntity.ok(wallet);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWallet(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        walletService.deleteWallet(id, userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }
}
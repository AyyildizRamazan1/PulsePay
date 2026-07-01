package com.kurumsal.wallet_api.user.service;

import com.kurumsal.wallet_api.infrastructure.exception.DuplicateEmailException;
import com.kurumsal.wallet_api.infrastructure.exception.UserNotFoundException;
import com.kurumsal.wallet_api.user.domain.User;
import com.kurumsal.wallet_api.user.dto.CreateUserRequest;
import com.kurumsal.wallet_api.user.dto.UserResponse;
import com.kurumsal.wallet_api.user.repository.UserRepository;
import com.kurumsal.wallet_api.wallet.domain.Wallet;
import com.kurumsal.wallet_api.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .name(request.name())
                .phone(request.phone())
                .build();
        user = userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .user(user)
                .build();
        walletRepository.save(wallet);

        log.info("Created user id={} with wallet", user.getId());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return UserResponse.from(user);
    }
}

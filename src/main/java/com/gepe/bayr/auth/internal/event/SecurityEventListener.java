    package com.gepe.bayr.auth.internal.event;

    import com.gepe.bayr.auth.api.event.ReplayAttackDetectedEvent;
    import com.gepe.bayr.auth.internal.repo.RefreshSessionRepo;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Component;
    import org.springframework.transaction.annotation.Propagation;
    import org.springframework.transaction.annotation.Transactional;
    import org.springframework.transaction.event.TransactionPhase;
    import org.springframework.transaction.event.TransactionalEventListener;

    @Component
    @RequiredArgsConstructor
    public class SecurityEventListener {
        private final RefreshSessionRepo refreshSessionRepo;

        @TransactionalEventListener(
                phase = TransactionPhase.AFTER_ROLLBACK, // Dieksekusi SAAT transaksi utama ROLLBAC
                fallbackExecution = true // Tetap jalan jika seandainya tidak ada transaksi berjalan
        )
        @Transactional(propagation = Propagation.REQUIRES_NEW) // Wajib buka transaksi baru mandiri
        public void onReplayAttack(ReplayAttackDetectedEvent event) {
            refreshSessionRepo.revokeAllByUserId(event.userId(), "SECURITY:REPLAY_ATTACK_DETECTED");
        }

    }

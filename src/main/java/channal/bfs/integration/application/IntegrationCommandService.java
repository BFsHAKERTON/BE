package channal.bfs.integration.application;

import channal.bfs.auth.infrastructure.AppUserEntity;
import channal.bfs.auth.infrastructure.JpaUserRepository;
import channal.bfs.common.domain.IntegrationType;
import channal.bfs.common.exception.AppException;
import channal.bfs.integration.infrastructure.ChannelTalkApiClient;
import channal.bfs.integration.infrastructure.IntegrationEntity;
import channal.bfs.integration.infrastructure.IntegrationJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 연동(Integration) 등록 및 수정을 담당하는 서비스
 */
@Service
@Transactional
public class IntegrationCommandService {

    private final IntegrationJpaRepository integrationRepository;
    private final JpaUserRepository userRepository;
    private final ChannelTalkApiClient channelTalkApiClient;

    public IntegrationCommandService(
            IntegrationJpaRepository integrationRepository,
            JpaUserRepository userRepository,
            ChannelTalkApiClient channelTalkApiClient) {
        this.integrationRepository = integrationRepository;
        this.userRepository = userRepository;
        this.channelTalkApiClient = channelTalkApiClient;
    }

    /**
     * 채널톡 API Key 등록 또는 업데이트
     * 
     * @param userId 사용자 ID
     * @param apiKey 채널톡 Access Key
     * @param apiSecret 채널톡 Access Secret
     * @throws AppException API Key가 유효하지 않거나 사용자를 찾을 수 없는 경우
     */
    public void registerChannelTalkApiKey(UUID userId, String apiKey, String apiSecret) {
        // 사용자 조회
        AppUserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        // API Key 유효성 검사
        if (!channelTalkApiClient.validateApiKey(apiKey, apiSecret)) {
            throw new AppException("INVALID_CHANNEL_TALK_KEY", "유효하지 않은 채널톡 API Key입니다.");
        }

        // 기존 연동 정보 조회 (있으면 업데이트, 없으면 생성)
        IntegrationEntity integration = integrationRepository
                .findByUserIdAndType(userId, IntegrationType.CHANNEL_TALK)
                .orElse(new IntegrationEntity());

        // 연동 정보 설정
        integration.setUser(user);
        integration.setType(IntegrationType.CHANNEL_TALK);
        integration.setChannelTalkApiKey(apiKey);
        integration.setChannelTalkApiSecret(apiSecret);

        // 저장
        integrationRepository.save(integration);
    }
}


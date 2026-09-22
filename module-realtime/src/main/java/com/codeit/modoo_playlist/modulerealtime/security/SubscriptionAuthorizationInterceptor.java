package com.codeit.modoo_playlist.modulerealtime.security;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.infra.repository.RealtimeConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SubscriptionAuthorizationInterceptor implements ChannelInterceptor {

    private final RealtimeConversationRepository conversationRepository;

    private static final Pattern SUBSCRIPTION = Pattern.compile(
            "^/sub/(contents|conversations)/"
                    + "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{12})/"
                    + "(chat|watch|direct-messages)$"
    );

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel){
        return checkSubscription(message);
    }

    private Message<?> checkSubscription(Message<?> message) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class
                );

        if (accessor == null
                || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        // 인증/역할 검사는 앞선 AuthorizationChannelInterceptor에서 수행한다.
        if ("/user/sub/errors".equals(destination)) {
            return message;
        }
        var matcher = SUBSCRIPTION.matcher(
                destination == null ? "" : destination
        );

        if (!matcher.matches()) {
            throw new BaseException(ErrorCode.ACCESS_DENIED);
        }

        String resource = matcher.group(1);
        String event = matcher.group(3);

        if ("contents".equals(resource)) {
            if (!"chat".equals(event) && !"watch".equals(event)) {
                throw new BaseException(ErrorCode.ACCESS_DENIED);
            }
            return message;
        }

        if (!"direct-messages".equals(event)) {
            throw new BaseException(ErrorCode.ACCESS_DENIED);
        }

        if (!(accessor.getUser() instanceof Authentication authentication)
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof RealtimePrincipal user)) {
            throw new BaseException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        UUID conversationId = UUID.fromString(matcher.group(2));

        if (!conversationRepository.existsByIdAndParticipants_User_Id(
                conversationId, user.userId()
        )) {
            throw new BaseException(ErrorCode.CONVERSATION_ACCESS_DENIED);
        }

        return message;
    }
}

package com.codeit.modoo_playlist.modulerealtime.security;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.core.global.security.AccessTokenClaims;
import com.codeit.modoo_playlist.core.global.security.LoginSessionStore;
import com.codeit.modoo_playlist.infra.repository.RealtimeUserRepository;
import com.codeit.modoo_playlist.infra.security.AccessTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

// jwt 검증, redis 세션 확인, 사용자 조회
// 액세스 토큰 검증 및 인증된 사용자 정보 생성
@Service
@RequiredArgsConstructor
public class RealtimeAuthenticationService {

    private final AccessTokenVerifier accessTokenVerifier;
    private final LoginSessionStore loginSessionStore;
    private final RealtimeUserRepository userRepository;
    private final RoleHierarchy roleHierarchy;

    public Authentication authenticate(String accessToken){

        // JWT 검증(서명, 만료 issuer, accesss 타입 및 필수 클레임)
        AccessTokenClaims claims = accessTokenVerifier.verify(accessToken);

        try{
        // Redis 세션 확인
            loginSessionStore.findActive(claims.userId(), claims.sid())
                    .orElseThrow(()-> new BaseException(ErrorCode.LOGIN_SESSION_INVALIDATED)
                    );

            //사용자 조회
            User user = userRepository.findByEmail(claims.email())
                    .orElseThrow(()->
                            new BaseException(ErrorCode.INVALID_TOKEN)
                    );

            // 토큰의 사용자 정보와 일치 여부 확인
            if(!claims.userId().equals(user.getId())){
                throw new BaseException(ErrorCode.INVALID_TOKEN);
            }

            RealtimePrincipal principal = new RealtimePrincipal(
                    user.getId(),
                    user.getEmail()
            );

            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );

            // 인증 객체 생성
            return new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    roleHierarchy.getReachableGrantedAuthorities(authorities)
            );
        } catch (DataAccessException e){
            throw new BaseException(
                    ErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE,
                    e
            );
        }
    }
}

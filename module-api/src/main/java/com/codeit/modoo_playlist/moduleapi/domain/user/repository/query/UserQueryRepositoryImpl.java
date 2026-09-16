package com.codeit.modoo_playlist.moduleapi.domain.user.repository.query;

import com.codeit.modoo_playlist.core.domain.user.entity.QUser;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserListRequest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

  private static final QUser qUser = QUser.user;

  private final JPAQueryFactory jpaQueryFactory;

  @Override
  public UserQueryPage findAllUsers(
      UserListRequest request
  ) {
    BooleanBuilder filter = createFilter(request);

    List<User> results = jpaQueryFactory
        .selectFrom(qUser)
        .where(filter, applyCursor(request))
        .orderBy(orderSpecifiers(request))
        .limit(request.limit() + 1L)
        .fetch();

    boolean hasNext =
        results.size() > request.limit();

    List<User> users = hasNext
        ? results.subList(0, request.limit())
        : results;

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext && !users.isEmpty()) {
      User last = users.get(users.size() - 1);

      nextCursor = cursorValue(last, request.sortBy());

      nextIdAfter = last.getId();
    }

    Long count = jpaQueryFactory
        .select(qUser.count())
        .from(qUser)
        .where(filter)
        .fetchOne();

    return new UserQueryPage(
        users,
        nextCursor,
        nextIdAfter,
        hasNext,
        count == null ? 0 : count
    );
  }

  private BooleanBuilder createFilter(
      UserListRequest request
  ) {
    BooleanBuilder builder = new BooleanBuilder();

//    Bot 제외
    builder.and(qUser.role.in(UserRole.ADMIN, UserRole.USER));

//    emailLike
    if (StringUtils.hasText(request.emailLike())) {
      builder.and(qUser.email.containsIgnoreCase(request.emailLike().trim()));
    }

//    role 필터링
    if (StringUtils.hasText(request.roleEqual())) {
      builder.and(qUser.role.eq(UserRole.valueOf(request.roleEqual())));
    }

//    lock 필터링
    if (request.isLocked() != null) {
      builder.and(qUser.locked.eq(request.isLocked()));
    }

    return builder;
  }

  private BooleanBuilder applyCursor(
      UserListRequest request
  ) {
    BooleanBuilder builder = new BooleanBuilder();

    boolean hasCursor = StringUtils.hasText(request.cursor());

    // 첫 페이지
    if (!hasCursor && request.idAfter() == null) {
      return builder;
    }

    // 둘 중 하나만 전달된 잘못된 요청
    if (!hasCursor || request.idAfter() == null) {
      throw new BaseException(ErrorCode.INVALID_REQUEST);
    }

    String cursor = request.cursor();
    UUID idAfter = request.idAfter();

    boolean descending = "DESCENDING".equals(request.sortDirection());

    BooleanExpression idCondition = descending
        ? qUser.id.lt(idAfter)
        : qUser.id.gt(idAfter);

    switch (request.sortBy()) {
      case "name" -> {
        BooleanExpression primary = descending
            ? qUser.username.lt(cursor)
            : qUser.username.gt(cursor);

        builder.and(
            primary.or(qUser.username.eq(cursor).and(idCondition))
        );
      }

      case "email" -> {
        BooleanExpression primary = descending
            ? qUser.email.lt(cursor)
            : qUser.email.gt(cursor);

        builder.and(
            primary.or(qUser.email.eq(cursor).and(idCondition))
        );
      }

      case "createdAt" -> {
        Instant cursorCreatedAt;

        try {
          cursorCreatedAt = Instant.parse(cursor);
        } catch (DateTimeParseException e) {
          throw new BaseException(ErrorCode.INVALID_REQUEST, e);
        }

        BooleanExpression primary = descending
            ? qUser.createdAt.lt(cursorCreatedAt)
            : qUser.createdAt.gt(cursorCreatedAt);

        builder.and(
            primary.or(qUser.createdAt.eq(cursorCreatedAt).and(idCondition))
        );
      }

      case "isLocked" -> {
        if (!cursor.equals("true") && !cursor.equals("false")) {
          throw new BaseException(ErrorCode.INVALID_REQUEST);
        }

        boolean cursorLocked = Boolean.parseBoolean(cursor);

        BooleanExpression sameLocked = qUser.locked.eq(cursorLocked).and(idCondition);

        /*
         * ASCENDING: false → true
         * DESCENDING: true → false
         */
        if (descending && cursorLocked) {
          builder.and(qUser.locked.isFalse().or(sameLocked));
        } else if (!descending && !cursorLocked) {
          builder.and(qUser.locked.isTrue().or(sameLocked));
        } else {
          builder.and(sameLocked);
        }
      }

      case "role" -> {
        UserRole cursorRole;

        try {
          cursorRole = UserRole.valueOf(cursor);
        } catch (IllegalArgumentException e) {
          throw new BaseException(ErrorCode.INVALID_REQUEST, e);
        }

        if (cursorRole != UserRole.ADMIN
            && cursorRole != UserRole.USER) {
          throw new BaseException(ErrorCode.INVALID_REQUEST);
        }

        BooleanExpression sameRole = qUser.role.eq(cursorRole).and(idCondition);

        /*
         * 문자열 DB 정렬 기준:
         * ASCENDING: ADMIN → USER
         * DESCENDING: USER → ADMIN
         */
        if (descending && cursorRole == UserRole.USER) {
          builder.and(qUser.role.eq(UserRole.ADMIN).or(sameRole));
        } else if (!descending && cursorRole == UserRole.ADMIN) {
          builder.and(qUser.role.eq(UserRole.USER).or(sameRole)
          );
        } else {
          builder.and(sameRole);
        }
      }

      default -> throw new BaseException(ErrorCode.INVALID_REQUEST);
    }

    return builder;
  }

  //  Order를 쓰면 같은 동작이지만 15줄 정도를 줄이고 깔끔하게 볼 수 있음.
  private OrderSpecifier<?>[] orderSpecifiers(UserListRequest request) {
    Order order = "DESCENDING".equals(request.sortDirection())
        ? Order.DESC
        : Order.ASC;

    OrderSpecifier<?> primary = switch (request.sortBy()) {
      case "name" -> new OrderSpecifier<>(order, qUser.username);
      case "email" -> new OrderSpecifier<>(order, qUser.email);
      case "createdAt" -> new OrderSpecifier<>(order, qUser.createdAt);
      case "isLocked" -> new OrderSpecifier<>(order, qUser.locked);
      case "role" -> new OrderSpecifier<>(order, qUser.role);
      default -> throw new BaseException(ErrorCode.INVALID_REQUEST);
    };

    return new OrderSpecifier<?>[]{
        primary,
        new OrderSpecifier<>(order, qUser.id)
    };
  }

  private String cursorValue(
      User user,
      String sortBy
  ) {
    return switch (sortBy) {
      case "name" -> user.getUsername();

      case "email" -> user.getEmail();

      case "createdAt" -> user.getCreatedAt().toString();

      case "isLocked" -> Boolean.toString(user.isLocked());

      case "role" -> user.getRole().name();

      default -> throw new BaseException(
          ErrorCode.INVALID_REQUEST
      );
    };
  }
}

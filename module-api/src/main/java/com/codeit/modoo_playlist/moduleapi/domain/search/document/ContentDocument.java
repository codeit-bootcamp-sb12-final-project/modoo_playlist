package com.codeit.modoo_playlist.moduleapi.domain.search.document;

import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document(indexName = "contents")
@Setting(settingPath = "elasticsearch/content-settings.json")
public class ContentDocument {

  // 원본 콘텐츠 ID를 문서 ID로 사용하고, id.keyword를 보조 정렬에 사용
  @Id
  @MultiField(
      mainField = @Field(type = FieldType.Text),
      otherFields = {
          @InnerField(
              suffix = "keyword",
              type = FieldType.Keyword,
              ignoreAbove = 256
          )
      }
  )
  private String id;

  @Field(type = FieldType.Keyword)
  private ContentType type;

  @Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_analyzer")
  private String title;

  @Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_analyzer")
  private String description;

  @Field(type = FieldType.Keyword)
  private String thumbnailUrl;

  @Field(type = FieldType.Keyword)
  private List<String> tags;

  @Field(type = FieldType.Double)
  private double averageRating;

  @Field(type = FieldType.Integer)
  private int reviewCount;

  @Field(
      type = FieldType.Date_Nanos,
      format = DateFormat.strict_date_optional_time_nanos
  )
  private Instant createdAt;
}

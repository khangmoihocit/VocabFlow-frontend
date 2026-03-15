package com.khangmoihocit.VocabFlow.core.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Builder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse <T>{
    int page;
    int size;
    long totalElements;
    int totalPages;
    List<T> data;
}

package com.khangmoihocit.VocabFlow.core.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class GenericSpecificationBuilder<T> {

    private final List<SearchCriteria> params;

    public GenericSpecificationBuilder() {
        this.params = new ArrayList<>();
    }

    // Hàm nhận điều kiện từ bên ngoài truyền vào
    public GenericSpecificationBuilder<T> with(String key, String operation, Object value) {
        params.add(new SearchCriteria(key, operation, value));
        return this;
    }

    // Hàm lắp ráp tất cả điều kiện bằng chữ AND
    public Specification<T> build() {
        if (params.isEmpty()) {
            //conjunction (Luôn đúng / WHERE 1=1)
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        }

        // Khởi tạo điều kiện đầu tiên
        Specification<T> result = new GenericSpecification<>(params.get(0));

        // Nối tất cả các điều kiện còn lại bằng AND
        for (int i = 1; i < params.size(); i++) {
            result = Specification.where(result).and(new GenericSpecification<>(params.get(i)));
        }

        return result;
    }
}
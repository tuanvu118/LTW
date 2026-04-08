package com.BTL_JAVA.BTL.Repository;

import com.BTL_JAVA.BTL.Search.ProductSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, Integer> {
}

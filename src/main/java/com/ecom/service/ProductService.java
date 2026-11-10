package com.ecom.service;

import com.ecom.dto.Product;
import com.ecom.entity.ProductEntity;
import com.ecom.mapper.ProductMapper;
import com.ecom.repository.ProductRepository;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    public ProductService(ProductRepository productRepository, ProductMapper mapper) {
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    public List<Product> getAllProducts(){
        return productRepository.findAll().stream().map(mapper::toProductDto).toList();
    }

    public Product createProduct(Product product){
        ProductEntity save = productRepository.save(mapper.toProductEntity(product));
        return mapper.toProductDto(save);
    }

    public Boolean updateProduct(Long id, Product product) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    mapper.updateProductEntityFromDto(product, existingProduct);
                    productRepository.save(existingProduct);
                    return true;
                }).orElse(false);
    }

    public List<Product> findActiveProducts(){
        return productRepository.findByActiveTrue().stream().map(mapper::toProductDto).toList();
    }

    public boolean deleteProduct(Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setActive(false);
                    productRepository.save(product);
                    return true;
                }).orElse(false);
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository.searchProduct(keyword).stream()
                .map(mapper::toProductDto)
                .toList();
    }
}

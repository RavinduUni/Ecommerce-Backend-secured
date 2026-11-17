package com.ecom.service;

import com.ecom.config.CacheConfig;
import com.ecom.dto.CatalogQuery;
import com.ecom.dto.CategoryCount;
import com.ecom.dto.PageResponse;
import com.ecom.dto.Product;
import com.ecom.dto.UploadedImage;
import com.ecom.entity.ProductEntity;
import com.ecom.exception.ApiException;
import com.ecom.mapper.ProductMapper;
import com.ecom.repository.ProductCatalogRepository;
import com.ecom.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

/**
 * Reads are cached (see CacheConfig / application.yml). Every method that changes a
 * product clears all product caches, so admins see their edits immediately.
 * Stock changes from orders are not evicted: the shop may show a stock count up to
 * 30 s old, but the cart and checkout always check live stock.
 */
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductCatalogRepository catalogRepository;
    private final ProductMapper mapper;
    private final ImageUploadService imageUploadService;

    public ProductService(ProductRepository productRepository, ProductCatalogRepository catalogRepository,
                          ProductMapper mapper, ImageUploadService imageUploadService) {
        this.productRepository = productRepository;
        this.catalogRepository = catalogRepository;
        this.mapper = mapper;
        this.imageUploadService = imageUploadService;
    }

    // ------------------------------------------------------------------ reads

    /** Paged shop listing. The cleaned-up query is the cache key. */
    @Cacheable(CacheConfig.CATALOG)
    @Transactional(readOnly = true)
    public PageResponse<Product> getCatalog(CatalogQuery query) {
        ProductCatalogRepository.Result result = catalogRepository.findActive(
                query.category(), query.keyword(), query.sort(), query.page(), query.size());
        List<Product> content = result.items().stream().map(mapper::toProductDto).toList();
        return PageResponse.of(content, query.page(), query.size(), result.total());
    }

    @Cacheable(CacheConfig.CATEGORIES)
    @Transactional(readOnly = true)
    public List<CategoryCount> getCategories() {
        return productRepository.countActiveByCategory().stream()
                .map(row -> new CategoryCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    /** One product as customers see it: active products only. */
    @Cacheable(CacheConfig.PRODUCT)
    @Transactional(readOnly = true)
    public Product getActiveProduct(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .map(mapper::toProductDto)
                .orElseThrow(() -> ApiException.notFound("Product not found."));
    }

    /** One product as admins see it, hidden ones included. Not cached. */
    @Transactional(readOnly = true)
    public Product getAnyProduct(Long id) {
        return productRepository.findById(id)
                .map(mapper::toProductDto)
                .orElseThrow(() -> ApiException.notFound("Product not found."));
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts(){
        return productRepository.findAll().stream().map(mapper::toProductDto).toList();
    }

    @Cacheable(CacheConfig.ACTIVE_PRODUCTS)
    @Transactional(readOnly = true)
    public List<Product> findActiveProducts(){
        return productRepository.findByActiveTrue().stream().map(mapper::toProductDto).toList();
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        return productRepository.searchProduct(keyword).stream()
                .map(mapper::toProductDto)
                .toList();
    }

    // ----------------------------------------------------------------- writes

    @CacheEvict(cacheNames = {CacheConfig.CATALOG, CacheConfig.CATEGORIES, CacheConfig.PRODUCT, CacheConfig.ACTIVE_PRODUCTS}, allEntries = true)
    public Product createProduct(Product product){
        ProductEntity save = productRepository.save(mapper.toProductEntity(product));
        return mapper.toProductDto(save);
    }

    @CacheEvict(cacheNames = {CacheConfig.CATALOG, CacheConfig.CATEGORIES, CacheConfig.PRODUCT, CacheConfig.ACTIVE_PRODUCTS}, allEntries = true)
    public Boolean updateProduct(Long id, Product product) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    // If the image link is replaced by hand, the old ImageKit file is no longer used.
                    String oldFileId = null;
                    if (product.getImageUrl() != null && !Objects.equals(product.getImageUrl(), existingProduct.getImageUrl())) {
                        oldFileId = existingProduct.getImageFileId();
                        existingProduct.setImageFileId(null);
                    }
                    mapper.updateProductEntityFromDto(product, existingProduct);
                    productRepository.save(existingProduct);
                    imageUploadService.deleteQuietly(oldFileId);
                    return true;
                }).orElse(false);
    }

    /**
     * Uploads a new image to ImageKit, saves its URL on the product, then deletes
     * the previous ImageKit file. Not @Transactional on purpose: the upload is a slow
     * network call and shouldn't hold a database connection open.
     */
    @CacheEvict(cacheNames = {CacheConfig.CATALOG, CacheConfig.CATEGORIES, CacheConfig.PRODUCT, CacheConfig.ACTIVE_PRODUCTS}, allEntries = true)
    public Product updateImage(Long id, MultipartFile file) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found."));

        String oldFileId = product.getImageFileId();
        UploadedImage image = imageUploadService.upload(file, product.getName());

        product.setImageUrl(image.url());
        product.setImageFileId(image.fileId());

        ProductEntity saved;
        try {
            saved = productRepository.save(product);
        } catch (RuntimeException e) {
            // The database save failed, so remove the file we just uploaded instead of leaving an orphan.
            imageUploadService.deleteQuietly(image.fileId());
            throw e;
        }

        imageUploadService.deleteQuietly(oldFileId);
        return mapper.toProductDto(saved);
    }

    @CacheEvict(cacheNames = {CacheConfig.CATALOG, CacheConfig.CATEGORIES, CacheConfig.PRODUCT, CacheConfig.ACTIVE_PRODUCTS}, allEntries = true)
    public boolean deleteProduct(Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setActive(false);
                    productRepository.save(product);
                    return true;
                }).orElse(false);
    }
}
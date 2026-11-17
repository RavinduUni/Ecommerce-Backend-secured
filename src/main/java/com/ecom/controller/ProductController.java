package com.ecom.controller;

import com.ecom.dto.*;
import com.ecom.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Product>> getAllProducts(){
        return new ResponseEntity<>(productService.getAllProducts(), HttpStatus.OK);
    }

    @PostMapping("/")
    public ResponseEntity<Product> createProduct(@RequestBody Product product){
        return new ResponseEntity<>(productService.createProduct(product), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateProduct(@PathVariable Long id,@RequestBody Product product){
        boolean updated = productService.updateProduct(id, product);

        if (updated){
            return new ResponseEntity<>("Product updated successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
    }

    /**
     * Paged shop listing for customers.
     * GET /api/products/catalog?page=0&size=24&category=Audio&q=sony&sort=price-asc
     * sort: featured (default), price-asc, price-desc, name, newest. size is capped at 60.
     */
    @GetMapping("/catalog")
    public ResponseEntity<PageResponse<Product>> getCatalog(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(productService.getCatalog(CatalogQuery.of(category, q, sort, page, size)));
    }

    /** Categories of active products with counts: [{ "name": "Audio", "count": 4 }, ...] */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryCount>> getCategories() {
        return ResponseEntity.ok(productService.getCategories());
    }

    /** One product. Customers get active products only; admins can also open hidden ones. */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        Product product = principal != null && principal.isAdmin()
                ? productService.getAnyProduct(id)
                : productService.getActiveProduct(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Product>> findActiveProducts(){
        return new ResponseEntity<>(productService.findActiveProducts(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id){
        boolean updated = productService.deleteProduct(id);

        if (updated){
            return new ResponseEntity<>("Product deleted successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam("keyword") String keyword){
        return new ResponseEntity<>(productService.searchProducts(keyword), HttpStatus.OK);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file){
        return ResponseEntity.ok(productService.updateImage(id, file));
    }
}

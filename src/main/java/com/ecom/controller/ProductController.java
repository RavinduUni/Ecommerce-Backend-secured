package com.ecom.controller;

import com.ecom.dto.Product;
import com.ecom.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}

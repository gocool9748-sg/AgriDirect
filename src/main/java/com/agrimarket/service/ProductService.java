package com.agrimarket.service;

import com.agrimarket.dao.CategoryDAO;
import com.agrimarket.dao.ProductDAO;
import com.agrimarket.model.Category;
import com.agrimarket.model.Product;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** Listing management for farmers, product search for buyers, categories for the admin. */
public class ProductService {

    private final ProductDAO products = new ProductDAO();
    private final CategoryDAO categories = new CategoryDAO();

    public int addProduct(int farmerId, Category category, String name, String description,
                          String price, String quantity) throws SQLException {
        if (category == null) {
            throw new ValidationException("Choose a category.");
        }
        return products.insert(farmerId, category.id(),
                Validate.required(name, "Product name", 80),
                Validate.optional(description, 255, "Description"),
                Validate.positiveAmount(price, "Price per kg"),
                Validate.nonNegativeAmount(quantity, "Quantity"));
    }

    public void updateProduct(int productId, int farmerId, Category category, String name, String description,
                              String price, String quantity) throws SQLException {
        if (category == null) {
            throw new ValidationException("Choose a category.");
        }
        boolean ok = products.update(productId, farmerId, category.id(),
                Validate.required(name, "Product name", 80),
                Validate.optional(description, 255, "Description"),
                Validate.positiveAmount(price, "Price per kg"),
                Validate.nonNegativeAmount(quantity, "Quantity"));
        if (!ok) {
            throw new ValidationException("That product was not found in your listings.");
        }
    }

    public void updateStock(int productId, int farmerId, String quantity) throws SQLException {
        if (!products.updateStock(productId, farmerId, Validate.nonNegativeAmount(quantity, "Quantity"))) {
            throw new ValidationException("That product was not found in your listings.");
        }
    }

    public void setListed(int productId, int farmerId, boolean listed) throws SQLException {
        if (!products.setActive(productId, farmerId, listed)) {
            throw new ValidationException("That product was not found in your listings.");
        }
    }

    public List<Product> farmerProducts(int farmerId) throws SQLException {
        return products.findByFarmer(farmerId);
    }

    public List<Product> search(String text, Category category, String location, String maxPrice)
            throws SQLException {
        BigDecimal max = (maxPrice == null || maxPrice.isBlank())
                ? null : Validate.positiveAmount(maxPrice, "Max price");
        return products.search(text, category == null ? null : category.id(), location, max);
    }

    public List<Category> categories() throws SQLException {
        return categories.findAll();
    }

    public void addCategory(String name, String description) throws SQLException {
        String n = Validate.required(name, "Category name", 50);
        if (categories.nameExists(n)) {
            throw new ValidationException("Category \"" + n + "\" already exists.");
        }
        categories.insert(n, Validate.optional(description, 255, "Description"));
    }
}

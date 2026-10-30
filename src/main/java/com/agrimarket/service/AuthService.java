package com.agrimarket.service;

import com.agrimarket.dao.AdminDAO;
import com.agrimarket.dao.BuyerDAO;
import com.agrimarket.dao.FarmerDAO;
import com.agrimarket.model.Admin;
import com.agrimarket.model.Buyer;
import com.agrimarket.model.Farmer;
import com.agrimarket.util.PasswordUtil;
import java.sql.SQLException;
import java.util.Optional;

/** Registration and login for farmers, buyers and admins. */
public class AuthService {

    private final FarmerDAO farmers = new FarmerDAO();
    private final BuyerDAO buyers = new BuyerDAO();
    private final AdminDAO admins = new AdminDAO();

    public Farmer registerFarmer(String name, String phone, String village, String district,
                                 String password, String confirm) throws SQLException {
        String n = Validate.required(name, "Name", 100);
        String p = Validate.phone(phone);
        String v = Validate.required(village, "Village", 100);
        String d = Validate.required(district, "District", 100);
        Validate.password(password, confirm);
        if (farmers.phoneExists(p)) {
            throw new ValidationException("A farmer account with this phone number already exists.");
        }
        int id = farmers.insert(n, p, v, d, PasswordUtil.hash(password));
        return new Farmer(id, n, p, v, d);
    }

    public Buyer registerBuyer(String name, String phone, String city, String password, String confirm)
            throws SQLException {
        String n = Validate.required(name, "Name", 100);
        String p = Validate.phone(phone);
        String c = Validate.required(city, "City", 100);
        Validate.password(password, confirm);
        if (buyers.phoneExists(p)) {
            throw new ValidationException("A buyer account with this phone number already exists.");
        }
        int id = buyers.insert(n, p, c, PasswordUtil.hash(password));
        return new Buyer(id, n, p, c);
    }

    public Optional<Farmer> loginFarmer(String phone, String password) throws SQLException {
        String p = Validate.phone(phone);
        return farmers.findByPhone(p)
                .filter(a -> PasswordUtil.verify(password, a.passwordHash()))
                .map(a -> a.user());
    }

    public Optional<Buyer> loginBuyer(String phone, String password) throws SQLException {
        String p = Validate.phone(phone);
        return buyers.findByPhone(p)
                .filter(a -> PasswordUtil.verify(password, a.passwordHash()))
                .map(a -> a.user());
    }

    public Optional<Admin> loginAdmin(String username, String password) throws SQLException {
        String u = Validate.required(username, "Username", 50);
        return admins.findByUsername(u)
                .filter(a -> PasswordUtil.verify(password, a.passwordHash()))
                .map(a -> a.user());
    }
}

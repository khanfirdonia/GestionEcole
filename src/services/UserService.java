package services;

import dao.UserDAO;
import models.User;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour la gestion des utilisateurs
 * Gère la logique métier liée aux utilisateurs
 */
public class UserService {
    private static final Logger logger = Logger.getLogger(UserService.class.getName());
    private UserDAO userDAO = new UserDAO();

    /**
     * Authentifier un utilisateur
     */
    public User authenticate(String username, String password) {
        try {
            User user = userDAO.authenticate(username, password);
            if (user != null) {
                logger.info("Utilisateur authentifié: " + username);
                // Enregistrer la connexion dans l'audit
                AuditService.logLogin(user.getId(), user.getUsername());
            }
            return user;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de l'authentification", e);
        }
        return null;
    }

    /**
     * Récupérer tous les utilisateurs
     */
    public List<User> getAllUsers() {
        try {
            return userDAO.getAllUsers();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des utilisateurs", e);
            return List.of();
        }
    }

    /**
     * Récupérer un utilisateur par ID
     */
    public User getUserById(int id) {
        try {
            return userDAO.getUserById(id);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de l'utilisateur", e);
        }
        return null;
    }

    /**
     * Récupérer un utilisateur par son nom d'utilisateur
     */
    public User getUserByUsername(String username) {
        try {
            return userDAO.getUserByUsername(username);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de l'utilisateur", e);
        }
        return null;
    }

    /**
     * Récupérer tous les utilisateurs d'un rôle spécifique
     */
    public List<User> getUsersByRole(String role) {
        try {
            return userDAO.getUsersByRole(role);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des utilisateurs par rôle", e);
            return List.of();
        }
    }

    /**
     * Créer un nouvel utilisateur
     */
    public int createUser(User user) {
        try {
            if (userDAO.usernameExists(user.getUsername())) {
                logger.warning("Nom d'utilisateur déjà existant: " + user.getUsername());
                return -1;
            }
            return userDAO.saveUser(user);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la création de l'utilisateur", e);
        }
        return -1;
    }

    /**
     * Mettre à jour un utilisateur
     */
    public boolean updateUser(User user) {
        try {
            return userDAO.updateUser(user);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise à jour de l'utilisateur", e);
        }
        return false;
    }

    /**
     * Supprimer un utilisateur
     */
    public boolean deleteUser(int userId) {
        try {
            return userDAO.deleteUser(userId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la suppression de l'utilisateur", e);
        }
        return false;
    }

    /**
     * Compter les utilisateurs par rôle
     */
    public int countUsersByRole(String role) {
        try {
            return userDAO.countUsersByRole(role);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors du comptage des utilisateurs", e);
        }
        return 0;
    }
}

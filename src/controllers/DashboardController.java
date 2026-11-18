package controllers;

import models.User;

/**
 * Interface pour les contrôleurs de dashboard
 * Tous les contrôleurs de dashboard doivent implémenter cette interface
 */
public interface DashboardController {
    
    /**
     * Définir l'utilisateur connecté
     */
    void setUser(User user);
    
    /**
     * Charger les données du dashboard
     */
    void loadDashboardData();
    
    /**
     * Gérer la déconnexion
     */
    void handleLogout();
}

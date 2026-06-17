@PostConstruct
public void initCategories() {
    try {
        if (categoryRepository.count() == 0) {
            log.info("Initializing categories...");
            
            // Create parent categories
            Category noise = new Category("NOISE", "#dc3545");
            Category maintenance = new Category("MAINTENANCE", "#ffc107");
            Category security = new Category("SECURITY", "#dc3545");
            Category cleanliness = new Category("CLEANLINESS", "#28a745");
            Category parking = new Category("PARKING", "#17a2b8");
            Category utilities = new Category("UTILITIES", "#6c757d");
            Category other = new Category("OTHER", "#6c757d");
            
            noise = categoryRepository.save(noise);
            maintenance = categoryRepository.save(maintenance);
            security = categoryRepository.save(security);
            cleanliness = categoryRepository.save(cleanliness);
            parking = categoryRepository.save(parking);
            utilities = categoryRepository.save(utilities);
            other = categoryRepository.save(other);
            
            // Sub-categories for NOISE
            categoryRepository.save(new Category("Loud Music", "#dc3545"));
            // Need to set parent properly
            String[] noiseSubs = {"Loud Music", "Construction", "Party", "Animals", "Vehicle"};
            for (String name : noiseSubs) {
                Category sub = new Category(name, "#dc3545");
                sub.setParent(noise);
                categoryRepository.save(sub);
            }
            
            String[] maintenanceSubs = {"Plumbing", "Electrical", "HVAC", "Structural", "Appliance"};
            for (String name : maintenanceSubs) {
                Category sub = new Category(name, "#ffc107");
                sub.setParent(maintenance);
                categoryRepository.save(sub);
            }
            
            String[] securitySubs = {"Break-in", "Suspicious Activity", "CCTV", "Vandalism", "Theft"};
            for (String name : securitySubs) {
                Category sub = new Category(name, "#dc3545");
                sub.setParent(security);
                categoryRepository.save(sub);
            }
            
            log.info("✅ Categories initialized successfully!");
        }
    } catch (Exception e) {
        log.warning("Failed to initialize categories: " + e.getMessage());
    }
}